/*
 * Copyright 2024 Bloomreach B.V. (http://www.bloomreach.com)
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

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang.ArrayUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.forge.embargo.repository.EmbargoConstants;
import org.onehippo.forge.embargo.repository.EmbargoUtils;
import org.onehippo.repository.scheduling.RepositoryJob;
import org.onehippo.repository.scheduling.RepositoryJobExecutionContext;
import org.onehippo.repository.scheduling.RepositoryJobInfo;
import org.onehippo.repository.scheduling.RepositoryJobSimpleTrigger;
import org.onehippo.repository.scheduling.RepositoryScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_METHOD_NAME;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_SUBJECT_ID;
import static org.onehippo.forge.embargo.repository.EmbargoConstants.EMBARGO_SCHEDULE_REQUEST_NODE_NAME;

public class EmbargoWorkflowImpl extends WorkflowImpl implements EmbargoWorkflow {

    private final static Logger log = LoggerFactory.getLogger(EmbargoWorkflowImpl.class);

    public static final String METHOD_REMOVE_EMBARGO = "removeEmbargo";

    public EmbargoWorkflowImpl() throws RemoteException {
    }

    @Override
    public void addEmbargo(final String userId, final String subjectId, final String[] forcedEmbargoGroups) throws RepositoryException {
        final WorkflowContext workflowContext = getWorkflowContext();
        final Session internalWorkflowSession = workflowContext.getInternalWorkflowSession();

        final Node handle = EmbargoUtils.extractHandle(internalWorkflowSession.getNodeByIdentifier(subjectId));
        if (!handle.isCheckedOut()) {
            internalWorkflowSession.getWorkspace().getVersionManager().checkout(handle.getPath());
        }

        String[] userEmbargoEnabledGroups = ArrayUtils.isEmpty(forcedEmbargoGroups) ?
                EmbargoUtils.getCurrentUserEmbargoEnabledGroups(internalWorkflowSession, userId) :
                forcedEmbargoGroups;

        if (userEmbargoEnabledGroups.length > 0) {

            //Set embargo mixin on handle & add group information
            handle.addMixin(EmbargoConstants.EMBARGO_MIXIN_NAME);
            handle.setProperty(EmbargoConstants.EMBARGO_GROUP_PROPERTY_NAME, userEmbargoEnabledGroups);

            //Set embargo mixin on the document
            addVariantsMixin(internalWorkflowSession, handle);

            internalWorkflowSession.save();
        } else {
            log.info("Trying to set the embargo on a document for user: {} who is not in any embargo enabled groups.", userId);
        }
    }

    @Override
    public void addEmbargoHandle(final String userId, final String subjectId, final String[] forcedEmbargoGroups) throws WorkflowException, RepositoryException, RemoteException {
        final WorkflowContext workflowContext = getWorkflowContext();
        final Session internalWorkflowSession = workflowContext.getInternalWorkflowSession();
        final Node handle = EmbargoUtils.extractHandle(internalWorkflowSession.getNodeByIdentifier(subjectId));
        if (!handle.isCheckedOut()) {
            internalWorkflowSession.getWorkspace().getVersionManager().checkout(handle.getPath());
        }

        String[] userEmbargoEnabledGroups = ArrayUtils.isEmpty(forcedEmbargoGroups) ?
                EmbargoUtils.getCurrentUserEmbargoEnabledGroups(internalWorkflowSession, userId) :
                forcedEmbargoGroups;

        if (userEmbargoEnabledGroups.length > 0) {

            //Set embargo mixin on handle & add group information
            handle.addMixin(EmbargoConstants.EMBARGO_MIXIN_NAME);
            handle.setProperty(EmbargoConstants.EMBARGO_GROUP_PROPERTY_NAME, userEmbargoEnabledGroups);
            internalWorkflowSession.save();
        } else {
            log.info("Trying to set the embargo on a document for user: {} who is not in any embargo enabled groups.", userId);
        }
    }

    @Override
    public void addEmbargoVariants(final String userId, final String subjectId, final String[] forcedEmbargoGroups) throws WorkflowException, RepositoryException, RemoteException {
        final WorkflowContext workflowContext = getWorkflowContext();
        final Session internalWorkflowSession = workflowContext.getInternalWorkflowSession();
        final Node handle = EmbargoUtils.extractHandle(internalWorkflowSession.getNodeByIdentifier(subjectId));
        // check if embargo handle:
        if (!handle.isNodeType(EmbargoConstants.EMBARGO_MIXIN_NAME)) {
            // not an embargo handle
            log.debug("Not an embargo handle {}", handle.getPath());
            return;
        }
        if (!handle.isCheckedOut()) {
            internalWorkflowSession.getWorkspace().getVersionManager().checkout(handle.getPath());
        }
        String[] userEmbargoEnabledGroups = ArrayUtils.isEmpty(forcedEmbargoGroups) ?
                EmbargoUtils.getCurrentUserEmbargoEnabledGroups(internalWorkflowSession, userId) :
                forcedEmbargoGroups;

        if (userEmbargoEnabledGroups.length > 0) {
            addVariantsMixin(internalWorkflowSession, handle);
            internalWorkflowSession.save();
        } else {
            log.info("Trying to set the embargo on a document for user: {} who is not in any embargo enabled groups.", userId);
        }
    }

    private void addVariantsMixin(final Session internalWorkflowSession, final Node handle) throws RepositoryException {
        for (Node documentNode : EmbargoUtils.getDocumentVariants(handle)) {
            if (documentNode.isNodeType(EmbargoConstants.EMBARGO_DOCUMENT_MIXIN_NAME)) {
                log.debug("Already embargo document: {}", documentNode.getPath());
                continue;
            }

            if (!documentNode.isCheckedOut()) {
                internalWorkflowSession.getWorkspace().getVersionManager().checkout(documentNode.getPath());
            }
            documentNode.addMixin(EmbargoConstants.EMBARGO_DOCUMENT_MIXIN_NAME);
        }
    }

    @Override
    public void removeEmbargo(final String subjectId) throws RepositoryException {
        final WorkflowContext workflowContext = getWorkflowContext();
        final Session internalWorkflowSession = workflowContext.getInternalWorkflowSession();
        final Node handle = EmbargoUtils.extractHandle(internalWorkflowSession.getNodeByIdentifier(subjectId));
        EmbargoUtils.removeEmbargoForHandle(internalWorkflowSession, handle);
        internalWorkflowSession.save();
    }

    @Override
    public void scheduleRemoveEmbargo(String subjectId, final Calendar publicationDate) throws WorkflowException, RepositoryException, RemoteException {
        cancelSchedule(subjectId);
        //wf.removeEmbargo(subjectId);
        final RepositoryScheduler scheduler = HippoServiceRegistry.getService(RepositoryScheduler.class);

        scheduler.scheduleJob(
                new WorkflowJobInfo(subjectId, METHOD_REMOVE_EMBARGO),
                new RepositoryJobSimpleTrigger("embargo", publicationDate.getTime())
        );
    }

    @Override
    public void cancelSchedule(final String subjectId) throws WorkflowException, RepositoryException, RemoteException {
        final WorkflowContext workflowContext = getWorkflowContext();
        final Session internalWorkflowSession = workflowContext.getInternalWorkflowSession();
        final Node handle = EmbargoUtils.extractHandle(internalWorkflowSession.getNodeByIdentifier(subjectId));
        if (!handle.isCheckedOut()) {
            internalWorkflowSession.getWorkspace().getVersionManager().checkout(handle.getPath());
        }

        if (handle.hasNode(EMBARGO_SCHEDULE_REQUEST_NODE_NAME)) {
            handle.getNode(EMBARGO_SCHEDULE_REQUEST_NODE_NAME).remove();
            internalWorkflowSession.save();
        }
    }

    @Override
    public void invokeWorkflow() throws Exception {
        // noop: execute() is called
    }


    public static class WorkflowJob implements RepositoryJob {

        private static final Logger log = LoggerFactory.getLogger(WorkflowJob.class);
        public static final char[] PASSWORD = {};

        @Override
        public void execute(final RepositoryJobExecutionContext context) {
            Session session = null;
            String methodName = null;
            String subjectPath = null;
            try {
                session = context.createSession(new SimpleCredentials("workflowuser", PASSWORD));
                final String subjectId = context.getAttribute(HIPPOSCHED_SUBJECT_ID);
                methodName = context.getAttribute(HIPPOSCHED_METHOD_NAME);
                final Node subject = session.getNodeByIdentifier(subjectId);
                subjectPath = subject.getPath();
                final EmbargoWorkflow workflow = (EmbargoWorkflow) getWorkflowManager(session).getWorkflow("embargo", subject);
                if (METHOD_REMOVE_EMBARGO.equals(methodName)) {
                    workflow.removeEmbargo(subjectId);
                } else {
                    log.warn("Unsupported method called on Embargo workflow: {} ", methodName);
                }
            } catch (ItemNotFoundException e) {
                final String targetId = context.getAttribute(HIPPOSCHED_SUBJECT_ID);
                final String removedAtticItemPath = removeAtticItem(session, targetId);
                if (!Strings.isNullOrEmpty(removedAtticItemPath)) {
                    log.info("Removed embargo request from deleted document (attic): {}", removedAtticItemPath);
                } else {
                    log.error("Node not found for embargo workflow operation {} on {}", new String[]{methodName, subjectPath}, e);
                }
            } catch (RemoteException | WorkflowException | RepositoryException e) {
                log.error("Execution of scheduled workflow operation {} on {} failed", new String[]{methodName, subjectPath}, e);
            } finally {
                if (session != null) {
                    session.logout();
                }
            }
        }

        private String removeAtticItem(final Session session, final String targetId) {
            String removed = null;
            try {
                final QueryManager manager = session.getWorkspace().getQueryManager();
                @SuppressWarnings("deprecation")
                final Query query = manager.createQuery("content/attic//element(*, " + EmbargoConstants.EMBARGO_JOB + ')', Query.XPATH);
                final QueryResult execute = query.execute();
                final NodeIterator nodes = execute.getNodes();
                final boolean needSave = nodes.hasNext();
                while (nodes.hasNext()) {
                    final Node jobNode = nodes.nextNode();
                    final Node handle = jobNode.getParent();
                    if (removed == null && handle.isNodeType(HippoNodeType.NT_HANDLE) && handle.hasNode(EMBARGO_SCHEDULE_REQUEST_NODE_NAME)) {
                        final Node requestNode = handle.getNode(EMBARGO_SCHEDULE_REQUEST_NODE_NAME);
                        final String[] values = JcrUtils.getMultipleStringProperty(requestNode, "hipposched:attributeValues", ArrayUtils.EMPTY_STRING_ARRAY);
                        for (String value : values) {
                            if (targetId.equals(value)) {
                                // store path of removed node
                                final String path = jobNode.getPath();
                                log.info("Removing embargo from node from attic: {}", path);
                                jobNode.remove();
                                removed = handle.getPath();
                                break;
                            }
                        }
                    }

                }
                if (needSave) {
                    session.save();
                }
            } catch (RepositoryException e) {
                log.error("Error removing embargo nodes", e);
            }
            return removed;
        }

        private static WorkflowManager getWorkflowManager(final Session session) throws RepositoryException {
            return ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
        }

    }

    private static class WorkflowJobInfo extends RepositoryJobInfo {

        private final String subjectId;

        public WorkflowJobInfo(final String subjectId, final String methodName) {
            super(EMBARGO_SCHEDULE_REQUEST_NODE_NAME, "embargo", WorkflowJob.class);
            this.subjectId = subjectId;
            setAttribute(HIPPOSCHED_SUBJECT_ID, subjectId);
            setAttribute(HIPPOSCHED_METHOD_NAME, methodName);
        }

        @Override
        public Node createNode(final Session session) throws RepositoryException {
            final Node handle = EmbargoUtils.extractHandle(session.getNodeByIdentifier(subjectId));
            JcrUtils.ensureIsCheckedOut(handle);
            handle.addMixin(EmbargoConstants.EMBARGO_MIXIN_NAME);
            final Node requestNode = handle.addNode(EMBARGO_SCHEDULE_REQUEST_NODE_NAME, EmbargoConstants.EMBARGO_JOB);
            // TODO mm is this one needed?
            requestNode.addMixin("mix:referenceable");
            return requestNode;
        }

    }
}
