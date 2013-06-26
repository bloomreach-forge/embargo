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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowManager;
import org.junit.After;
import org.junit.Before;

/**
 * @author Jeroen Reijn, Minos and Kenan..
 */
public class BaseRepositoryTest {

    protected HippoRepository repository;
    protected Session adminSession;
    TestCase testCase;

    @Before
    public void setUp() throws Exception {
        this.testCase = new TestCase() {
        };
        TestCase.setUpClass(true);
        TestCase.fixture();
        this.repository = HippoRepositoryFactory.getHippoRepository();
        adminSession = repository.login(TestConstants.ADMIN_CREDENTIALS);
    }

    @After
    public void tearDown() throws Exception {
        if (repository != null) {
            repository.close();
        }
        TestCase.tearDownClass(true);
    }

    protected Workflow getWorkflow(Node node, String category) throws RepositoryException {
        WorkflowManager workflowManager = ((HippoWorkspace) node.getSession().getWorkspace()).getWorkflowManager();
        Node canonicalNode = ((HippoNode) node).getCanonicalNode();
        return workflowManager.getWorkflow(category, canonicalNode);
    }

}
