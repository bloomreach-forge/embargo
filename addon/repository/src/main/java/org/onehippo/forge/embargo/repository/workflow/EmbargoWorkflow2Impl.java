package org.onehippo.forge.embargo.repository.workflow;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jdo.annotations.DatastoreIdentity;
import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
@PersistenceCapable(identityType = IdentityType.DATASTORE, cacheable = "true", detachable = "false", table = "documents")
@DatastoreIdentity(strategy = IdGeneratorStrategy.NATIVE)
@Discriminator(strategy = DiscriminatorStrategy.CLASS_NAME)
public class EmbargoWorkflow2Impl extends WorkflowImpl implements EmbargoWorkflow2 {

    private static Logger log = LoggerFactory.getLogger(EmbargoWorkflow2Impl.class);

    protected static final String EMBARGO_MIXIN_NAME = "embargo:embargo";
    protected static final String EMBARGO_GROUP_PROPERTY_NAME = "embargo:groups";
    protected static final String SELECT_GROUPS_QUERY = "SELECT * FROM hipposys:group WHERE jcr:primaryType='hipposys:group' AND hipposys:members='{}'";
    protected static final String EMBARGO_GROUPS_MAPPING_NODE_PATH = "hippo:configuration/hippo:domains/embargo/hipposys:authrole";


    @Persistent(column = "jcr:uuid")
    protected String uuid;

    /**
     * @throws java.rmi.RemoteException
     */
    public EmbargoWorkflow2Impl() throws RemoteException {
    }

    @Override
    public void addEmbargo() throws WorkflowException, RepositoryException, MappingException, RemoteException {
        final WorkflowContext workflowContext = getWorkflowContext();
        final Session internalWorkflowSession = workflowContext.getInternalWorkflowSession();
        final Node nodeByIdentifier = internalWorkflowSession.getNodeByIdentifier(uuid);
        final Node handle = nodeByIdentifier.getParent();
        if (!handle.isCheckedOut()) {
            internalWorkflowSession.getWorkspace().getVersionManager().checkout(handle.getPath());
        }
        handle.addMixin(EMBARGO_MIXIN_NAME);
        //Issue is it is scheduled the useridentity is system. So I have to check if user is system or else use the embargo identity property
        if (workflowContext.getUserIdentity().equals("system") && handle.isNodeType("embargo:handle")) {
            handle.setProperty(EMBARGO_GROUP_PROPERTY_NAME, getUserGroups(handle.getProperty("embargo:identity").getString()));
        } else {
            handle.setProperty(EMBARGO_GROUP_PROPERTY_NAME, getUserGroups(workflowContext.getUserIdentity()));
        }

        internalWorkflowSession.save();
    }

    protected String[] getUserGroups(String userIdentity) throws RepositoryException {
        Query selectGroupsQuery = getWorkflowContext().getInternalWorkflowSession().getWorkspace().getQueryManager().createQuery(
                SELECT_GROUPS_QUERY.replace("{}", userIdentity),
                Query.SQL);
        NodeIterator groupNodes = selectGroupsQuery.execute().getNodes();
        if (!groupNodes.hasNext()) {
            throw new RepositoryException("User does not have the permissions to set/remove embargo");
        }

        List<String> groupNames = new ArrayList<String>();
        List<String> embargoEnabledGroupNames = getAllEmbargoEnabledGroups();

        while (groupNodes.hasNext()) {
            String groupName = groupNodes.nextNode().getName();
            if(embargoEnabledGroupNames.contains(groupName)){
                groupNames.add(groupName);
            }
        }

        return groupNames.toArray(new String[0]);
    }

    protected List<String> getAllEmbargoEnabledGroups(){
        try {
            Value[] embargoGroups = getWorkflowContext().getInternalWorkflowSession().getRootNode().getNode(EMBARGO_GROUPS_MAPPING_NODE_PATH).getProperty("hipposys:groups").getValues();
            List<String> embargoGroupNames = new ArrayList<String>();
            for(int i=0; i<embargoGroups.length; i++){
                embargoGroupNames.add(embargoGroups[i].getString());
            }
            return embargoGroupNames;

        } catch (RepositoryException e) {
            log.error("Error while reading list of embargo enabled groups", e);
        }

        return Collections.EMPTY_LIST;
    }

/*
    @Override
    public void addEmbargo(final Calendar publicationDate) throws WorkflowException, RepositoryException, MappingException, RemoteException {
        final WorkflowContext workflowContext = getWorkflowContext();
        final Session internalWorkflowSession = workflowContext.getInternalWorkflowSession();
        final Node nodeByIdentifier = internalWorkflowSession.getNodeByIdentifier(uuid);
        final Node handle = nodeByIdentifier.getParent();
        if (!handle.isCheckedOut()) {
            internalWorkflowSession.getWorkspace().getVersionManager().checkout(handle.getPath());
        }
        handle.addMixin("embargo:handle");
        handle.setProperty("embargo:identity", workflowContext.getUserIdentity());
        internalWorkflowSession.save();

        WorkflowContext wfCtx = getWorkflowContext();
        wfCtx = wfCtx.getWorkflowContext(publicationDate);

        EmbargoWorkflow2 wf = (EmbargoWorkflow2) wfCtx.getWorkflow("embargo");
        wf.addEmbargo();
    }
*/

    @Override
    public void removeEmbargo() throws WorkflowException, RepositoryException, MappingException, RemoteException {
        final WorkflowContext workflowContext = getWorkflowContext();
        final Session internalWorkflowSession = workflowContext.getInternalWorkflowSession();
        final Node nodeByIdentifier = internalWorkflowSession.getNodeByIdentifier(uuid);
        final Node handle = nodeByIdentifier.getParent();
        if (!handle.isCheckedOut()) {
            internalWorkflowSession.getWorkspace().getVersionManager().checkout(handle.getPath());
        }
        handle.removeMixin(EMBARGO_MIXIN_NAME);
        if (handle.hasProperty(EMBARGO_GROUP_PROPERTY_NAME)) {
            handle.getProperty(EMBARGO_GROUP_PROPERTY_NAME).remove();
        }
        if(handle.isNodeType("embargo:handle")){
            handle.removeMixin("embargo:handle");
        }
        internalWorkflowSession.save();
    }

    @Override
    public void removeEmbargo(final Calendar publicationDate) throws WorkflowException, RepositoryException, MappingException, RemoteException {
        WorkflowContext wfCtx = getWorkflowContext();
        wfCtx = wfCtx.getWorkflowContext(publicationDate);

        EmbargoWorkflow2 wf = (EmbargoWorkflow2) wfCtx.getWorkflow("embargo");
        wf.removeEmbargo();
    }
}
