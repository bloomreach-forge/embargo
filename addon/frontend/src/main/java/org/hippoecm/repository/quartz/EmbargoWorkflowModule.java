package org.hippoecm.repository.quartz;

import java.util.Calendar;
import java.util.Date;

import org.hippoecm.repository.api.CronExpression;
import org.hippoecm.repository.ext.WorkflowInvocationHandlerModule;
import org.hippoecm.repository.ext.WorkflowInvocationHandlerModuleFactory;
import org.hippoecm.repository.ext.WorkflowManagerModule;
import org.hippoecm.repository.ext.WorkflowManagerRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class EmbargoWorkflowModule implements WorkflowManagerModule {
    public EmbargoWorkflowModule() {
    }

    @Override
    public void register(WorkflowManagerRegister register) {
        register.bind(Calendar.class, new WorkflowInvocationHandlerModuleFactory<Calendar>() {
            @Override
            public WorkflowInvocationHandlerModule createInvocationHandler(Calendar calendar) {
                return new CalendarSchedulerInvocationModule(calendar);
            }
        });

    }
}
