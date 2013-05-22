package org.onehippo.forge.embargo.repository.workflow;

import java.rmi.RemoteException;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jdo.annotations.DatastoreIdentity;
import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.onehippo.forge.embargo.repository.EmbargoConstants;
import org.onehippo.forge.embargo.repository.EmbargoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
@PersistenceCapable(identityType = IdentityType.DATASTORE, cacheable = "true", detachable = "false", table = "documents")
@DatastoreIdentity(strategy = IdGeneratorStrategy.NATIVE)
@Discriminator(strategy = DiscriminatorStrategy.CLASS_NAME)
public class EmbargoWorkflowImpl extends WorkflowImpl implements EmbargoWorkflow {

    private static Logger log = LoggerFactory.getLogger(EmbargoWorkflowImpl.class);

    @Persistent(column = "jcr:uuid")
    protected String uuid;

    /**
     * @throws java.rmi.RemoteException
     */
    public EmbargoWorkflowImpl() throws RemoteException {
    }

    @Override
    public void addEmbargo() throws WorkflowException, RepositoryException, MappingException, RemoteException {
        final WorkflowContext workflowContext = getWorkflowContext();
        final Session internalWorkflowSession = workflowContext.getInternalWorkflowSession();
        String invokingUserId = ((UserSession) org.apache.wicket.Session.get()).getJcrSession().getUserID();

        final Node handle = internalWorkflowSession.getNodeByIdentifier(uuid).getParent();
        if (!handle.isCheckedOut()) {
            internalWorkflowSession.getWorkspace().getVersionManager().checkout(handle.getPath());
        }

        String[] userEmbargoEnabledGroups = EmbargoUtils.getCurrentUserEmbargoEnabledGroups(internalWorkflowSession, invokingUserId);
        if (userEmbargoEnabledGroups.length > 0) {
            handle.addMixin(EmbargoConstants.EMBARGO_MIXIN_NAME);
            handle.setProperty(
                    EmbargoConstants.EMBARGO_GROUP_PROPERTY_NAME,
                    userEmbargoEnabledGroups);
            internalWorkflowSession.save();
        }
    }

    @Override
    public void removeEmbargo() throws WorkflowException, RepositoryException, MappingException, RemoteException {
        final WorkflowContext workflowContext = getWorkflowContext();
        final Session internalWorkflowSession = workflowContext.getInternalWorkflowSession();

        final Node handle = internalWorkflowSession.getNodeByIdentifier(uuid).getParent();
        if (!handle.isCheckedOut()) {
            internalWorkflowSession.getWorkspace().getVersionManager().checkout(handle.getPath());
        }
        //remove embargo:groups
        if (handle.hasProperty(EmbargoConstants.EMBARGO_GROUP_PROPERTY_NAME)) {
            handle.getProperty(EmbargoConstants.EMBARGO_GROUP_PROPERTY_NAME).remove();
        }
        //remove any embargo:request
        if (handle.hasNode(EmbargoConstants.EMBARGO_SCHEDULE_REQUEST_NODE_NAME)) {
            handle.getNode(EmbargoConstants.EMBARGO_SCHEDULE_REQUEST_NODE_NAME).remove();
        }
        handle.removeMixin(EmbargoConstants.EMBARGO_MIXIN_NAME);
        internalWorkflowSession.save();
    }

    @Override
    public void removeEmbargo(final Calendar publicationDate) throws WorkflowException, RepositoryException, MappingException, RemoteException {
        cancelSchedule();
        WorkflowContext wfCtx = getWorkflowContext().getWorkflowContext(publicationDate);
        EmbargoWorkflow wf = (EmbargoWorkflow) wfCtx.getWorkflow("embargo");
        wf.removeEmbargo();
    }

    @Override
    public void cancelSchedule() throws WorkflowException, RepositoryException, MappingException, RemoteException {
        final WorkflowContext workflowContext = getWorkflowContext();
        final Session internalWorkflowSession = workflowContext.getInternalWorkflowSession();
        final Node handle = internalWorkflowSession.getNodeByIdentifier(uuid).getParent();
        if (!handle.isCheckedOut()) {
            internalWorkflowSession.getWorkspace().getVersionManager().checkout(handle.getPath());
        }

        if (handle.hasNode(EmbargoConstants.EMBARGO_SCHEDULE_REQUEST_NODE_NAME)) {
            handle.getNode(EmbargoConstants.EMBARGO_SCHEDULE_REQUEST_NODE_NAME).remove();
            internalWorkflowSession.save();
        }

    }
}