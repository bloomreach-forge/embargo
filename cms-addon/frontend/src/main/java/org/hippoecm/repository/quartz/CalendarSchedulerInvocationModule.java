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
package org.hippoecm.repository.quartz;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.ext.WorkflowInvocation;
import org.hippoecm.repository.ext.WorkflowInvocationHandlerModule;
import org.hippoecm.repository.quartz.workflow.WorkflowJobDetail;
import org.hippoecm.repository.util.JcrUtils;
import org.quartz.JobDataMap;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class CalendarSchedulerInvocationModule implements WorkflowInvocationHandlerModule {

    private static final Logger log = LoggerFactory.getLogger(SchedulerModule.class);

    Calendar timestamp;

    public CalendarSchedulerInvocationModule(Calendar calendar) {
        this.timestamp = calendar;
    }

    public Object submit(WorkflowManager manager, WorkflowInvocation invocation) {
        try {
            if(log.isDebugEnabled()) {
                log.debug("Storing scheduled workflow for document {}", invocation.getSubject().getPath());
            }
            final Session session = invocation.getSubject().getSession();
            Scheduler scheduler = SchedulerModule.getScheduler(session);
            if (scheduler!=null) {
                Node subject = invocation.getSubject();
                Node handle = subject.getParent();
                JcrUtils.ensureIsCheckedOut(handle, false);
                Node requestNode = handle.addNode("embargo:request", "embargo:job");
                requestNode.addMixin("mix:referenceable");
                scheduler.scheduleJob(new WorkflowJobDetail(requestNode, invocation), createTrigger("default"));
            } else {
                log.warn("Scheduler is not available, cannot schedule workflow for document {}", invocation.getSubject().getPath());
            }
        } catch (RepositoryException ex) {
            log.error("failure storing scheduled workflow", ex);
        } catch (SchedulerException ex) {
            log.error("failure storing scheduled workflow", ex);
        }
        return null;
    }

    protected Trigger createTrigger(String name) {
        SimpleTrigger trigger = new SimpleTrigger(name, null, timestamp.getTime());
        trigger.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);
        return trigger;
    }

}
