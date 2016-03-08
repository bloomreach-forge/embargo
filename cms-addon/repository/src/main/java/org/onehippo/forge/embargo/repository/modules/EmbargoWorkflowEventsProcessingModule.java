package org.onehippo.forge.embargo.repository.modules;

import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import com.google.common.base.Strings;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;

import org.hippoecm.repository.decorating.SessionDecorator;
import org.hippoecm.repository.jackrabbit.RepositoryImpl;
import org.hippoecm.repository.security.HippoSecurityManager;
import org.hippoecm.repository.security.service.SecurityServiceImpl;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.onehippo.cms7.services.eventbus.Subscribe;
import org.onehippo.forge.embargo.repository.workflow.EmbargoWorkflow;
import org.onehippo.repository.events.HippoWorkflowEvent;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;
import org.onehippo.repository.modules.DaemonModule;
import org.onehippo.repository.security.Group;
import org.onehippo.repository.security.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmbargoWorkflowEventsProcessingModule extends AbstractReconfigurableDaemonModule {

    private static final Logger log = LoggerFactory.getLogger(EmbargoWorkflowEventsProcessingModule.class);
    public static final String EMBARGO_GROUPS = "embargoGroups";

    private Session session;

    private Set<String> embargoGroups;

    @Override
    protected void doConfigure(final Node moduleConfig) throws RepositoryException {
        embargoGroups = new HashSet<>();
        if (moduleConfig == null) {
            return;
        }
        if (moduleConfig.hasProperty(EMBARGO_GROUPS)) {
            final Property property = moduleConfig.getProperty(EMBARGO_GROUPS);
            final Value[] values = property.getValues();
            for (Value value : values) {
                embargoGroups.add(value.getString());
            }
        }
    }

    @Override
    protected void doInitialize(final Session session) throws RepositoryException {
        this.session = session;
        HippoServiceRegistry.registerService(this, HippoEventBus.class);
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void handleEvent(final HippoWorkflowEvent event) {
        if (embargoGroups == null || embargoGroups.size() == 0) {
            log.warn("No embargo groups configured for Embargo workflow events processing module");
            return;
        }
        if (event.success() && "workflow".equals(event.category())) {


            try {
                final String u = event.user();
                if (Strings.isNullOrEmpty(u)) {
                    return;
                }
                // check if can set embargo


                final Node handle = getHandleFromEvent(event);
                //NOTE:  folders have no handle so those should be filtered out:
                if (handle == null) {
                    return;
                }
                if ("add".equals(event.action()) || "createGalleryItem".equals(event.action()) || "copyTo".equals(event.action())) {
                    final User user = getUser(u);
                    if (user == null) {
                        return;
                    }
                    final Iterable<Group> memberships = user.getMemberships();
                    boolean embargoUser = false;
                    for (Group membership : memberships) {
                        if (embargoGroups.contains(membership.getId())) {
                            embargoUser = true;
                            break;
                        }

                    }
                    if (!embargoUser) {
                        return;
                    }
                    setEmbargoWorkflow(event, handle);
                }
            } catch (Exception e) {
                log.error("Embargo workflow error", e);
            }
        }
    }


    public User getUser(final String user)  {
        try {
            final JackrabbitSession session = (JackrabbitSession)SessionDecorator.unwrap(this.session);
            final RepositoryImpl repository = (RepositoryImpl)session.getRepository();
            final HippoSecurityManager securityManager = (HippoSecurityManager)repository.getSecurityManager();
            final SecurityServiceImpl securityService = new SecurityServiceImpl(securityManager, session);
            return securityService.getUser(user);
        } catch (RepositoryException e) {
            log.error("Error obtaining security manager", e);
        }
        return null;
    }

    public void setEmbargoWorkflow(final HippoWorkflowEvent event, final Node handle) throws WorkflowException, RemoteException {
        try {
            final Node node = handle.getNode(handle.getName());
            final EmbargoWorkflow embargoWorkflow = getWorkflow(node, "embargo");
            if (embargoWorkflow != null) {
                embargoWorkflow.addEmbargo(event.user(), handle.getIdentifier(), null);
            }
        } catch (RepositoryException e) {
            log.error("Unable to get node with id: {}", event.subjectId());
        }
    }

    private String getUuidFromReturnValue(final String returnValue) {
        return StringUtils.substringBetween(returnValue, "uuid=", ",");
    }

    private Node getHandleFromEvent(final HippoWorkflowEvent event) throws RepositoryException {
        if ("document".equals(event.returnType())) {
            log.error("UUID: {}", event.returnValue());
            final String uuid = getUuidFromReturnValue(event.returnValue());
            if (uuid != null) {
                final Node byIdentifier = session.getNodeByIdentifier(event.subjectId());
                return extractHandle(byIdentifier);
            }
        } else if ("java.lang.String".equals(event.returnType())) {
            log.debug("Path: {}", event.returnValue());
            final Node node = session.getNode(event.returnValue());
            if (extractHandle(node) != null) {
                return node;
            } else {
                return node.getParent();
            }
        }
        return null;
    }

    private Node extractHandle(final Node node) throws RepositoryException {
        if (node == null) {
            return null;
        }
        if (node.isNodeType("hippo:handle")) {
            return node;
        }
        return null;

    }

    private EmbargoWorkflow getWorkflow(final Node node, final String category) throws RepositoryException {
        final WorkflowManager workflowManager = ((HippoWorkspace)node.getSession().getWorkspace()).getWorkflowManager();
        final Node canonicalNode = ((HippoNode)node).getCanonicalNode();
        final Workflow workflow = workflowManager.getWorkflow(category, canonicalNode);
        if (workflow instanceof EmbargoWorkflow) {
            return (EmbargoWorkflow)workflow;
        }
        return null;
    }



    @Override
    protected void doShutdown() {
        HippoServiceRegistry.unregisterService(this, HippoEventBus.class);
    }
}
