/*
package org.hippoecm.repository.reviewedactions;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.Map;

import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.DatastoreIdentity;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowImpl;

@PersistenceCapable(identityType = IdentityType.DATASTORE, cacheable = "true", detachable = "false", table = "documents")
@DatastoreIdentity(strategy = IdGeneratorStrategy.NATIVE)
@Discriminator(strategy = DiscriminatorStrategy.CLASS_NAME)
public class BasicReviewedActionsWorkflowImpl extends WorkflowImpl implements BasicReviewedActionsWorkflow {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    @Persistent(column = "hippostd:holder")
    protected String userIdentity;

    @Persistent(column = "hippostd:state")
    protected String state;

    @Persistent(defaultFetchGroup = "true", column = "../{.}[hippostd:state='draft']")
    protected PublishableDocument draftDocument;

    @Persistent(defaultFetchGroup = "true", column = "../{.}[hippostd:state='unpublished']")
    protected PublishableDocument unpublishedDocument;

    @Persistent(defaultFetchGroup = "true", column = "../{.}[hippostd:state='published']")
    protected PublishableDocument publishedDocument;

    @Persistent(defaultFetchGroup = "true", column = "../hippo:request[hippostdpubwf:type!='rejected']")
    protected PublicationRequest current;

    @Override
    public Map<String, Serializable> hints() {
        Map<String, Serializable> info = super.hints();
        boolean editable;
        boolean publishable = false;
        boolean depublishable = false;
        boolean deleteable = false;
        boolean locked = false;
        boolean status = false;
        boolean pendingRequest;
        if (current != null) {
            pendingRequest = true;
        } else {
            pendingRequest = false;
        }
        if (PublishableDocument.DRAFT.equals(state)) {
            locked = true;
            editable = draftDocument.getOwner() == null || draftDocument.getOwner().equals(super.getWorkflowContext().getUserIdentity());
            depublishable = false;
            publishable = false;
            status = true;
        } else if (PublishableDocument.PUBLISHED.equals(state)) {
            if (draftDocument == null && unpublishedDocument == null) {
                status = true;
            }
            if (draftDocument != null || unpublishedDocument != null) {
                editable = false;
            } else if (pendingRequest) {
                editable = false;
            } else {
                editable = true;
            }
            if (draftDocument == null && !pendingRequest) {
                depublishable = true;
            }
        } else if (PublishableDocument.UNPUBLISHED.equals(state)) {
            if (draftDocument == null) {
                status = true;
            }
            if (draftDocument != null) {
                editable = false;
            } else if (pendingRequest) {
                editable = false;
            } else {
                editable = true;
            }
            if (draftDocument == null && !pendingRequest) {
                publishable = true;
            }
            if (draftDocument == null && publishedDocument == null && !pendingRequest) {
                deleteable = true;
            }
        } else {
            editable = false;
        }
        if (!editable && PublishableDocument.DRAFT.equals(state)) {
            info.put("inUseBy", draftDocument.getOwner());
        }
        info.put("obtainEditableInstance", editable);
        info.put("publish", publishable);
        info.put("depublish", depublishable);
        info.put("delete", deleteable);
        info.put("status", status);
        return info;
    }

    public BasicReviewedActionsWorkflowImpl() throws RemoteException {
    }

    public Document obtainEditableInstance() throws WorkflowException {
        ReviewedActionsWorkflowImpl.log.info("obtain editable instance on document ");
        if (draftDocument == null) {
            if (current != null) {
                throw new WorkflowException("unable to edit document with pending operation");
            }
            try {
                if (unpublishedDocument != null) {
                    draftDocument = (PublishableDocument) unpublishedDocument.clone();
                } else {
                    draftDocument = (PublishableDocument) publishedDocument.clone();
                }
                draftDocument.setState(PublishableDocument.DRAFT);
                draftDocument.setAvailability(new String[0]);
                draftDocument.setOwner(getWorkflowContext().getUserIdentity());
                if (unpublishedDocument != null) {
                    unpublishedDocument.setOwner(getWorkflowContext().getUserIdentity());
                }
                if (publishedDocument != null) {
                    publishedDocument.setOwner(getWorkflowContext().getUserIdentity());
                }
                userIdentity = getWorkflowContext().getUserIdentity();
            } catch (CloneNotSupportedException ex) {
                throw new WorkflowException("document is not a publishable document");
            }
        } else {
            if (draftDocument.getOwner() != null
                    && !getWorkflowContext().getUserIdentity().equals(draftDocument.getOwner()))
                throw new WorkflowException("document already being edited");
        }
        return draftDocument;
    }

    public Document commitEditableInstance() throws WorkflowException {
        ReviewedActionsWorkflowImpl.log.info("commit editable instance of document ");
        if (draftDocument != null) {
            unpublishedDocument = null;
            draftDocument.setState(PublishableDocument.UNPUBLISHED);
            draftDocument.setAvailability(new String[]{"preview"});
            draftDocument.setModified(getWorkflowContext().getUserIdentity());
            if (publishedDocument != null) {
                publishedDocument.setAvailability(new String[]{"live"});
            }
            return draftDocument;
        } else {
            throw new WorkflowException("no draft version of publication");
        }
    }

    public Document disposeEditableInstance() throws WorkflowException {
        ReviewedActionsWorkflowImpl.log.info("dispose editable instance on document ");
        draftDocument = null;
        if (unpublishedDocument != null) {
            return unpublishedDocument;
        } else if (publishedDocument != null) {
            return publishedDocument;
        } else {
            return null;
        }
    }

    public void requestDeletion() throws WorkflowException {
        ReviewedActionsWorkflowImpl.log.info("deletion request on document ");
        if (current == null) {
            current = new PublicationRequest(PublicationRequest.DELETE, unpublishedDocument, getWorkflowContext()
                    .getUserIdentity());
        } else {
            throw new WorkflowException("request deletion failure");
        }
    }

    public void requestPublication() throws WorkflowException {
        ReviewedActionsWorkflowImpl.log.info("publication request on document ");
        if (current == null) {
            current = new PublicationRequest(PublicationRequest.PUBLISH, unpublishedDocument, getWorkflowContext()
                    .getUserIdentity());
        } else {
            throw new WorkflowException("publication request already pending");
        }
    }

    public void requestDepublication() throws WorkflowException {
        ReviewedActionsWorkflowImpl.log.info("depublication request on document ");
        if (current == null) {
            current = new PublicationRequest(PublicationRequest.DEPUBLISH, publishedDocument, getWorkflowContext()
                    .getUserIdentity());
        } else {
            throw new WorkflowException("publication request already pending");
        }
    }

    public void requestPublication(Date publicationDate) throws WorkflowException {
        ReviewedActionsWorkflowImpl.log.info("publication request on document ");
        if (current == null) {
            current = new PublicationRequest(PublicationRequest.SCHEDPUBLISH, unpublishedDocument, getWorkflowContext()
                    .getUserIdentity(), publicationDate);
        } else {
            throw new WorkflowException("publication request already pending");
        }
    }

    public void requestPublication(Date publicationDate, Date depublicationDate) throws WorkflowException {
        ReviewedActionsWorkflowImpl.log.info("publication request on document ");
        throw new WorkflowException("unsupported");
    }

    public void requestDepublication(Date depublicationDate) throws WorkflowException {
        ReviewedActionsWorkflowImpl.log.info("depublication request on document ");
        if (current == null) {
            current = new PublicationRequest(PublicationRequest.SCHEDDEPUBLISH, publishedDocument, getWorkflowContext()
                    .getUserIdentity(), depublicationDate);
        } else {
            throw new WorkflowException("publication request already pending");
        }
    }
}
*/
