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

import javax.jcr.RepositoryException;

import org.hippoecm.addon.workflow.IWorkflowInvoker;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;

/**
 * @version $Id$
 */
public interface EmbargoWorkflow extends Workflow, IWorkflowInvoker {

    /**
     * Adds the embargo to the current document. The user ID used to fetch the group of the user and mark the document
     * so that it will only be available for users in the same group.
     *
     * @param userId the ID of the user.
     * @throws WorkflowException
     * @throws RepositoryException
     * @throws MappingException
     * @throws RemoteException
     */
    void addEmbargo(String userId, String subjectId, String[] forcedEmbargoGroups) throws WorkflowException, RepositoryException, RemoteException;

    void addEmbargoHandle(String userId, String subjectId, String[] forcedEmbargoGroups) throws WorkflowException, RepositoryException, RemoteException;

    void addEmbargoVariants(String userId, String subjectId, String[] forcedEmbargoGroups) throws WorkflowException, RepositoryException, RemoteException;

    void removeEmbargo(String subjectId) throws WorkflowException, RepositoryException, RemoteException;

    void scheduleRemoveEmbargo(String subjectId, Calendar publicationDate) throws WorkflowException, RepositoryException, RemoteException;

    void cancelSchedule(String subjectId) throws WorkflowException, RepositoryException, RemoteException;


}
