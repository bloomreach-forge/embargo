package org.onehippo.forge.embargo.repository.workflow;

import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;

/**
 * @version $Id$
 */
public interface EmbargoWorkflow2 extends Workflow{

    public void addEmbargo() throws WorkflowException, RepositoryException, MappingException, RemoteException;

    public void addEmbargo(Calendar publicationDate) throws WorkflowException, RepositoryException, MappingException, RemoteException;

    public void removeEmbargo() throws WorkflowException, RepositoryException, MappingException, RemoteException;

    public void removeEmbargo(Calendar publicationDate) throws WorkflowException, RepositoryException, MappingException, RemoteException;


}
