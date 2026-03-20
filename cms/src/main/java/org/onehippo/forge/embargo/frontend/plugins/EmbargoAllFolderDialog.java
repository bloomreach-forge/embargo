/*
 * Copyright 2026 Bloomreach B.V. (http://www.bloomreach.com)
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
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmbargoAllFolderDialog extends Dialog<Boolean> {

    private static final Logger log = LoggerFactory.getLogger(EmbargoAllFolderDialog.class);

    private final StdWorkflow<FolderWorkflow> action;
    private final IModel<String> titleModel;

    public EmbargoAllFolderDialog(final StdWorkflow<FolderWorkflow> action, final IModel<Boolean> recurseModel,
                                   final IModel<String> titleModel, final IModel<String> messageModel) {
        super(recurseModel);
        this.titleModel = titleModel;
        add(new Label("message", messageModel));
        add(new CheckBox("recurse", recurseModel));
        add(new Label("recurseLabel", new ResourceModel("recurse-sub-folders-label")));
        this.action = action;
    }

    @Override
    public IModel<String> getTitle() {
        return titleModel;
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
            log.error("Error invoking embargo all in folder workflow", e);
        }
    }
}
