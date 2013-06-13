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
package org.onehippo.forge.embargo.frontend.plugins;

import java.util.Calendar;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.onehippo.forge.embargo.repository.EmbargoConstants;
import org.onehippo.forge.embargo.repository.EmbargoUtils;
import org.onehippo.forge.embargo.repository.workflow.EmbargoWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
//TODO: CompatibilityWorkflowPlugin is deprecated, please change (when the API supports it)
public class EmbargoWorkflowPlugin extends CompatibilityWorkflowPlugin<EmbargoWorkflow> {

    private static Logger log = LoggerFactory.getLogger(EmbargoWorkflowPlugin.class);
    private static final long serialVersionUID = 1L;

    public EmbargoWorkflowPlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    protected void onModelChanged() {
        super.onModelChanged();
        try {
            WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel) getDefaultModel();
            WorkflowDescriptor workflowDescriptor = (WorkflowDescriptor) getDefaultModelObject();
            if (workflowDescriptor != null) {
                //TODO: Change this with a non deprecated call (when the API supports it)
                Node documentNode = workflowDescriptorModel.getNode();
                if (EmbargoUtils.isVisibleInPreview(documentNode)) {
                    final Mode mode = resolveMode(documentNode.getParent());
                    createMenu(mode);
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage(), ex);
        }
    }


    /**
     * Creates the proper menu items based on the mode the document is in
     *
     * @param mode
     */
    private void createMenu(final Mode mode) {

        if (Mode.UNEMBARGOED.equals(mode)) {
            add(new StdWorkflow<EmbargoWorkflow>("set", new StringResourceModel("set-embargo-label", this, null), getPluginContext(), this) {
                private static final long serialVersionUID = 1L;

                @Override
                protected ResourceReference getIcon() {
                    return new ResourceReference(getClass(), "lock_add.png");
                }

                @Override
                protected String execute(EmbargoWorkflow workflow) throws Exception {
                    workflow.addEmbargo();
                    return null;
                }
            });
        }

        if (Mode.EMBARGOED.equals(mode)) {
            add(new StdWorkflow<EmbargoWorkflow>("remove", new StringResourceModel("remove-embargo-label", this, null), getPluginContext(), this) {
                private static final long serialVersionUID = 1L;

                @Override
                protected ResourceReference getIcon() {
                    return new ResourceReference(getClass(), "lock_break.png");
                }

                @Override
                protected String execute(EmbargoWorkflow workflow) throws Exception {
                    workflow.removeEmbargo();
                    return null;
                }
            });

            add(new WorkflowAction("scheduleUnembargo", new StringResourceModel("schedule-unembargo-label", this, null).getString(), null) {
                private static final long serialVersionUID = 1L;
                public Date date = new Date();

                @Override
                protected ResourceReference getIcon() {
                    return new ResourceReference(getClass(), "clock_delete.png");
                }

                @Override
                protected IDialogService.Dialog createRequestDialog() {
                    return new ScheduleDialog(this, new PropertyModel(this, "date"), "schedule-removal-embargo-title", "schedule-removal-embargo-text");
                }

                @Override
                protected String execute(final EmbargoWorkflow embargoWorkflow) throws Exception {
                    final Calendar embargoDate = Calendar.getInstance();
                    embargoDate.setTime(date);
                    if (date != null) {
                        embargoWorkflow.scheduleRemoveEmbargo(embargoDate);
                    }
                    return null;
                }
            });
        }


        if (Mode.SCHEDULED_UNEMBARGO.equals(mode)) {
            add(new WorkflowAction("rescheduledUnembargo", new StringResourceModel("reschedule-unembargo-label", this, null).getString(), null) {
                private static final long serialVersionUID = 1L;
                public Date date = new Date();

                @Override
                protected ResourceReference getIcon() {
                    return new ResourceReference(getClass(), "clock_delete.png");
                }

                @Override
                protected IDialogService.Dialog createRequestDialog() {

                    //Set the date to that of the existing embargo:request
                    WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel) getDefaultModel();
                    WorkflowDescriptor workflowDescriptor = (WorkflowDescriptor) getDefaultModelObject();
                    if (workflowDescriptor != null) {
                        try {
                            //TODO: Change this with a non deprecated call (when the API supports it)
                            Node handleNode = workflowDescriptorModel.getNode().getParent();
                            Calendar existingExpirationDate = EmbargoUtils.getEmbargoExpirationDate(handleNode);
                            if (existingExpirationDate != null) {
                                date = existingExpirationDate.getTime();
                            }
                        } catch (RepositoryException e) {
                            log.error("Error while retrieving embargo schedule", e);
                        }
                    }
                    return new ScheduleDialog(this, new PropertyModel(this, "date"), "reschedule-removal-embargo-title", "reschedule-removal-embargo-text");
                }

                @Override
                protected String execute(EmbargoWorkflow embargoWorkflow) throws Exception {
                    final Calendar embargoDate = Calendar.getInstance();
                    embargoDate.setTime(date);
                    if (date != null) {
                        embargoWorkflow.scheduleRemoveEmbargo(embargoDate);
                    }
                    return null;
                }
            });


            add(new StdWorkflow<EmbargoWorkflow>("cancelScheduledUnembargo", new StringResourceModel("cancel-scheduled-unembargo-label", this, null), getPluginContext(), this) {
                private static final long serialVersionUID = 1L;

                @Override
                protected ResourceReference getIcon() {
                    return new ResourceReference(getClass(), "cancel_schedule.png");
                }

                @Override
                protected String execute(EmbargoWorkflow workflow) throws Exception {
                    workflow.cancelSchedule();
                    return null;
                }
            });
        }
    }


    /**
     * Resolve the mode the handle is currenlty in for the menu
     *
     * @param handleNode
     * @return
     * @throws RepositoryException
     */
    private Mode resolveMode(Node handleNode) throws RepositoryException {
        return handleNode.hasNode(EmbargoConstants.EMBARGO_SCHEDULE_REQUEST_NODE_NAME) ?
                Mode.SCHEDULED_UNEMBARGO :
                handleNode.isNodeType(EmbargoConstants.EMBARGO_MIXIN_NAME) ?
                        Mode.EMBARGOED :
                        Mode.UNEMBARGOED;
    }

    private enum Mode {
        UNEMBARGOED,
        EMBARGOED,
        SCHEDULED_UNEMBARGO
    }

}