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

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.forge.embargo.repository.workflow.EmbargoWorkflow;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replay;

/**
 * @version $Id: EmbargoSecurityDomainsTest.java 84 2013-05-27 09:01:12Z mchatzidakis $
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({org.apache.wicket.Session.class})
@PowerMockIgnore("javax.management.*")
public class EmbargoSecurityDomainsTest extends BaseRepositoryTest {

    private static Logger log = LoggerFactory.getLogger(EmbargoSecurityDomainsTest.class);
    private Session adminSession;
    private Session embargoEditor;
    private Session embargoAuthor;
    private Session editor;


    @Before
    public void setUp() throws Exception {
        super.setUp();
        //Skip all tests if we can't reach the repository.These tests need to run on an existing repository (integration tests)
        assumeTrue(repository.getRepository() != null);
        //load internal workflow before we can use any other one (needed to split up functionality in different methods)
        adminSession = repository.login(TestConstants.ADMIN_CREDENTIALS);
        assumeNotNull(adminSession);
        final Node adminDocumentsFolder = adminSession.getNode(TestConstants.CONTENT_DOCUMENTS_EMBARGODEMO_PATH);
        final FolderWorkflow adminFolderWorkflow = (FolderWorkflow) getWorkflow(adminDocumentsFolder, "internal");
        //(normal) admin creates a news document
        final String adminDocumentLocation = adminFolderWorkflow.add("new-document", "embargodemo:newsdocument", TestConstants.TEST_DOCUMENT_NAME);
        //quick check if document exitst, and now the threepane workflow is enabled for some jdo reason...
        assumeTrue(adminSession.itemExists(adminDocumentLocation));
        //prepare sessions:
        editor = repository.login(TestConstants.EDITOR_CREDENTIALS);
        embargoEditor = repository.login(TestConstants.EMBARGO_EDITOR_CREDENTIALS);
        embargoAuthor = repository.login(TestConstants.EMBARGO_AUTHOR_CREDENTIALS);

        /**
         *  Need powermock to mock the wicket Usersession: org.onehippo.forge.embargo.repository.workflow.EmbargoWorkflowImpl#addEmbargo()
         */
        UserSession userSession = createNiceMock(UserSession.class);
        mockStatic(org.apache.wicket.Session.class);
        expect(org.apache.wicket.Session.get()).andReturn(userSession).anyTimes();
        expect(userSession.getJcrSession()).andReturn(embargoEditor).anyTimes();
        replay(org.apache.wicket.Session.class, userSession);
        /**
         *  end powermock code
         */

    }

    /**
     * Test admin user for embargo node
     *
     * @throws Exception
     */
    @Test
    public void testAdminRightsToEmbargo() throws Exception {
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
     * Test repository connection with admin credentials .. assuming everything goes well
     */
    @Test
    public void testConnectToRepository() {
        Session localSession = null;
        try {
            localSession = repository.login(TestConstants.ADMIN_CREDENTIALS);
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
        final String embargoEditorDocumentLocation = embargoEditorFolderWorkflow.add("new-document", "embargodemo:newsdocument", TestConstants.TEST_DOCUMENT_NAME);
        assertTrue(embargoEditor.itemExists(embargoEditorDocumentLocation));
    }


    /**
     * We now test if a document created by an embargo editor is viewable by an ordinary editor without embargo rights.
     * We are also testing the workflow event.
     *
     * @throws Exception
     */
    @Test
    public void testDocumentCreationWorkflowAccessLevelAndWorkflowEventOnCreationWithEmbargoUser() throws Exception {
        //testing creating a document with the embargo user.
        final Node embargoEditorFolderNode = embargoEditor.getNode(TestConstants.CONTENT_DOCUMENTS_EMBARGODEMO_PATH);
        final FolderWorkflow adminFolderWorkflow = (FolderWorkflow) getWorkflow(embargoEditorFolderNode, "threepane");
        final String embargoEditorDocumentLocation = adminFolderWorkflow.add("new-document", "embargodemo:newsdocument", TestConstants.TEST_DOCUMENT_NAME);
        assertTrue(embargoEditor.itemExists(embargoEditorDocumentLocation));
        assertFalse(editor.itemExists(embargoEditorDocumentLocation));
    }


    /**
     * Test if we can successfully retrieve the right embargo workflow with different users, embargo and non embargo
     * users.
     *
     * @throws Exception
     */
    @Test
    public void testEmbargoWorkflowExistenceOnDocumentForDifferentUsers() throws Exception {
        //testing creating a document with the embargo user.
        final Node editorFolderNode = editor.getNode(TestConstants.CONTENT_DOCUMENTS_EMBARGODEMO_PATH);
        final FolderWorkflow editorFolderWorkflow = (FolderWorkflow) getWorkflow(editorFolderNode, "internal");
        final String editorDocumentLocation = editorFolderWorkflow.add("new-document", "embargodemo:newsdocument", TestConstants.TEST_DOCUMENT_NAME);
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
        //testing creating a document with the embargo user.
        final Node editorFolderNode = editor.getNode(TestConstants.CONTENT_DOCUMENTS_EMBARGODEMO_PATH);
        final FolderWorkflow editorFolderWorkflow = (FolderWorkflow) getWorkflow(editorFolderNode, "internal");
        final String editorDocumentLocation = editorFolderWorkflow.add("new-document", "embargodemo:newsdocument", TestConstants.TEST_DOCUMENT_NAME);
        assertTrue(editor.itemExists(editorDocumentLocation));

        final EmbargoWorkflow embargoEditorsEmbargoWorkflow = (EmbargoWorkflow) getWorkflow(embargoEditor.getNode(editorDocumentLocation), "embargo");

        embargoEditorsEmbargoWorkflow.addEmbargo();

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
        //testing creating a document with the embargo user.
        final Node editorFolderNode = editor.getNode(TestConstants.CONTENT_DOCUMENTS_EMBARGODEMO_PATH);
        final FolderWorkflow editorFolderWorkflow = (FolderWorkflow) getWorkflow(editorFolderNode, "internal");
        final String editorDocumentLocation = editorFolderWorkflow.add("new-document", "embargodemo:newsdocument", TestConstants.TEST_DOCUMENT_NAME);
        assertTrue(editor.itemExists(editorDocumentLocation));

        final EmbargoWorkflow embargoEditorsEmbargoWorkflow = (EmbargoWorkflow) getWorkflow(embargoEditor.getNode(editorDocumentLocation), "embargo");

        embargoEditorsEmbargoWorkflow.addEmbargo();

        assertTrue(embargoEditor.itemExists(editorDocumentLocation));
        assertFalse(editor.itemExists(editorDocumentLocation));

        embargoEditorsEmbargoWorkflow.removeEmbargo();

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
        //testing creating a document with the embargo user.
        final Node editorFolderNode = editor.getNode(TestConstants.CONTENT_DOCUMENTS_EMBARGODEMO_PATH);
        final FolderWorkflow editorFolderWorkflow = (FolderWorkflow) getWorkflow(editorFolderNode, "internal");
        final String editorDocumentLocation = editorFolderWorkflow.add("new-document", "embargodemo:newsdocument", TestConstants.TEST_DOCUMENT_NAME);
        assertTrue(editor.itemExists(editorDocumentLocation));

        final EmbargoWorkflow embargoEditorsEmbargoWorkflow = (EmbargoWorkflow) getWorkflow(embargoEditor.getNode(editorDocumentLocation), "embargo");

        embargoEditorsEmbargoWorkflow.addEmbargo();

        assertTrue(embargoEditor.itemExists(editorDocumentLocation));
        assertFalse(editor.itemExists(editorDocumentLocation));

        //creating a calendar object which removes embargo 10 seconds in the future

        final Calendar tenSecondFuture = Calendar.getInstance();
        tenSecondFuture.add(Calendar.SECOND, 10);

        embargoEditorsEmbargoWorkflow.scheduleRemoveEmbargo(tenSecondFuture);

        assertFalse(editor.itemExists(editorDocumentLocation));

        //wait 11 seconds just to be sure
        Thread.sleep(11000);

        assertTrue(embargoEditor.itemExists(editorDocumentLocation));
        assertTrue(editor.itemExists(editorDocumentLocation));
    }


    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        //logging out all sessions.. tests all passed!!!
        editor.logout();
        adminSession.logout();
        embargoAuthor.logout();
        embargoEditor.logout();
    }

}
