/*
 * Copyright 2013-2014 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.AbstractWorkflowDialog;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.onehippo.forge.embargo.repository.workflow.EmbargoWorkflow;

import java.util.List;

public class SetEmbargoDialog extends AbstractWorkflowDialog {

    private static final long serialVersionUID = 1L;

    final String title;

    public SetEmbargoDialog(StdWorkflow<EmbargoWorkflow> action, IModel<List<String>> selectedEmbargoGroups, List<String> availableEmbargoGroups, final String title, final String text) {
        super(selectedEmbargoGroups, action);
        add(new CheckBoxMultipleChoice<>("checkboxes", selectedEmbargoGroups, availableEmbargoGroups));
        add(new Label("text", new ResourceModel(text)));
        this.title = title;
    }

    @Override
    public IModel<String> getTitle() {
        return new StringResourceModel(this.title, this, null);
    }

    @Override
    public IValueMap getProperties() {
        return DialogConstants.SMALL;
    }
}
