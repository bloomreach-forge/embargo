/*
 * Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.impl.WorkspaceDecorator;

import org.onehippo.cms7.services.eventbus.HippoEventListenerRegistry;
import org.onehippo.cms7.services.eventbus.Subscribe;
import org.onehippo.forge.embargo.repository.EmbargoConstants;
import org.onehippo.forge.embargo.repository.EmbargoUtils;
import org.onehippo.forge.embargo.repository.workflow.EmbargoWorkflow;
import org.onehippo.repository.events.HippoWorkflowEvent;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;
import org.onehippo.repository.security.Group;
import org.onehippo.repository.security.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unused")
public class EmbargoWorkflowEventsProcessingModule extends AbstractReconfigurableDaemonModule {

    private static final Logger log = LoggerFactory.getLogger(EmbargoWorkflowEventsProcessingModule.class);

    public static final String EMBARGO_GROUPS = "embargoGroups";
    public static final String EMBARGO_WAIT_TIME = "waitTime";
    public static final long DEFAULT_WAIT_TIME = 500L;

    private Session session;
    private long sleepTime;
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
        if (moduleConfig.hasProperty(EMBARGO_WAIT_TIME)) {
            final Property property = moduleConfig.getProperty(EMBARGO_WAIT_TIME);
            sleepTime = property.getLong();
        } else {
            sleepTime = DEFAULT_WAIT_TIME;
        }
    }

    @Override
    protected void doInitialize(final Session session) throws RepositoryException {
        this.session = session;
        HippoEventListenerRegistry.get().register(this);
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
                if (EmbargoUtils.getCurrentUserEmbargoEnabledGroups(session, u).length <= 0) {
                    log.debug("Not setting embargo on subject {} as user {} is not in any embargo enabled groups.", event.subjectPath(), u);
                    return;
                }

                final Node subject = getSubject(event);
                final Node handle = EmbargoUtils.extractHandle(subject);
                //NOTE:  folders have no handle so those should be filtered out:
                final String action = event.action();
                if (handle == null && !"createGalleryItem".equals(action)) {
                    return;
                }
                if ("add".equals(action) || "createGalleryItem".equals(action) || "copyTo".equals(action)) {
                    if (isValidEmbargoUser(u)) {
                        setEmbargoHandle(event, subject);
                    }
                } else if ("commitEditableInstance".equals(action)) {
                    if (handle.isNodeType(EmbargoConstants.EMBARGO_MIXIN_NAME)) {
                        setEmbargoVariants(event, subject);
                    }
                }
            } catch (Exception e) {
                log.error("Embargo workflow error", e);
            }
        }
    }

    private boolean isValidEmbargoUser(final String u) throws RepositoryException {
        final User user = getUser(u);
        if (user == null) {
            return false;
        }
        final Iterable<Group> memberships = user.getMemberships();
        boolean embargoUser = false;
        for (Group membership : memberships) {
            if (embargoGroups.contains(membership.getId())) {
                embargoUser = true;
                break;
            }

        }
        return embargoUser;
    }

    @SuppressWarnings("WeakerAccess")
    public User getUser(final String user) {
        try {
            // TODO in 14.x this is expected be:
            // SecurityService securityService = HippoServiceRegistry.getService(SecurityService.class);
            // return securityService.getUser(user);
            return ((WorkspaceDecorator) session.getWorkspace()).getSecurityService().getUser(user);
        } catch (RepositoryException e) {
            log.error("Error obtaining user", e);
        }

        return null;
    }

    @SuppressWarnings("WeakerAccess")
    public void setEmbargoVariants(final HippoWorkflowEvent event, final Node subject) throws WorkflowException, RemoteException {
        try {
            final EmbargoWorkflow embargoWorkflow = getWorkflow(subject, "embargo");
            if (embargoWorkflow != null) {
                embargoWorkflow.addEmbargoVariants(event.user(), subject.getIdentifier(), null);
            }
        } catch (RepositoryException e) {
            log.error("Unable to get node with id: {}", event.subjectId(), e);
        }
    }

    @SuppressWarnings("WeakerAccess")
    public void setEmbargoHandle(final HippoWorkflowEvent event, final Node subject) throws WorkflowException, RemoteException {
        try {
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException ignore) {

                }
            }
            final EmbargoWorkflow embargoWorkflow = getWorkflow(subject, "embargo");
            if (embargoWorkflow != null) {
                embargoWorkflow.addEmbargoHandle(event.user(), subject.getIdentifier(), null);
            }
        } catch (RepositoryException e) {
            log.error("Unable to get node with id: {}", event.subjectId(), e);
        }
    }

    private String getUuidFromReturnValue(final String returnValue) {
        return StringUtils.substringBetween(returnValue, "uuid=", ",");
    }

    private Node getSubject(final HippoWorkflowEvent event) throws RepositoryException {
        if ("document".equals(event.returnType())) {
            log.debug("UUID: {}", event.returnValue());
            final String uuid = getUuidFromReturnValue(event.returnValue());
            if (uuid != null) {
                return session.getNodeByIdentifier(uuid);
            }
        } else if ("java.lang.String".equals(event.returnType())) {
            log.debug("Path: {}", event.returnValue());
            return session.getNode(event.returnValue());

        }
        return null;
    }


    private Node getHandleFromEvent(final HippoWorkflowEvent event) throws RepositoryException {
        return EmbargoUtils.extractHandle(getSubject(event));
    }

    private EmbargoWorkflow getWorkflow(final Node node, final String category) throws RepositoryException {
        final WorkflowManager workflowManager = ((HippoWorkspace) node.getSession().getWorkspace()).getWorkflowManager();
        final Node canonicalNode = ((HippoNode) node).getCanonicalNode();
        final Workflow workflow = workflowManager.getWorkflow(category, canonicalNode);
        if (workflow instanceof EmbargoWorkflow) {
            return (EmbargoWorkflow) workflow;
        }
        return null;
    }


    @Override
    protected void doShutdown() {
        HippoEventListenerRegistry.get().unregister(this);
    }
}
