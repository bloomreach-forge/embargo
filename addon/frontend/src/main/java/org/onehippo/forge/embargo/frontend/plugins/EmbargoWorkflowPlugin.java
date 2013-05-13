package org.onehippo.forge.embargo.frontend.plugins;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.Session;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.onehippo.forge.embargo.repository.workflow.EmbargoWorkflow2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
public class EmbargoWorkflowPlugin extends CompatibilityWorkflowPlugin<EmbargoWorkflow2> {

    private static Logger log = LoggerFactory.getLogger(EmbargoWorkflowPlugin.class);

    public EmbargoWorkflowPlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    protected void onModelChanged() {
        super.onModelChanged();
        try {
            WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
            WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel) getDefaultModel();
            WorkflowDescriptor workflowDescriptor = (WorkflowDescriptor) getDefaultModelObject();
            if (workflowDescriptor != null) {
                Node documentNode = workflowDescriptorModel.getNode();
                Workflow workflow = manager.getWorkflow(workflowDescriptor);
                Map<String, Serializable> info = workflow.hints();

                final Mode mode = resolveMode(documentNode.getParent());
                //only create single menu
                if (isVisibleInPreview(documentNode)) {
                    createMenu(mode);
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage(), ex);
        } catch (WorkflowException ex) {
            log.error(ex.getMessage(), ex);
        } catch (RemoteException ex) {
            log.error(ex.getMessage(), ex);
        }
    }


    /**
     *TODO Adding all menu items for test purposes. Menu items should be visible according to the MODE
     *
     * @param mode
     */
    private void createMenu(final Mode mode) {

        if(Mode.UNEMBARGOED.equals(mode)){
            add(new StdWorkflow<EmbargoWorkflow2>("set", new StringResourceModel("set-embargo-label", this, null), getPluginContext(), this) {
                @Override
                protected ResourceReference getIcon() {
                    return new ResourceReference(getClass(), "lock_add.png");
                }

                @Override
                protected String execute(EmbargoWorkflow2 workflow) throws Exception {
                    WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel) getDefaultModel();
                    WorkflowDescriptor workflowDescriptor = (WorkflowDescriptor) getDefaultModelObject();
                    if (workflowDescriptor != null) {
                        Node documentNode = workflowDescriptorModel.getNode();
                        Node handleNode = documentNode.getParent();
                        workflow.addEmbargo();
                        // workflow.setEmbargo(handleNode, true);
                    }
                    return null;
                }
            });
        }

        if(Mode.EMBARGOED.equals(mode)){
            add(new StdWorkflow<EmbargoWorkflow2>("remove", new StringResourceModel("remove-embargo-label", this, null), getPluginContext(), this) {
                @Override
                protected ResourceReference getIcon() {
                    return new ResourceReference(getClass(), "lock_break.png");
                }

                @Override
                protected String execute(EmbargoWorkflow2 workflow) throws Exception {
                    WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel) getDefaultModel();
                    WorkflowDescriptor workflowDescriptor = (WorkflowDescriptor) getDefaultModelObject();
                    if (workflowDescriptor != null) {
                        Node documentNode = workflowDescriptorModel.getNode();
                        Node handleNode = documentNode.getParent();
                        workflow.removeEmbargo();
                        //workflow.setEmbargo(handleNode, false);
                    }
                    return null;
                }
            });

            add(new WorkflowAction("scheduleUnembargo", new StringResourceModel("schedule-unembargo-label", this, null).getString(), null) {
                public Date date = new Date();

                @Override
                protected ResourceReference getIcon() {
                    return new ResourceReference(getClass(), "clock_delete.png");
                }

                @Override
                protected IDialogService.Dialog createRequestDialog() {
                    return new ScheduleDialog(this, new PropertyModel(this, "date"), "unschedule-embargo-title", "unschedule-embargo-text");
                }

                @Override
                protected String execute(final EmbargoWorkflow2 wf) throws Exception {
                    //return super.execute(workflow);
                    // EmbargoWorkflow2 workflow = (EmbargoWorkflow2) wf;
                    final Calendar embargoDate = Calendar.getInstance();
                    embargoDate.setTime(date);
                    if (date != null) {
                        wf.removeEmbargo(embargoDate);
                    } else {
                        wf.removeEmbargo();
                    }
                    return null;
                }
            });
        }


        if(Mode.SCHEDULED_UNEMBARGO.equals(mode)){
            add(new StdWorkflow<EmbargoWorkflow2>("cancelScheduledUnembargo", new StringResourceModel("cancel-scheduled-unembargo-label", this, null), getPluginContext(), this) {
                @Override
                protected ResourceReference getIcon() {
                    return new ResourceReference(getClass(), "cancel_schedule.png");
                }

                @Override
                protected String execute(EmbargoWorkflow2 workflow) throws Exception {
                    return null;
                }
            });
        }

        /*add(new WorkflowAction("scheduleEmbargo", new StringResourceModel(
                "schedule-embargo-label", this, null).getString(), null) {
            public Date date = new Date();

            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "clock_add.png");
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                return new ScheduleDialog(this, new PropertyModel(this, "date"), "schedule-embargo-title", "schedule-embargo-text");
            }

            @Override
            protected String execute(final EmbargoWorkflow2 wf) throws Exception {
                //return super.execute(workflow);
                // EmbargoWorkflow2 workflow = (EmbargoWorkflow2) wf;
                final Calendar embargoDate = Calendar.getInstance();
                embargoDate.setTime(date);
                if (date != null) {
                    wf.addEmbargo(embargoDate);
                } else {
                    wf.addEmbargo();
                }
                return null;
            }


        })*/;


    }

    /**
     * TODO add support for images and assets insetad of publishable documents
     *
     * @param documentNode
     * @return
     */
    private static boolean isVisibleInPreview(Node documentNode) {
        try {
            if (documentNode.hasProperty(HippoNodeType.HIPPO_AVAILABILITY)) {
                for (Value availability : documentNode.getProperty(HippoNodeType.HIPPO_AVAILABILITY).getValues()) {
                    if ("preview".equals(availability.getString())) {
                        return true;
                    }
                }
            } else {
                log.warn("Document '{}' does not contain the property '{}'. No 'View' menu items will be shown.",
                        documentNode.getPath(), HippoNodeType.HIPPO_AVAILABILITY);
            }
        } catch (RepositoryException e) {
            log.error("Error getting " + HippoNodeType.HIPPO_AVAILABILITY + " property from document", e);
        }
        return false;
    }

    /**
     * Resolve the mode the handle is currenlty in for the menu
     *
     * @param handleNode
     * @return
     * @throws RepositoryException
     */
    private Mode resolveMode(Node handleNode) throws RepositoryException {
        return handleNode.hasNode("embargo:request") ?
                Mode.SCHEDULED_UNEMBARGO:
                handleNode.isNodeType("embargo:embargo") ?
                        Mode.EMBARGOED :
                        Mode.UNEMBARGOED;

        //todo resolve scheduled-set mode of embargo
    }

    private enum Mode {
        UNEMBARGOED,
        EMBARGOED,
        SCHEDULED_EMBARGO,
        SCHEDULED_UNEMBARGO
    }

}
