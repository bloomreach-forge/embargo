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
package org.onehippo.forge.embargo.repository.workflow;

import java.rmi.RemoteException;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jdo.annotations.DatastoreIdentity;
import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.PersistenceCapable;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.hippoecm.repository.standardworkflow.WorkflowEventWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */

@PersistenceCapable(identityType = IdentityType.DATASTORE, cacheable = "false", detachable = "false", table = "documents")
@DatastoreIdentity(strategy = IdGeneratorStrategy.NATIVE)
@Inheritance(strategy = InheritanceStrategy.SUBCLASS_TABLE)
@Discriminator(strategy = DiscriminatorStrategy.CLASS_NAME)
public class EmbargoEventsAddDocumentWorkflow extends WorkflowImpl implements WorkflowEventWorkflow {

    private static final long serialVersionUID = 1L;
    private final static Logger log = LoggerFactory.getLogger(EmbargoEventsAddDocumentWorkflow.class);

    public EmbargoEventsAddDocumentWorkflow() throws RemoteException {
        super();
    }

    @Override
    public void fire() throws WorkflowException, MappingException, RepositoryException, RemoteException {
    }

    @Override
    public void fire(final Document document) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        final WorkflowContext workflowContext = getWorkflowContext();
        final Session internalWorkflowSession = workflowContext.getInternalWorkflowSession();
        Node newDocumentNode = internalWorkflowSession.getNodeByIdentifier(document.getIdentity());

        if (newDocumentNode.isNodeType(HippoNodeType.NT_DOCUMENT) && !newDocumentNode.isNodeType(HippoStdNodeType.NT_FOLDER)) {
            HippoWorkspace workspace = (HippoWorkspace) internalWorkflowSession.getWorkspace();
            Workflow embargo = workspace.getWorkflowManager().getWorkflow("embargo", newDocumentNode);
            ((EmbargoWorkflow) embargo).addEmbargo();
        }
    }

    @Override
    public void fire(final Iterator<Document> documentIterator) throws WorkflowException, MappingException, RepositoryException, RemoteException {
    }
}
