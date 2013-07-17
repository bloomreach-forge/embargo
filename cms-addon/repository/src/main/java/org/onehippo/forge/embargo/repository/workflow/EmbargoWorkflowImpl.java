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
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jdo.annotations.DatastoreIdentity;
import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

import org.apache.wicket.RequestCycle;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.onehippo.forge.embargo.repository.EmbargoConstants;
import org.onehippo.forge.embargo.repository.EmbargoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
@PersistenceCapable(identityType = IdentityType.DATASTORE, cacheable = "true", detachable = "false", table = "documents")
@DatastoreIdentity(strategy = IdGeneratorStrategy.NATIVE)
@Discriminator(strategy = DiscriminatorStrategy.CLASS_NAME)
public class EmbargoWorkflowImpl extends WorkflowImpl implements EmbargoWorkflow {

    private static Logger log = LoggerFactory.getLogger(EmbargoWorkflowImpl.class);
    private static final long serialVersionUID = 1L;

    @Persistent(column = "jcr:uuid")
    protected String uuid;

    /**
     * @throws java.rmi.RemoteException
     */
    public EmbargoWorkflowImpl() throws RemoteException {
    }

    @Override
    public void addEmbargo() throws WorkflowException, RepositoryException, MappingException, RemoteException {
        final WorkflowContext workflowContext = getWorkflowContext();
        final Session internalWorkflowSession = workflowContext.getInternalWorkflowSession();
        //Check for the Wicket requestContext. The user will be fetched from the Wicket Session.
        if(RequestCycle.get()!=null) {
            String invokingUserId = ((UserSession) org.apache.wicket.Session.get()).getJcrSession().getUserID();

            final Node handle = internalWorkflowSession.getNodeByIdentifier(uuid).getParent();
            if (!handle.isCheckedOut()) {
                internalWorkflowSession.getWorkspace().getVersionManager().checkout(handle.getPath());
            }

            String[] userEmbargoEnabledGroups = EmbargoUtils.getCurrentUserEmbargoEnabledGroups(internalWorkflowSession, invokingUserId);
            if (userEmbargoEnabledGroups.length > 0) {

                //Set embargo mixin on handle & add group information
                handle.addMixin(EmbargoConstants.EMBARGO_MIXIN_NAME);
                handle.setProperty(
                        EmbargoConstants.EMBARGO_GROUP_PROPERTY_NAME,
                        userEmbargoEnabledGroups);

                //Set embargo mixin on the document
                for(Node documentNode : EmbargoUtils.getDocumentVariants(handle)){
                    if (!documentNode.isCheckedOut()) {
                        internalWorkflowSession.getWorkspace().getVersionManager().checkout(documentNode.getPath());
                    }
                    documentNode.addMixin(EmbargoConstants.EMBARGO_DOCUMENT_MIXIN_NAME);
                }

                internalWorkflowSession.save();
            }
        }
    }

    @Override
    public void removeEmbargo() throws WorkflowException, RepositoryException, MappingException, RemoteException {
        final WorkflowContext workflowContext = getWorkflowContext();
        final Session internalWorkflowSession = workflowContext.getInternalWorkflowSession();

        final Node handle = internalWorkflowSession.getNodeByIdentifier(uuid).getParent();
        if (!handle.isCheckedOut()) {
            internalWorkflowSession.getWorkspace().getVersionManager().checkout(handle.getPath());
        }
        //remove embargo:groups
        if (handle.hasProperty(EmbargoConstants.EMBARGO_GROUP_PROPERTY_NAME)) {
            handle.getProperty(EmbargoConstants.EMBARGO_GROUP_PROPERTY_NAME).remove();
        }
        //remove any embargo:request
        if (handle.hasNode(EmbargoConstants.EMBARGO_SCHEDULE_REQUEST_NODE_NAME)) {
            handle.getNode(EmbargoConstants.EMBARGO_SCHEDULE_REQUEST_NODE_NAME).remove();
        }
        //remove embargo mixin from handle
        handle.removeMixin(EmbargoConstants.EMBARGO_MIXIN_NAME);

        //remove embargo mixin from document(s)
        for(Node documentNode : EmbargoUtils.getDocumentVariants(handle)){
            if (!documentNode.isCheckedOut()) {
                internalWorkflowSession.getWorkspace().getVersionManager().checkout(documentNode.getPath());
            }
            documentNode.removeMixin(EmbargoConstants.EMBARGO_DOCUMENT_MIXIN_NAME);
        }

        internalWorkflowSession.save();
    }

    @Override
    public void scheduleRemoveEmbargo(final Calendar publicationDate) throws WorkflowException, RepositoryException, MappingException, RemoteException {
        cancelSchedule();
        WorkflowContext wfCtx = getWorkflowContext().getWorkflowContext(publicationDate);
        EmbargoWorkflow wf = (EmbargoWorkflow) wfCtx.getWorkflow("embargo");
        wf.removeEmbargo();
    }

    @Override
    public void cancelSchedule() throws WorkflowException, RepositoryException, MappingException, RemoteException {
        final WorkflowContext workflowContext = getWorkflowContext();
        final Session internalWorkflowSession = workflowContext.getInternalWorkflowSession();
        final Node handle = internalWorkflowSession.getNodeByIdentifier(uuid).getParent();
        
        if (!handle.isCheckedOut()) {
            internalWorkflowSession.getWorkspace().getVersionManager().checkout(handle.getPath());
        }

        if (handle.hasNode(EmbargoConstants.EMBARGO_SCHEDULE_REQUEST_NODE_NAME)) {
            handle.getNode(EmbargoConstants.EMBARGO_SCHEDULE_REQUEST_NODE_NAME).remove();
            internalWorkflowSession.save();
        }

    }
}
