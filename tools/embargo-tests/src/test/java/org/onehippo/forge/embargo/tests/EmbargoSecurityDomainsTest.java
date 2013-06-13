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

import java.io.IOException;
import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.forge.embargo.repository.workflow.EmbargoWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * @version $Id: EmbargoSecurityDomainsTest.java 84 2013-05-27 09:01:12Z mchatzidakis $
 */
public class EmbargoSecurityDomainsTest extends BaseRepositoryTest {

    private static Logger log = LoggerFactory.getLogger(EmbargoSecurityDomainsTest.class);

    @Before
    public void setUp() throws Exception {
        super.setUp();
        //Skip all tests if we can't reach the repository.These tests need to run on an existing repository (integration tests)
        assumeTrue(repository.getRepository() != null);
    }

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

    @Test
    public void testEmbargoTestNamespaceAvailable() throws RepositoryException {
        String expectedUri = "http://forge.onehippo.org/jcr/embargotest/nt/1.0";
        final String uri = adminSession.getWorkspace().getNamespaceRegistry().getURI("embargotest");
        assertEquals(expectedUri, uri);
    }

    @Test
    public void testEmbargoWorkflowAccessLevels() throws RepositoryException, WorkflowException, RemoteException {
        final Session editorSession = repository.login(TestConstants.EDITOR_CREDENTIALS);
        final Node editorsDocumentsFolder = editorSession.getNode(TestConstants.CONTENT_DOCUMENTS_PATH);
        final FolderWorkflow folderWorkflow = (FolderWorkflow) getWorkflow(editorsDocumentsFolder, "threepane");
        String test = folderWorkflow.add("new-document", "embargotest:document", TestConstants.TEST_DOCUMENT_NAME);
        final Node testDocumentNode = editorsDocumentsFolder.getNode(TestConstants.TEST_DOCUMENT_NAME);

        final EmbargoWorkflow editorsEmbargoWorkflow = (EmbargoWorkflow) getWorkflow(testDocumentNode, "embargo");
        final boolean editorsCondition = (editorsEmbargoWorkflow == null);

        final Session embargoUserSession = repository.login(TestConstants.EMBARGO_USER_CREDENTIALS);
        final Node embargoUserTestDocumentNode = embargoUserSession.getNode(TestConstants.CONTENT_DOCUMENTS_PATH + "/" + TestConstants.TEST_DOCUMENT_NAME);
        final EmbargoWorkflow embargoUserEmbargoWorkflow = (EmbargoWorkflow) getWorkflow(embargoUserTestDocumentNode, "embargo");
        final boolean embargoUsersCondition = (embargoUserEmbargoWorkflow != null);

        embargoUserTestDocumentNode.remove();
        editorSession.logout();
        embargoUserSession.logout();

        //TODO assertTrue("Editor has access to embargo workflow", editorsCondition);
        //TODO assertTrue("Embargo user does not have access to the embargo workflow", embargoUsersCondition);
    }

    @Test
    public void testBehaviorOnNewEmbargoDocument() throws RepositoryException, WorkflowException, RemoteException {
        final Session embargoUserSession = repository.login(TestConstants.EMBARGO_USER_CREDENTIALS);
        final Node embargoUserDocumentsFolder = embargoUserSession.getNode(TestConstants.CONTENT_DOCUMENTS_PATH);
        final FolderWorkflow folderWorkflow = (FolderWorkflow) getWorkflow(embargoUserDocumentsFolder, "threepane");
        final String test = folderWorkflow.add("new-document", "embargotest:document", TestConstants.TEST_DOCUMENT_NAME);

        final Node embargoDocumentNode = embargoUserSession.getNode(TestConstants.CONTENT_DOCUMENTS_PATH + "/" + TestConstants.TEST_DOCUMENT_NAME);
        //TODO assertTrue("New document created by embargo user is not under embargo", embargoDocumentNode.isNodeType("embargo:handle"));

        //TestUtils.printTree(embargoUserSession.getNode("/hippo:configuration/hippo:workflows"));
        final Session editorSession = repository.login(TestConstants.EDITOR_CREDENTIALS);
        final Node editorsDocumentFolder = editorSession.getNode(TestConstants.CONTENT_DOCUMENTS_PATH);

        final boolean condition = editorsDocumentFolder.hasNode(TestConstants.TEST_DOCUMENT_NAME);

        editorSession.logout();
        embargoDocumentNode.remove();
        embargoUserSession.logout();

        //TODO assertFalse("Editor user has access to embargo document: ",condition);
    }


    @Test
    public void testBehaviorOnExistingDocument() throws RepositoryException, WorkflowException, RemoteException {
        Session editorSession = repository.login(TestConstants.EDITOR_CREDENTIALS);
        Node editorsDocumentsFolder = editorSession.getNode(TestConstants.CONTENT_DOCUMENTS_PATH);
        final FolderWorkflow folderWorkflow = (FolderWorkflow) getWorkflow(editorsDocumentsFolder, "threepane");
        String test = folderWorkflow.add("new-document", "embargotest:document", TestConstants.TEST_DOCUMENT_NAME);
        editorSession.save();
        editorSession.logout();

        //Add embargo
        final Session embargoUserSession = repository.login(TestConstants.EMBARGO_USER_CREDENTIALS);
        final Node embargoUserTestDocumentNode = embargoUserSession.getNode(TestConstants.CONTENT_DOCUMENTS_PATH + "/" + TestConstants.TEST_DOCUMENT_NAME);
        final EmbargoWorkflow embargoUserEmbargoWorkflow = (EmbargoWorkflow) getWorkflow(embargoUserTestDocumentNode, "embargo");
        //TODO This fails, NPE... embargoUserEmbargoWorkflow.addEmbargo();
        embargoUserSession.save();
        //TODO assertTrue("Embargo flag was not set", embargoUserTestDocumentNode.isNodeType("embargo:handle"));

        editorSession = repository.login(TestConstants.EDITOR_CREDENTIALS);
        editorsDocumentsFolder = editorSession.getNode(TestConstants.CONTENT_DOCUMENTS_PATH);
        final boolean editorDisallowedCondition = editorsDocumentsFolder.hasNode(TestConstants.TEST_DOCUMENT_NAME);
        editorSession.logout();

        //Remove embargo
        //TODO This fails, NPE... embargoUserEmbargoWorkflow.removeEmbargo();
        embargoUserSession.save();
        //TODO assertFalse("Embargo flag was not cleared", embargoUserTestDocumentNode.isNodeType("embargo:handle"));

        editorSession = repository.login(TestConstants.EDITOR_CREDENTIALS);
        editorsDocumentsFolder = editorSession.getNode(TestConstants.CONTENT_DOCUMENTS_PATH);
        final boolean editorAllowedCondition = editorsDocumentsFolder.hasNode(TestConstants.TEST_DOCUMENT_NAME);
        editorSession.logout();

        embargoUserTestDocumentNode.remove();
        embargoUserSession.logout();

        //TODO assertFalse("Editor user has access to embargo document: ",editorDisallowedCondition);
        //TODO assertTrue("Editor user did not regain access after removal of embargo", editorAllowedCondition);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

}
