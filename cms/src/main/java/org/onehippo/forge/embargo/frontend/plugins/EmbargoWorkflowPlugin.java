/*
 * Copyright 2013-2022 Bloomreach B.V. (http://www.bloomreach.com)
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

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.onehippo.forge.embargo.repository.EmbargoConstants;
import org.onehippo.forge.embargo.repository.EmbargoUtils;
import org.onehippo.forge.embargo.repository.workflow.EmbargoWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plugin for the menu
 */
public class EmbargoWorkflowPlugin extends RenderPlugin<WorkflowDescriptor> {

    private static Logger log = LoggerFactory.getLogger(EmbargoWorkflowPlugin.class);

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
                final Node node = workflowDescriptorModel.getNode();
                final Node handle = EmbargoUtils.extractHandle(node);
                final String name = handle.getName();
                final String handlePath = handle.getPath();
                final String path = handlePath + '/' + name;
                final Mode mode = resolveMode(handle);
                if (node.getPath().equals(handlePath) || node.getPath().equals(path)) {
                    createMenu(mode);
                }
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
        add(new StdWorkflow<EmbargoWorkflow>("cancelScheduledUnembargo", nameModel, getPluginContext(), (WorkflowDescriptorModel)getDefaultModel()) {

            @Override
            protected String execute(EmbargoWorkflow workflow) throws Exception {
                workflow.cancelSchedule(getSubjectId());
                return null;
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.MINUS_CIRCLE_CLOCK);
            }
        });
    }

    private void addRescheduleUnembargoOption() {
        StringResourceModel nameModel = new StringResourceModel("reschedule-unembargo-label", this, null);
        add(new StdWorkflow<EmbargoWorkflow>("rescheduledUnembargo", nameModel, getPluginContext(), (WorkflowDescriptorModel)getDefaultModel()) {
            private Date date = new Date();

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
                        Calendar existingExpirationDate = EmbargoUtils.getEmbargoExpirationDate(EmbargoUtils.extractHandle(node));
                        if (existingExpirationDate != null) {
                            date = existingExpirationDate.getTime();
                        }
                    } catch (RepositoryException e) {
                        log.error("Error while retrieving embargo schedule", e);
                    }

                    return new ScheduleDialog(this, new PropertyModel<>(this, "date"),"reschedule-removal-embargo-title");
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

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.CHECK_CIRCLE_CLOCK);
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
        add(new StdWorkflow<EmbargoWorkflow>("scheduleUnembargo", Model.of(name), getPluginContext(), (WorkflowDescriptorModel)getDefaultModel()) {
            public Date date = new Date();

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                return new ScheduleDialog(this, new PropertyModel<>(this, "date"),"schedule-removal-embargo-title");
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

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.MINUS_CIRCLE_CLOCK);
            }

        });
    }

    private void addRemoveEmbargoOption() {
        StringResourceModel nameModel = new StringResourceModel("remove-embargo-label", this, null);
        add(new StdWorkflow<EmbargoWorkflow>("remove", nameModel, getPluginContext(), (WorkflowDescriptorModel)getDefaultModel()) {

            @Override
            protected String execute(EmbargoWorkflow workflow) throws Exception {
                workflow.removeEmbargo(getSubjectId());
                return null;
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.UNLOCKED);
            }
        });
    }

    private void addSetEmbargoOption() {
        final IModel<String> nameModel = new StringResourceModel("set-embargo-label", this, null);

        final boolean isAdminUser = EmbargoUtils.isAdminUser(getJcrSession(), getJcrSession().getUserID());
        final boolean multiSelectEnabled = getPluginConfig().getAsBoolean("multiSelectEnabled", false);
        final String[] currentUserEmbargoEnabledGroups = EmbargoUtils.getCurrentUserEmbargoEnabledGroups(getJcrSession(), getJcrSession().getUserID());

        if ( isAdminUser || (multiSelectEnabled && currentUserEmbargoEnabledGroups.length > 1)) {

            add(new StdWorkflow<EmbargoWorkflow>("set", nameModel, getPluginContext(), (WorkflowDescriptorModel)getDefaultModel()) {
                final ArrayList<String> selectedEmbargoGroups = new ArrayList<>();
                final IModel selectedEmbargoGroupsModel = new Model<>(selectedEmbargoGroups);

                @SuppressWarnings("unchecked")
                @Override
                protected IDialogService.Dialog createRequestDialog() {
                    return new SetEmbargoDialog(
                            this,
                            selectedEmbargoGroupsModel,
                            isAdminUser? EmbargoUtils.getAllEmbargoEnabledGroups(getJcrSession()) : List.of(currentUserEmbargoEnabledGroups));
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

                @Override
                protected Component getIcon(final String id) {
                    return HippoIcon.fromSprite(id, Icon.LOCKED);
                }
            });

        } else {
            add(new StdWorkflow<EmbargoWorkflow>("set", nameModel, getPluginContext(), (WorkflowDescriptorModel)getDefaultModel()) {

                @Override
                protected String execute(EmbargoWorkflow workflow) throws Exception {
                    final String userID = getJcrSession().getUserID();
                    workflow.addEmbargo(userID, getSubjectId(), null);
                    return null;
                }

                @Override
                protected Component getIcon(final String id) {
                    return HippoIcon.fromSprite(id, Icon.LOCKED);
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
