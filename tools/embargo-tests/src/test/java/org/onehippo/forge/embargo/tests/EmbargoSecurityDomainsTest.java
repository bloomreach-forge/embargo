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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.forge.embargo.tests.helpers.TestConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assume.assumeTrue;

/**
 * @version $Id: EmbargoSecurityDomainsTest.java 84 2013-05-27 09:01:12Z mchatzidakis $
 */
public class EmbargoSecurityDomainsTest extends BaseRepositoryTest {

    public static final String EMBARGO_USER_NAME = "embargouser";
    public static final String CONTENT_DOCUMENTS_PATH = "/content/documents";
    public static final String TEST_DOCUMENT_NAME = "test";
    private static Logger log = LoggerFactory.getLogger(EmbargoSecurityDomainsTest.class);

    @Before
    public void setUp() throws Exception {
        super.setUp();
        //Skip all tests if we can't reach the repository.
        //These tests need to run on an existing repository (integration tests)
        assumeTrue(repository.getRepository() != null);

        final Node usersNode = session.getNode("/hippo:configuration/hippo:users");
        if (!usersNode.hasNode(EMBARGO_USER_NAME)) {
            final Node embargoUserNode = usersNode.addNode(EMBARGO_USER_NAME, "hipposys:user");
            embargoUserNode.setProperty("hipposys:password", EMBARGO_USER_NAME);
            final Node groupsNode = session.getNode("/hippo:configuration/hippo:groups");
            final Node embargoGroup = groupsNode.getNode("embargo-editors-example-group");
            final Property property = embargoGroup.getProperty("hipposys:members");

            List<Value> valueList = new ArrayList<Value>(property.getValues().length);
            for(Value value : property.getValues()) {
                valueList.add(value);
            }

            final ValueFactory instance = org.apache.jackrabbit.value.ValueFactoryImpl.getInstance();
            valueList.add(instance.createValue(EMBARGO_USER_NAME));
            property.setValue(valueList.toArray(new Value[valueList.size()]));
            session.save();
        }

    }

    @Test
    public void testConnectToRepository() {
        Session localSession = null;
        try {
            localSession = repository.login(TestConstants.ADMIN_CREDENTIALS);
        } catch (RepositoryException e) {
            e.printStackTrace();
        } finally {
            localSession.logout();
        }
        Assert.assertNotNull(localSession);
    }

    @Test
    public void testEmbargoTestNamespaceAvailable() throws RepositoryException {
        String expectedUri = "http://forge.onehippo.org/jcr/embargotest/nt/1.0";
        final String uri = session.getWorkspace().getNamespaceRegistry().getURI("embargotest");
        assertEquals(expectedUri, uri);
    }

    @Test
    public void testDeniedAccess() throws RepositoryException, WorkflowException, RemoteException {
        final Session embargoSession = repository.login(EMBARGO_USER_NAME, EMBARGO_USER_NAME.toCharArray());
        final Node documentsFolder = embargoSession.getNode(CONTENT_DOCUMENTS_PATH);
        final FolderWorkflow folderWorkflow = (FolderWorkflow) getWorkflow(documentsFolder, "threepane");
        final String test = folderWorkflow.add("new-document", "embargotest:document", TEST_DOCUMENT_NAME);

        printTree(embargoSession.getNode("/hippo:configuration/hippo:workflows"));

        final Session editorSession = repository.login(TestConstants.EDITOR_CREDENTIALS);
        final Node documentsNode = editorSession.getNode(CONTENT_DOCUMENTS_PATH);

        final boolean condition = documentsNode.hasNode(TEST_DOCUMENT_NAME);
        //assertFalse("editor user has access to document: ",condition);

        editorSession.logout();
        embargoSession.getNode("/content/documents/test").remove();
        embargoSession.logout();
    }

    private void printTree(Node node) throws RepositoryException {
        printTree(node, 1);
    }

    private void printTree(Node node, int depth) throws RepositoryException {
        final NodeIterator nodeIterator = node.getNodes();
        while(nodeIterator.hasNext()) {
            final Node node1 = nodeIterator.nextNode();

            System.out.println(printDash(depth) + node1.getName());
            if(node1.hasNodes()) {
                printTree(node1, depth + 1);
            }
        }
    }

    private String printDash(int depth) {
        String dashes = "";
        for(int i=0;i<depth;i++) {
            if(i!=depth) {
                dashes +="-";
            }
        }
        return dashes;
    }

    @Test
    public void testCssFolder_ViewAsAdmin_FolderShown() {
        //Assert.assertTrue(queryReturnMultipleNodes("//formdata", TestConstants.ADMIN_CREDENTIALS));

    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

}
