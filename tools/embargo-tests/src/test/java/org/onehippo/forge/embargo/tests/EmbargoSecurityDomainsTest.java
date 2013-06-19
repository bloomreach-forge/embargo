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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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

    @Before
    public void setUp() throws Exception {
        super.setUp();
        //Skip all tests if we can't reach the repository.These tests need to run on an existing repository (integration tests)
        assumeTrue(repository.getRepository() != null);
    }

    /**
     * Test admin user for embargo node
     *
     * @throws Exception
     */
    @Test
    public void testAdminRightsToEmbargo() throws Exception {
        final Session editor = repository.login("admin", "admin".toCharArray());
        final boolean b = editor.itemExists("/hippo:configuration/hippo:workflows/embargo");
        assertTrue(b);
    }

    /**
     * Test embargo users (embargo-author and embargo-editor) for the embargo node
     *
     * @throws Exception
     */
    @Test
    public void testEmbargoUserRightsToEmbargoDomain() throws Exception {
        final Session editor = repository.login(TestConstants.EMBARGO_EDITOR_CREDENTIALS);
        final boolean a = editor.itemExists("/hippo:configuration/hippo:workflows/embargo");
        assertTrue(a);

        final Session author = repository.login(TestConstants.EMBARGO_AUTHOR_CREDENTIALS);
        final boolean b = author.itemExists("/hippo:configuration/hippo:workflows/embargo");
        assertTrue(b);
    }

    /**
     * Test editor for the embargo node
     *
     * @throws Exception
     */
    @Test
    public void testEditorRightsToEmbargo() throws Exception {
        final Session editor = repository.login("editor", "editor".toCharArray());
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
     * Testing embargo rights, functionally.
     *
     * Mostly with #itemExists to check if the authenticated session has access to the embargoed document
     * @throws Exception
     */
    @Test
    public void testEmbargoWorkflowAccessLevels() throws Exception {
        final Session editorSession = repository.login(TestConstants.EDITOR_CREDENTIALS);
        final Node editorsDocumentsFolder = editorSession.getNode(TestConstants.CONTENT_DOCUMENTS_EMBARGODEMO_PATH);

        final Session adminSession = repository.login(TestConstants.ADMIN_CREDENTIALS);
        final Node adminDocumentsFolder = adminSession.getNode(TestConstants.CONTENT_DOCUMENTS_EMBARGODEMO_PATH);

        final FolderWorkflow editorFolderWorkflow = (FolderWorkflow) getWorkflow(editorsDocumentsFolder, "internal");
        final FolderWorkflow adminFolderWorkflow = (FolderWorkflow) getWorkflow(adminDocumentsFolder, "internal");

        //(normal) editor creates a news document
        final String editorDocumentLocation = editorFolderWorkflow.add("new-document", "embargodemo:newsdocument", TestConstants.TEST_DOCUMENT_NAME);
        //(normal) admin creates a news document
        final String adminDocumentLocation = adminFolderWorkflow.add("new-document", "embargodemo:newsdocument", TestConstants.TEST_DOCUMENT_NAME);

        final Node testDocumentNode = editorsDocumentsFolder.getNode(TestConstants.TEST_DOCUMENT_NAME);
        //test if editor is allowed to view created document
        assertTrue(editorSession.itemExists(editorDocumentLocation));
        //test if editor is allowed to see document created by admin.
        assertTrue(editorSession.itemExists(adminDocumentLocation));

        final EmbargoWorkflow editorsEmbargoWorkflow = (EmbargoWorkflow) getWorkflow(testDocumentNode, "embargo");
        //test is editor is allowed to have the embargo workflow
        assertNull(editorsEmbargoWorkflow);


        final Workflow workflow = getWorkflow(adminSession.getNode(adminDocumentLocation), "embargo");
        //test if admin is allowed to retrieve embargo workflow
        assertTrue(workflow instanceof EmbargoWorkflow);// just can not do anything with it because the admin is not part of the embargo domain. So we continue.

        final Session embargoEditorSession = repository.login(TestConstants.EMBARGO_EDITOR_CREDENTIALS);
        final Workflow embargoEditorWorkflow = getWorkflow(embargoEditorSession.getNode(adminDocumentLocation), "embargo");

        //test if embargo editor is allowed to retrieve embargo workflow
        assertTrue(embargoEditorWorkflow instanceof EmbargoWorkflow);

        EmbargoWorkflow embargoWorkflow = (EmbargoWorkflow) embargoEditorWorkflow;

        /**
         * Need powermock to mock the wicket Usersession: org.onehippo.forge.embargo.repository.workflow.EmbargoWorkflowImpl#addEmbargo()
         */
        UserSession userSession = createNiceMock(UserSession.class);
        mockStatic(org.apache.wicket.Session.class);
        expect(org.apache.wicket.Session.get()).andReturn(userSession).anyTimes();
        expect(userSession.getJcrSession()).andReturn(embargoEditorSession).anyTimes();
        replay(org.apache.wicket.Session.class, userSession);
        /**
         * end powermock code
         */

        //execute adding embargo on the document the admin created as the embargo-editor: /content/documents/embargodemo/test[2]...
        embargoWorkflow.addEmbargo();

        //admin has access to the document (because it has access to everything)
        assertTrue(adminSession.itemExists(adminDocumentLocation));

        //editor does not have access to the document anymore because it is now under embargo.
        assertFalse(editorSession.itemExists(adminDocumentLocation));

        //removing embargo, editor should be able to see it again afterwards
        embargoWorkflow.removeEmbargo();
        assertTrue(editorSession.itemExists(adminDocumentLocation));

        //adding embargo, editor should not be able to see it again afterwards...  preparing for scheduling removal embargo
        embargoWorkflow.addEmbargo();
        assertFalse(editorSession.itemExists(adminDocumentLocation));

        //creating a calendar object which removes embargo 10 seconds in the future
        final Calendar tenSecondFuture = Calendar.getInstance();
        tenSecondFuture.add(Calendar.SECOND, 10);

        //schedule remove embargo with the 10 second in the future rule
        embargoWorkflow.scheduleRemoveEmbargo(tenSecondFuture);
        assertFalse(editorSession.itemExists(adminDocumentLocation));

        //wait 11 seconds just to be sure
        Thread.sleep(11000);

        //poof the document is visible again...  magic? awesomeness?
        assertTrue(editorSession.itemExists(adminDocumentLocation));

        //testing creating a document with the embargo user.
        final Node embargoEditorDocumentsFolder = embargoEditorSession.getNode(TestConstants.CONTENT_DOCUMENTS_EMBARGODEMO_PATH);
        final FolderWorkflow embargoEditorFolderWorkflow = (FolderWorkflow) getWorkflow(embargoEditorDocumentsFolder, "threepane");

        //(embargo) editor creates a news document, it should automatically trigger the workflow event..
        final String embargoEditorDocumentLocation = embargoEditorFolderWorkflow.add("new-document", "embargodemo:newsdocument", TestConstants.TEST_DOCUMENT_NAME);

        assertFalse(editorSession.itemExists(embargoEditorDocumentLocation)); //not visible is it??!

        //it should be visible for the embargo editor ofcourse
        assertTrue(embargoEditorSession.itemExists(embargoEditorDocumentLocation));

        //logging out.. tests all passed..!!!
        editorSession.logout();
        adminSession.logout();
        embargoEditorSession.logout();
    }


    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

}
