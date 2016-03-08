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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.render.RenderPlugin;
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
public class EmbargoWorkflowPlugin extends RenderPlugin<WorkflowDescriptor> { //CompatibilityWorkflowPlugin<EmbargoWorkflow> {
//public class EmbargoWorkflowPlugin extends CompatibilityWorkflowPlugin<EmbargoWorkflow> {

    private static Logger log = LoggerFactory.getLogger(EmbargoWorkflowPlugin.class);
    private static final long serialVersionUID = 1L;

    public EmbargoWorkflowPlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    protected void onModelChanged() {
        super.onModelChanged();
        try {
            WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel)getDefaultModel();
            WorkflowDescriptor workflowDescriptor = (WorkflowDescriptor)getDefaultModelObject();
            if (workflowDescriptor != null) {
                Node documentNode = workflowDescriptorModel.getNode();
                // TODO CHeck whether is visible check is required
                //if (EmbargoUtils.isVisibleInPreview(documentNode)) {
                    final Mode mode = resolveMode(documentNode);
                    createMenu(mode);
                //}
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        modelChanged();
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
        add(new StdWorkflow<EmbargoWorkflow>("cancelScheduledUnembargo", nameModel, new PackageResourceReference(getClass(), "cancel_schedule.png"), getPluginContext(), (WorkflowDescriptorModel) getDefaultModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String execute(EmbargoWorkflow workflow) throws Exception {
                workflow.cancelSchedule(getSubjectId());
                return null;
            }
        });
    }

    private void addRescheduleUnembargoOption() {
        StringResourceModel nameModel = new StringResourceModel("reschedule-unembargo-label", this, null);
        add(new StdWorkflow<EmbargoWorkflow>("rescheduledUnembargo", nameModel, new PackageResourceReference(getClass(), "clock_delete.png"), getPluginContext(), (WorkflowDescriptorModel) getDefaultModel()) {
            private static final long serialVersionUID = 1L;
            public Date date = new Date();

            @Override
            protected IDialogService.Dialog createRequestDialog() {

                //Set the date to that of the existing embargo:request
                WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel)getDefaultModel();
                final Node node;
                try {
                    node = workflowDescriptorModel.getNode();
                } catch (RepositoryException e) {
                    log.error("Error while retrieving embargo schedule", e);
                    return null;
                }

                WorkflowDescriptor workflowDescriptor = (WorkflowDescriptor)getDefaultModelObject();
                if (workflowDescriptor != null) {
                    try {
                        final Node handleNode = node;
                        Calendar existingExpirationDate = EmbargoUtils.getEmbargoExpirationDate(handleNode);
                        if (existingExpirationDate != null) {
                            date = existingExpirationDate.getTime();
                        }
                    } catch (RepositoryException e) {
                        log.error("Error while retrieving embargo schedule", e);
                    }

                    return new ScheduleDialog(this, new JcrNodeModel(node),
                            new PropertyModel<Date>(this, "date"),getEditorManager(), "reschedule-removal-embargo-title", "reschedule-removal-embargo-text");
                }

                return null;
            }

            @Override
            protected String execute(EmbargoWorkflow embargoWorkflow) throws Exception {
                final Calendar embargoDate = Calendar.getInstance();
                embargoDate.setTime(date);
                if (date != null) {

                    final String subjectId = getSubjectId();
                    embargoWorkflow.scheduleRemoveEmbargo(subjectId, embargoDate);
                }
                return null;
            }


        });
    }

    private String getSubjectId() throws RepositoryException {
        final WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel)getDefaultModel();
        final Node documentNode = workflowDescriptorModel.getNode();
        return documentNode.getIdentifier();
    }

    private void addScheduleUnembargoOption() {
        final String name = new StringResourceModel("schedule-unembargo-label", this, null).getString();
        add(new StdWorkflow<EmbargoWorkflow>("scheduleUnembargo", Model.of(name), new PackageResourceReference(getClass(), "clock_delete.png"), getPluginContext(), (WorkflowDescriptorModel) getDefaultModel()) {
            private static final long serialVersionUID = 1L;
            public Date date = new Date();

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                final WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel)getDefaultModel();
                try {
                    return new ScheduleDialog(this, new JcrNodeModel(workflowDescriptorModel.getNode()),
                            new PropertyModel<Date>(this, "date"), getEditorManager(), "reschedule-removal-embargo-title", "reschedule-removal-embargo-text");
                } catch (RepositoryException e) {
                    log.error("Error crating ScheduleDialog", e);
                }
                return null;
            }

            @Override
            protected void execute() throws Exception {
                super.execute();
            }

            @Override
            protected void execute(final WorkflowDescriptorModel model) throws Exception {
                super.execute(model);
            }

            @Override
            protected String execute(final EmbargoWorkflow embargoWorkflow) throws Exception {
                final Calendar embargoDate = Calendar.getInstance();
                embargoDate.setTime(date);
                if (date != null) {
                    embargoWorkflow.scheduleRemoveEmbargo(getSubjectId(), embargoDate);
                }
                return null;
            }


        });
    }

    private void addRemoveEmbargoOption() {
        StringResourceModel nameModel = new StringResourceModel("remove-embargo-label", this, null);
        add(new StdWorkflow<EmbargoWorkflow>("remove", nameModel, new PackageResourceReference(getClass(), "lock_break.png"), getPluginContext(), (WorkflowDescriptorModel) getDefaultModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String execute(EmbargoWorkflow workflow) throws Exception {
                workflow.removeEmbargo(getSubjectId());
                return null;
            }
        });
    }

    private void addSetEmbargoOption() {
        StringResourceModel nameModel = new StringResourceModel("set-embargo-label", this, null);

        if (EmbargoUtils.isAdminUser(getJcrSession(), getJcrSession().getUserID())) {

            add(new StdWorkflow<EmbargoWorkflow>("set", nameModel, new PackageResourceReference(getClass(), "lock_add.png"), null) {
                private static final long serialVersionUID = 1L;
                final ArrayList<String> selectedEmbargoGroups = new ArrayList<>();
                final IModel selectedEmbargoGroupsModel = new Model<>(selectedEmbargoGroups);

                @SuppressWarnings("unchecked")
                @Override
                protected IDialogService.Dialog createRequestDialog() {
                    return  new SetEmbargoDialog(
                            this,
                            selectedEmbargoGroupsModel,
                            EmbargoUtils.getAllEmbargoEnabledGroups(getJcrSession()),
                            "select-embargo-groups-title",
                            "select-embargo-groups-text");
                }

                @Override
                protected String execute(EmbargoWorkflow workflow) throws Exception {
                    final String userID = getJcrSession().getUserID();
                    if (selectedEmbargoGroups.size() != 0) {
                        final WorkflowDescriptorModel defaultModel = (WorkflowDescriptorModel)getDefaultModel();
                        final String subjectId = defaultModel.getNode().getIdentifier();
                        workflow.addEmbargo(userID, subjectId, selectedEmbargoGroups.toArray(new String[selectedEmbargoGroups.size()]));
                    }
                    return null;
                }
            });

        } else {
            add(new StdWorkflow<EmbargoWorkflow>("set", nameModel, new PackageResourceReference(getClass(), "lock_add.png"), getPluginContext(), (WorkflowDescriptorModel) getDefaultModel()) {
                private static final long serialVersionUID = 1L;

                @Override
                protected String execute(EmbargoWorkflow workflow) throws Exception {
                    final String userID = getJcrSession().getUserID();
                    workflow.addEmbargo(userID, getSubjectId(), null);
                    return null;
                }
            });
        }
    }

    private javax.jcr.Session getJcrSession() {
        return ((UserSession)Session.get()).getJcrSession();
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

    protected IEditorManager getEditorManager() {
        return getPluginContext().getService(getPluginConfig().getString("editor.id"), IEditorManager.class);
    }

    private enum Mode {
        UNEMBARGOED,
        EMBARGOED,
        SCHEDULED_UNEMBARGO
    }

}
