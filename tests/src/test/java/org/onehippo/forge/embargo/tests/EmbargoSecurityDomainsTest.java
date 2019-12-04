/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.embargo.tests;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.forge.embargo.repository.EmbargoConstants;
import org.onehippo.forge.embargo.repository.workflow.EmbargoWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

/**
 * @version $Id: EmbargoSecurityDomainsTest.java 84 2013-05-27 09:01:12Z mchatzidakis $
 */
public class EmbargoSecurityDomainsTest extends org.onehippo.repository.testutils.RepositoryTestCase {

    private static Logger log = LoggerFactory.getLogger(EmbargoSecurityDomainsTest.class);
    private Session adminSession;
    private Session embargoEditor;
    private Session embargoAuthor;
    private Session editor;

    @Before
    public void setUp() throws Exception {
        //prepareFixture();
        super.setUp(true);
        //load internal workflow before we can use any other one (needed to split up functionality in different methods)
        adminSession = session;
        assumeNotNull(adminSession);
        final Node adminDocumentsFolder = adminSession.getNode(TestConstants.CONTENT_DOCUMENTS_EMBARGODEMO_PATH);
        final FolderWorkflow adminFolderWorkflow = (FolderWorkflow) getWorkflow(adminDocumentsFolder, "internal");
        //(normal) admin creates a news document
        final String adminDocumentLocation = adminFolderWorkflow.add("new-document", "embargotest:document", TestConstants.TEST_DOCUMENT_NAME);
        //quick check if document exists, and now the threepane workflow is enabled for some jdo reason...
        assumeTrue(adminSession.itemExists(adminDocumentLocation));
        //prepare sessions:
        editor = server.login(TestConstants.EDITOR_CREDENTIALS);
        embargoEditor = server.login(TestConstants.EMBARGO_EDITOR_CREDENTIALS);
        embargoAuthor = server.login(TestConstants.EMBARGO_AUTHOR_CREDENTIALS);
    }

    /**
     * Test repository connection with admin credentials .. assuming everything goes well
     */
    @Test
    public void testConnectToRepository() {
        Session localSession = null;
        try {
            localSession = server.login(TestConstants.ADMIN_CREDENTIALS);
        } catch (RepositoryException e) {
            log.error("Can't login to hippo repository", e);
        } finally {
            if (localSession != null) {
                localSession.logout();
            }
        }
        Assert.assertNotNull(localSession);
    }

    /**
     * Test admin user for embargo node
     *
     * @throws Exception
     */
    @Test
    public void testAdminRightsToEmbargoWorkflow() throws Exception {
        final boolean b = adminSession.itemExists("/hippo:configuration/hippo:workflows/embargo");
        assertTrue(b);
    }

    /**
     * Test embargo users (embargo-author and embargo-editor) for the embargo node
     *
     * @throws Exception
     */
    @Test
    public void testEmbargoUserRightsToEmbargoDomain() throws Exception {
        final boolean a = embargoEditor.itemExists("/hippo:configuration/hippo:workflows/embargo");
        assertTrue(a);

        final boolean b = embargoAuthor.itemExists("/hippo:configuration/hippo:workflows/embargo");
        assertTrue(b);
    }

    /**
     * Test editor for the embargo node
     *
     * @throws Exception
     */
    @Test
    public void testEditorRightsToEmbargo() throws Exception {
        final boolean b = editor.itemExists("/hippo:configuration/hippo:workflows/embargo");
        assertFalse(b);
    }

    /**
     * Test if embargo namespace is loaded.
     *
     * @throws RepositoryException
     */
    @Test
    public void testEmbargoTestNamespaceAvailable() throws RepositoryException {
        String expectedUri = "http://www.onehippo.org/embargo/nt/1.0";
        final String uri = adminSession.getWorkspace().getNamespaceRegistry().getURI("embargo");
        assertEquals(expectedUri, uri);
    }

    /**
     * We need to test if the threepane workflow works (jdo complains) because the embargo is tied to the three pane
     * workflow to execute the workflow event on embargo users.
     *
     * @throws Exception
     */
    @Test
    public void testDocumentCreationWithThreePaneWorkflow() throws Exception {
        //testing creating a document with the embargo user.
        final Node embargoEditorFolderNode = embargoEditor.getNode(TestConstants.CONTENT_DOCUMENTS_EMBARGODEMO_PATH);
        final FolderWorkflow embargoEditorFolderWorkflow = (FolderWorkflow) getWorkflow(embargoEditorFolderNode, "threepane");
        final String embargoEditorDocumentLocation = embargoEditorFolderWorkflow.add("new-document", "embargotest:document", TestConstants.TEST_DOCUMENT_NAME);
        assertTrue(embargoEditor.itemExists(embargoEditorDocumentLocation));
    }

    /**
     * We now test if a document created by a normal editor does not put the mixins on the documents.
     *
     * @throws Exception
     */
    @Test
    public void testDocumentCreationWorkflowWithoutSettingEmbargoMixinForNormalUser() throws Exception {
        //testing creating a document with the embargo user.
        final Node editorFolderNode = editor.getNode(TestConstants.CONTENT_DOCUMENTS_EMBARGODEMO_PATH);
        final FolderWorkflow adminFolderWorkflow = (FolderWorkflow) getWorkflow(editorFolderNode, "threepane");
        final String documentLocation = adminFolderWorkflow.add("new-document", "embargotest:document", TestConstants.TEST_DOCUMENT_NAME);
        assertTrue(embargoEditor.itemExists(documentLocation));
        assertTrue(editor.itemExists(documentLocation));
        final NodeType[] mixinNodeTypes = editor.getNode(documentLocation).getMixinNodeTypes();
        List<String> nodetypes = new ArrayList<>();
        for(NodeType nodeType : mixinNodeTypes) {
            nodetypes.add(nodeType.getName());
        }
        assertFalse(nodetypes.contains(EmbargoConstants.EMBARGO_DOCUMENT_MIXIN_NAME));
        assertFalse(nodetypes.contains(EmbargoConstants.EMBARGO_MIXIN_NAME));
    }

    /**
     * We now test if a document created by an embargo editor is viewable by an ordinary editor without embargo rights.
     * We are also testing the workflow event.
     *
     * @throws Exception
     */
    @Ignore
    @Test
    public void testDocumentCreationWorkflowAccessLevelAndWorkflowEventOnCreationWithEmbargoUser() throws Exception {
        //testing creating a document with the embargo user.
        final Node embargoEditorFolderNode = embargoEditor.getNode(TestConstants.CONTENT_DOCUMENTS_EMBARGODEMO_PATH);
        final FolderWorkflow adminFolderWorkflow = (FolderWorkflow) getWorkflow(embargoEditorFolderNode, "threepane");
        final String embargoEditorDocumentLocation = adminFolderWorkflow.add("new-document", "embargotest:document", TestConstants.TEST_DOCUMENT_NAME);
        assertTrue(embargoEditor.itemExists(embargoEditorDocumentLocation));
        assertFalse(editor.itemExists(embargoEditorDocumentLocation));
    }

    /**
     * We now test if a document created by an embargo editor is viewable by an ordinary editor without embargo rights.
     * We are also testing the workflow event for both adding a document and copying a document.
     *
     * @throws Exception
     */
    @Ignore
    @Test
    public void testDocumentCreationWorkflowAccessLevelAndWorkflowEventOnCopyWithEmbargoUser() throws Exception {
        //testing creating a document with the embargo user.
        final Node embargoEditorFolderNode = embargoEditor.getNode(TestConstants.CONTENT_DOCUMENTS_EMBARGODEMO_PATH);
        final FolderWorkflow adminFolderWorkflow = (FolderWorkflow) getWorkflow(embargoEditorFolderNode, "threepane");
        final String embargoEditorDocumentLocation = adminFolderWorkflow.add("new-document", "embargotest:document", TestConstants.TEST_DOCUMENT_NAME);
        assertTrue(embargoEditor.itemExists(embargoEditorDocumentLocation));
        assertFalse(editor.itemExists(embargoEditorDocumentLocation));

        final Node node = embargoEditorFolderNode.getSession().getNode(embargoEditorDocumentLocation);
        //source, target folder, name
        final Document document = adminFolderWorkflow.copy(new Document(node.getIdentifier()), new Document(embargoEditorFolderNode.getIdentifier()), TestConstants.TEST_DOCUMENT_NAME + "1");
        assertTrue(embargoEditor.getNodeByIdentifier(document.getIdentity())!=null);

    }


    /**
     * Test if we can successfully retrieve the right embargo workflow with different users, embargo and non embargo
     * users.
     *
     * @throws Exception
     */
    @Test
    public void testEmbargoWorkflowExistenceOnDocumentForDifferentUsers() throws Exception {
        //testing creating a document with the editor user.
        final Node editorFolderNode = editor.getNode(TestConstants.CONTENT_DOCUMENTS_EMBARGODEMO_PATH);
        final FolderWorkflow editorFolderWorkflow = (FolderWorkflow) getWorkflow(editorFolderNode, "internal");
        final String editorDocumentLocation = editorFolderWorkflow.add("new-document", "embargotest:document", TestConstants.TEST_DOCUMENT_NAME);
        assertTrue(editor.itemExists(editorDocumentLocation));

        final Workflow editorsEmbargoWorkflow = getWorkflow(editor.getNode(editorDocumentLocation), "embargo");
        //test is editor is allowed to have the embargo workflow
        assertNull(editorsEmbargoWorkflow);

        final Workflow embargoEditorsEmbargoWorkflow = getWorkflow(embargoEditor.getNode(editorDocumentLocation), "embargo");
        //test is embargo editor is allowed to have the embargo workflow
        assertNotNull(embargoEditorsEmbargoWorkflow);

        //check if instance type is embargo workflow
        assertTrue(embargoEditorsEmbargoWorkflow instanceof EmbargoWorkflow);
    }


    /**
     * Test if we can add an embargo on a document created by somebody else and see if it is accessible afterwards for non
     * embargo users.
     *
     * @throws Exception
     */
    @Test
    public void testAddEmbargoOnDocumentAndCheckAccessibilityWithDifferentUsers() throws Exception {
        //testing creating a document with the editor user.
        final Node editorFolderNode = editor.getNode(TestConstants.CONTENT_DOCUMENTS_EMBARGODEMO_PATH);
        final FolderWorkflow editorFolderWorkflow = (FolderWorkflow) getWorkflow(editorFolderNode, "internal");
        final String editorDocumentLocation = editorFolderWorkflow.add("new-document", "embargotest:document", TestConstants.TEST_DOCUMENT_NAME);
        assertTrue(editor.itemExists(editorDocumentLocation));
        final EmbargoWorkflow embargoEditorsEmbargoWorkflow = (EmbargoWorkflow) getWorkflow(embargoEditor.getNode(editorDocumentLocation), "embargo");

        final String subejctId = editor.getNode(editorDocumentLocation).getIdentifier();

        embargoEditorsEmbargoWorkflow.addEmbargo(embargoEditor.getUserID(), subejctId, null);

        assertTrue(embargoEditor.itemExists(editorDocumentLocation));
        assertFalse(editor.itemExists(editorDocumentLocation));

    }

    /**
     * Test if we can add an embargo on a document created by somebody else and see if it is accessible afterwards for non
     * embargo users.. and then remove it.
     *
     * @throws Exception
     */
    @Test
    public void testRemovalOfEmbargoAndCheckAccessibilityWithDifferentUsers() throws Exception {
        //testing creating a document with the editor user.
        final Node editorFolderNode = editor.getNode(TestConstants.CONTENT_DOCUMENTS_EMBARGODEMO_PATH);
        final FolderWorkflow editorFolderWorkflow = (FolderWorkflow) getWorkflow(editorFolderNode, "internal");
        final String editorDocumentLocation = editorFolderWorkflow.add("new-document", "embargotest:document", TestConstants.TEST_DOCUMENT_NAME);
        assertTrue(editor.itemExists(editorDocumentLocation));

        final EmbargoWorkflow embargoEditorsEmbargoWorkflow = (EmbargoWorkflow) getWorkflow(embargoEditor.getNode(editorDocumentLocation), "embargo");

        final String subjectId = editor.getNode(editorDocumentLocation).getIdentifier();
        embargoEditorsEmbargoWorkflow.addEmbargo(embargoEditor.getUserID(), subjectId, null);

        assertTrue(embargoEditor.itemExists(editorDocumentLocation));
        assertFalse(editor.itemExists(editorDocumentLocation));

        embargoEditorsEmbargoWorkflow.removeEmbargo(subjectId);

        assertTrue(embargoEditor.itemExists(editorDocumentLocation));
        assertTrue(editor.itemExists(editorDocumentLocation));
    }


    /**
     * Test if we can add an embargo on a document created by somebody else and see if it is visible afterwards for non
     * embargo users.. and then schedule to remove embargo and check accessibility.
     *
     * @throws Exception
     */
    @Test
    public void testScheduledRemovalOfEmbargoAndCheckAccessibilityWithDifferentUsers() throws Exception {
        //testing creating a document with the editor user.
        final Node editorFolderNode = editor.getNode(TestConstants.CONTENT_DOCUMENTS_EMBARGODEMO_PATH);
        final FolderWorkflow editorFolderWorkflow = (FolderWorkflow) getWorkflow(editorFolderNode, "internal");
        final String editorDocumentLocation = editorFolderWorkflow.add("new-document", "embargotest:document", TestConstants.TEST_DOCUMENT_NAME);
        assertTrue(editor.itemExists(editorDocumentLocation));

        final EmbargoWorkflow embargoEditorsEmbargoWorkflow = (EmbargoWorkflow) getWorkflow(embargoEditor.getNode(editorDocumentLocation), "embargo");

        final String subjectId = editor.getNode(editorDocumentLocation).getIdentifier();

        embargoEditorsEmbargoWorkflow.addEmbargo(embargoEditor.getUserID(), subjectId,null);

        assertTrue(embargoEditor.itemExists(editorDocumentLocation));
        assertFalse(editor.itemExists(editorDocumentLocation));

        //creating a calendar object which removes embargo 10 seconds in the future

        final Calendar scheduledTime = Calendar.getInstance();
        scheduledTime.add(Calendar.SECOND, 2);

        embargoEditorsEmbargoWorkflow.scheduleRemoveEmbargo(subjectId, scheduledTime);

        assertFalse(editor.itemExists(editorDocumentLocation));

        //wait 3 seconds just to be sure
        Thread.sleep(3000);

        assertTrue(embargoEditor.itemExists(editorDocumentLocation));
        assertTrue(editor.itemExists(editorDocumentLocation));
    }


    @Override
    @After
    public void tearDown() throws Exception {
        logoutIfNotNull(editor);
        logoutIfNotNull(embargoAuthor);
        logoutIfNotNull(embargoEditor);

        super.tearDown();
    }

    private static void logoutIfNotNull(final Session session) {
        if (session != null) {
            session.logout();
        }
    }

    protected Workflow getWorkflow(Node node, String category) throws RepositoryException {
        WorkflowManager workflowManager = ((HippoWorkspace) node.getSession().getWorkspace()).getWorkflowManager();
        Node canonicalNode = ((HippoNode) node).getCanonicalNode();
        return workflowManager.getWorkflow(category, canonicalNode);
    }


    private void printTree(Node node) {
        try {
            System.out.println(node.getPath());
            final NodeIterator nodes = node.getNodes();
            while(nodes.hasNext()) {
                final Node childNode = nodes.nextNode();
                if(!((HippoNode)childNode).isVirtual()) {
                    printTree(childNode);
                }
            }
        } catch (RepositoryException e) {
            log.error("Repository error", e);
        }
    }

}
