/*
package org.onehippo.forge.embargo.repository.workflow2;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;

import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.DefaultWorkflowImpl;

*/
/**
 * @version $Id$
 *//*

public class EmbargoWorkflowImpl extends DefaultWorkflowImpl implements EmbargoWorkflow {

    protected static final String EMBARGO_MIXIN_NAME = "embargo:embargo";
    protected static final String EMBARGO_GROUP_PROPERTY_NAME = "embargo:groups";
    protected static final String SELECT_GROUPS_QUERY = "SELECT * FROM hipposys:group WHERE jcr:primaryType='hipposys:group' AND hipposys:members='{}'";

    protected Session userSession;
    protected Session rootSession;

    public EmbargoWorkflowImpl(WorkflowContext context, Session userSession, Session rootSession, Node subject) throws RepositoryException {
        super(context, userSession, rootSession, subject);
        this.userSession = userSession;
        this.rootSession = rootSession;
    }

    public void setEmbargo(Node documentNode, boolean status) throws WorkflowException, MappingException, RepositoryException {

        if (!documentNode.isCheckedOut()) {
            rootSession.getWorkspace().getVersionManager().checkout(documentNode.getPath());
        }

        if (status) {
            documentNode.addMixin(EMBARGO_MIXIN_NAME);
            documentNode.setProperty(EMBARGO_GROUP_PROPERTY_NAME, getUserGroups());

        } else {
            documentNode.removeMixin(EMBARGO_MIXIN_NAME);
            documentNode.getProperty(EMBARGO_GROUP_PROPERTY_NAME).remove();
        }

        documentNode.getSession().save();
    }

    protected String[] getUserGroups() throws RepositoryException {
        Query selectGroupsQuery = rootSession.getWorkspace().getQueryManager().createQuery(
                SELECT_GROUPS_QUERY.replace("{}", userSession.getUserID()),
                Query.SQL);
        NodeIterator groupNodes = selectGroupsQuery.execute().getNodes();

        if (!groupNodes.hasNext()) {
            throw new RepositoryException("User does not have the permissions to set/remove embargo");
        }

        List<String> groupNames = new ArrayList<String>();
        while (groupNodes.hasNext()) {
            groupNames.add(groupNodes.nextNode().getName());
        }

        return groupNames.toArray(new String[0]);
    }
}
*/
