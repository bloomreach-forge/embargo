/*
 *  Copyright 2010 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.forge.embargo.frontend.plugins;

import java.util.Date;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin.WorkflowAction;

public class ScheduleDialog extends WorkflowAction.DateDialog {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    final String title;

    public ScheduleDialog(WorkflowAction action, PropertyModel<Date> dateModel, final String title, final String text) {
        action.super(new ResourceModel(text), dateModel);
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
