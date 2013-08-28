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

import java.util.*;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
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
     * @param mode which {@link Mode} to create the menu for
     */
    private void createMenu(final Mode mode) {
        switch (mode) {
            case UNEMBARGOED:
                addSetEmbargoOption();
                break;
            case EMBARGOED:
                addRemoveEmbargoOption();
                addScheduleUnembargoOption();
                break;
            case SCHEDULED_UNEMBARGO:
                addRescheduleUnembargoOption();
                addCancelScheduledUnembargoOption();
                break;
        }
    }

    private void addCancelScheduledUnembargoOption() {
        StringResourceModel nameModel = new StringResourceModel("cancel-scheduled-unembargo-label", this, null);
        add(new StdWorkflow<EmbargoWorkflow>("cancelScheduledUnembargo", nameModel, getPluginContext(), this) {
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

    private void addRescheduleUnembargoOption() {
        final String name = new StringResourceModel("reschedule-unembargo-label", this, null).getString();
        add(new WorkflowAction("rescheduledUnembargo", name, null) {
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
                return new ScheduleDialog(this, new PropertyModel<Date>(this, "date"),
                        "reschedule-removal-embargo-title", "reschedule-removal-embargo-text");
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
    }

    private void addScheduleUnembargoOption() {
        final String name = new StringResourceModel("schedule-unembargo-label", this, null).getString();
        add(new WorkflowAction("scheduleUnembargo", name, null) {
            private static final long serialVersionUID = 1L;
            public Date date = new Date();

            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "clock_delete.png");
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                return new ScheduleDialog(this, new PropertyModel<Date>(this, "date"),
                        "schedule-removal-embargo-title", "schedule-removal-embargo-text");
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

    private void addRemoveEmbargoOption() {
        StringResourceModel nameModel = new StringResourceModel("remove-embargo-label", this, null);
        add(new StdWorkflow<EmbargoWorkflow>("remove", nameModel, getPluginContext(), this) {
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
    }

    private void addSetEmbargoOption() {
        String name = new StringResourceModel("set-embargo-label", this, null).getString();

        if(EmbargoUtils.isAdminUser(getJcrSession(), getJcrSession().getUserID())){

            add(new WorkflowAction("set", name, null) {
                private static final long serialVersionUID = 1L;
                final public ArrayList<String> selectedEmbargoGroups = new ArrayList<String>();
                final IModel selectedEmbargoGroupsModel = new Model<ArrayList<String>>(selectedEmbargoGroups);

                @Override
                protected ResourceReference getIcon() {
                    return new ResourceReference(getClass(), "lock_add.png");
                }

                @Override
                protected IDialogService.Dialog createRequestDialog() {
                    return new SetEmbargoDialog(
                            this,
                            selectedEmbargoGroupsModel,
                            EmbargoUtils.getAllEmbargoEnabledGroups(getJcrSession()),
                            "select-embargo-groups-title",
                            "select-embargo-groups-text");
                }

                @Override
                protected String execute(EmbargoWorkflow workflow) throws Exception {
                    final String userID = getJcrSession().getUserID();
                    if(selectedEmbargoGroups.size() != 0){
                        workflow.addEmbargo(userID, selectedEmbargoGroups.toArray(new String[selectedEmbargoGroups.size()]));
                    }
                    return null;
                }
            });

        } else {
            add(new StdWorkflow<EmbargoWorkflow>("set", name, getPluginContext(), this) {
                private static final long serialVersionUID = 1L;

                @Override
                protected ResourceReference getIcon() {
                    return new ResourceReference(getClass(), "lock_add.png");
                }

                @Override
                protected String execute(EmbargoWorkflow workflow) throws Exception {
                    final String userID = getJcrSession().getUserID();
                    workflow.addEmbargo(userID, null);
                    return null;
                }
            });
        }
    }

    private javax.jcr.Session getJcrSession() {
        return ((UserSession) Session.get()).getJcrSession();
    }

    /**
     * Resolve the mode the handle is currently in for the menu
     *
     * @param handleNode the handle of the document
     * @return the {@link Mode} this document is in, embargo wise
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
