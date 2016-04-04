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

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowImpl;


/**
 * WorkflowEvent that checks if the embargo needs to be applied to a document being added to a folder.
 *
 * @version $Id$
 */

public class EmbargoEventsAddDocumentWorkflow extends WorkflowImpl  {

    private static final long serialVersionUID = 1L;

    public EmbargoEventsAddDocumentWorkflow() throws RemoteException {
        super();
    }


    public void fire() throws WorkflowException, RepositoryException, RemoteException {
    }


    public void fire(final Document document) throws WorkflowException, RepositoryException, RemoteException {
        final WorkflowContext workflowContext = getWorkflowContext();
        final Session internalWorkflowSession = workflowContext.getInternalWorkflowSession();
        Node newDocumentNode = internalWorkflowSession.getNodeByIdentifier(document.getIdentity());

        if (newDocumentNode.isNodeType(HippoNodeType.NT_DOCUMENT) && !newDocumentNode.isNodeType(HippoStdNodeType.NT_FOLDER)) {
            HippoWorkspace workspace = (HippoWorkspace)internalWorkflowSession.getWorkspace();
            Workflow embargo = workspace.getWorkflowManager().getWorkflow("embargo", newDocumentNode);
            //pass along the user id from this action, so the original user id is used for the embargo
            ((EmbargoWorkflow)embargo).addEmbargo(workflowContext.getUserIdentity(), document.getIdentity(),  null, true);
        }
    }

    public void fire(final Iterator<Document> documentIterator) throws WorkflowException, RepositoryException, RemoteException {
    }
}
