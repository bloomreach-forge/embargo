package org.onehippo.forge.embargo.tests;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowManager;
import org.junit.After;
import org.junit.Before;

/**
 * @author Jeroen Reijn
 */
public class BaseRepositoryTest {

    private static final String SYSTEMUSER_ID = "admin";
    private static final char[] SYSTEMUSER_PASSWORD = "admin".toCharArray();

    protected HippoRepository repository;
    protected Session session;
    protected WorkflowManager workflowMgr = null;

    @Before
    public void setUp() throws Exception {
        HippoRepositoryFactory.setDefaultRepository(System.getProperty("repopath"));
        repository = HippoRepositoryFactory.getHippoRepository();
        session = repository.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
    }

    @After
    public void tearDown() throws Exception {
        if(repository!=null) {
            repository.close();
        }
    }

    protected Workflow getWorkflow(Node node, String category) throws RepositoryException {
        if (workflowMgr == null) {
            HippoWorkspace wsp = (HippoWorkspace) node.getSession().getWorkspace();
            workflowMgr = wsp.getWorkflowManager();
        }
        Node canonicalNode = ((HippoNode) node).getCanonicalNode();
        return workflowMgr.getWorkflow(category, canonicalNode);
    }

    protected Workflow getWorkflow(Document document, String category) throws RepositoryException {
        if (workflowMgr == null) {
            HippoWorkspace wsp = (HippoWorkspace) session.getWorkspace();
            workflowMgr = wsp.getWorkflowManager();
        }
        return workflowMgr.getWorkflow(category, document);
    }

    protected Node getNode(String path) throws RepositoryException {
        return ((HippoWorkspace)session.getWorkspace()).getHierarchyResolver().getNode(session.getRootNode(), path);
    }

}
