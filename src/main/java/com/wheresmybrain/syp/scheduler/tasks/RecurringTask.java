package com.wheresmybrain.syp.scheduler.tasks;

import com.wheresmybrain.syp.scheduler.ScheduledTask;
import com.wheresmybrain.syp.scheduler.SypScheduler;
import com.wheresmybrain.syp.scheduler.enums.TaskInternalState;
import com.wheresmybrain.syp.scheduler.utils.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Abstract base class for tasks that occur on an interval or some other periodic
 * schedule. When the class is first added to the Task Scheduler it will execute
 * when the scheduler first starts. But subsequent executions will be scheduled
 * according to the recurring interval implemented by the subclass.
 * <p/>
 * This class <b>should not</b> be extended directly by the developer unless custom scheduling
 * behavior is required. There are easier ways to create Tasks to execute
 * on a schedule - see {@link SypScheduler} javadoc.
 *
 * @author Chris McFarland
 */
public abstract class RecurringTask extends ScheduledTask {

    private static Logger log = LoggerFactory.getLogger(RecurringTask.class);

    /**
     * Executes this task then schedules the next occurrence.
     *
     * @see ScheduledTask#run()
     */
    @Override
    public final void run() {
        try {
            super.run();
        } finally {
            // If the task Thread is not interrupted, add
            // self to scheduler to execute next occurrence.
            if (!Thread.currentThread().isInterrupted()) {
                this.setInternalState(TaskInternalState.ACTIVE);
                this.getScheduler().scheduleCustomTaskExecution(this, -1);
            }
            log.debug("task ("+this+") rescheduled itself for next execution: "+
                    TimeUtils.getTimeDescription(this.getDelay(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS));
        }
    }

}
