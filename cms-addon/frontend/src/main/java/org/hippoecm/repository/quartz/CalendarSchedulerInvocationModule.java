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

import java.lang.reflect.InvocationHandler;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.ext.WorkflowImpl;

import org.hippoecm.repository.impl.WorkflowManagerImpl;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.scheduling.RepositoryJob;
import org.onehippo.repository.scheduling.RepositoryJobInfo;
import org.onehippo.repository.scheduling.RepositoryJobSimpleTrigger;
import org.onehippo.repository.scheduling.RepositoryJobTrigger;
import org.onehippo.repository.scheduling.RepositoryScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class CalendarSchedulerInvocationModule extends WorkflowManagerImpl {

    private static final Logger log = LoggerFactory.getLogger(SchedulerModule.class);
    private static final long serialVersionUID = 1L;

    Calendar timestamp;

    public CalendarSchedulerInvocationModule(final Session session) throws RepositoryException {
        super(session);
    }

    /*  public CalendarSchedulerInvocationModule(Calendar calendar) {
          this.timestamp = calendar;
      }
  */
    public Object submit(WorkflowManager manager, InvocationHandler invocation) {
      /*  try {
            if (log.isDebugEnabled()) {
                log.debug("Storing scheduled workflow for document {}", invocation.getSubject().getPath());
            }
            final Session session = invocation.getSubject().getSession();
            final RepositoryScheduler scheduler = HippoServiceRegistry.getService(RepositoryScheduler.class);
            if (scheduler != null) {
                Node subject = invocation.getSubject();
                Node handle = subject.getParent();
                JcrUtils.ensureIsCheckedOut(handle, false);
                Node requestNode = handle.addNode("embargo:request", "embargo:job");
                requestNode.addMixin("mix:referenceable");
                // TODO mm
                final RepositoryJobInfo repositoryJobInfo = new RepositoryJobInfo("embargo", RepositoryJob.class);
                scheduler.scheduleJob(repositoryJobInfo, createTrigger("default"));
                //scheduler.scheduleJob(new WorkflowJobDetail(requestNode, invocation), createTrigger("default"));
            } else {
                log.warn("Scheduler is not available, cannot schedule workflow for document {}", invocation.getSubject().getPath());
            }
        } catch (RepositoryException ex) {
            log.error("failure storing scheduled workflow", ex);
        }*/
        return null;
    }

    protected RepositoryJobTrigger createTrigger(String name) {
        final RepositoryJobTrigger trigger = new RepositoryJobSimpleTrigger(name, timestamp.getTime());
        //trigger.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);
        return trigger;
    }

}
