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
package org.onehippo.forge.embargo.frontend.plugins;

import java.util.List;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.onehippo.forge.embargo.repository.workflow.EmbargoWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetEmbargoDialog extends Dialog<List<String>> {

    private static final Logger log = LoggerFactory.getLogger(SetEmbargoDialog.class);

    private static final CssResourceReference CSS = new CssResourceReference(SetEmbargoDialog.class, "SetEmbargoDialog.css");

    private StdWorkflow<EmbargoWorkflow> action;

    public SetEmbargoDialog(StdWorkflow<EmbargoWorkflow> action, IModel<List<String>> selectedEmbargoGroups, List<String> availableEmbargoGroups) {
        super(selectedEmbargoGroups);
        add(new Label("text", new ResourceModel("select-embargo-groups-text")));
        final CheckBoxMultipleChoice<String> embargoGroups = new CheckBoxMultipleChoice<>("checkboxes", selectedEmbargoGroups, availableEmbargoGroups);
        embargoGroups.setPrefix("<div>");
        embargoGroups.setSuffix("</div>");
        add(embargoGroups);
        this.action = action;
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);

        response.render(CssHeaderItem.forReference(CSS));
    }

    @Override
    public IModel<String> getTitle() {
        return new StringResourceModel("select-embargo-groups-title", this, null);
    }

    @Override
    public IValueMap getProperties() {
        return DialogConstants.SMALL;
    }

    @Override
    protected void onOk() {
        try {
            action.invokeWorkflow();
        } catch (Exception e) {
           log.error("error invoking wf on set embargo dialog", e);
        }
    }
}
