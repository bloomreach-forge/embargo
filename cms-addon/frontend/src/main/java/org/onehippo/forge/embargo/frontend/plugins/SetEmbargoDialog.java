package org.onehippo.forge.embargo.frontend.plugins;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;

import java.util.List;

public class SetEmbargoDialog extends CompatibilityWorkflowPlugin.WorkflowAction.WorkflowDialog {

    private static final long serialVersionUID = 1L;

    final String title;

    public SetEmbargoDialog(CompatibilityWorkflowPlugin.WorkflowAction action, IModel<List<String>> selectedEmbargoGroups, List<String> availableEmbargoGroups, final String title, final String text) {
        action.super();
        add(new CheckBoxMultipleChoice<String>("checkboxes", selectedEmbargoGroups, availableEmbargoGroups));
        add(new Label("text", new ResourceModel(text)));
        this.title = title;
    }

    @Override
    public IModel getTitle() {
        return new StringResourceModel(this.title, this, null);
    }

    @Override
    public IValueMap getProperties() {
        return SMALL;
    }
}
