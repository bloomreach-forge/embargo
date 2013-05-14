package org.hippoecm.repository.quartz;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.ext.WorkflowInvocation;
import org.hippoecm.repository.ext.WorkflowInvocationHandlerModule;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;

/**
 * @version "$Id$"
 */
public class CalendarSchedulerInvocationModule implements WorkflowInvocationHandlerModule {

    Calendar timestamp;

    public CalendarSchedulerInvocationModule(Calendar calendar) {
        this.timestamp = calendar;
    }

    public Object submit(WorkflowManager manager, WorkflowInvocation invocation) {
        try {
            if (SchedulerModule.log.isDebugEnabled()) {
                SchedulerModule.log.debug("Storing scheduled workflow {}", invocation.toString());
            }
            final Session session = invocation.getSubject().getSession();
            Scheduler scheduler = SchedulerModule.getScheduler(session);
            Node subject = invocation.getSubject();
            Node handle = subject.getParent();
            if (handle.isNodeType("mix:versionable") && !handle.isCheckedOut()) {
                //TODO: Use version manager for this
                handle.checkout();
            }
            Node request = handle.addNode("embargo:request", "embargo:job");
            request.addMixin("mix:referenceable");

            String detail = request.getPath();
            JobDetail jobDetail = new JobDetail(detail, null, WorkflowJob.class);
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put("invocation", invocation);
            jobDataMap.put("document", invocation.getSubject().getIdentifier());
            jobDetail.setJobDataMap(jobDataMap);

            Trigger trigger = createTrigger(detail + "/default");
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (RepositoryException ex) {
            SchedulerModule.log.error("failure storing scheduled workflow", ex);
        } catch (SchedulerException ex) {
            SchedulerModule.log.error("failure storing scheduled workflow", ex);
        }
        return null;
    }

    protected Trigger createTrigger(String name) {
        SimpleTrigger trigger = new SimpleTrigger(name, null, timestamp.getTime());
        trigger.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);
        return trigger;
    }

}
