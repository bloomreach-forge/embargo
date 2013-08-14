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

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.validation.validator.DateValidator;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin.WorkflowAction;
import org.hippoecm.frontend.plugins.yui.datetime.YuiDateTimeField;

public class ScheduleDialog extends WorkflowAction.WorkflowDialog {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    final String title;

    public ScheduleDialog(WorkflowAction action, PropertyModel<Date> dateModel, final String title, final String text) {
        action.super();
        Calendar minimum = Calendar.getInstance();
        minimum.setTime(dateModel.getObject());
        minimum.set(Calendar.SECOND, 0);
        minimum.set(Calendar.MILLISECOND, 0);
        // if you want to round upwards, the following ought to be executed: minimum.add(Calendar.MINUTE, 1);
        dateModel.setObject(minimum.getTime());
        add(new Label("question", new ResourceModel(text)));
        YuiDateTimeField ydtf = new YuiDateTimeField("value", dateModel);
        // changed from the default WorkflowAction.DateDialog to use the current date/time as a minimal instead of the
        // previous date found in the schedule.
        ydtf.add(DateValidator.minimum(Calendar.getInstance().getTime()));
        add(ydtf);
        setFocusOnCancel();
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
