package com.wheresmybrain.syp.scheduler.testtasks;

import com.wheresmybrain.syp.scheduler.ScheduledTask;
import com.wheresmybrain.syp.scheduler.SchedulerContext;
import com.wheresmybrain.syp.scheduler.tasks.RecurringTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * This is an example of a custom recurring task -
 * Logs the tasks (executing and scheduled) inside the TaskScheduler
 * once per minute.
 *
 * @author Chris McFarland
 */
public class LogSchedulerState extends RecurringTask {

    private static Logger log = LoggerFactory.getLogger(LogSchedulerState.class);

    private static final long ONE_MINUTE = 1000 * 60;

    private boolean executed;

    /**
     * @see ScheduledTask#executeTask(SchedulerContext)
     */
    @Override
    public void executeTask(SchedulerContext context) throws Throwable {
        log.info(this.getScheduler().getState(true));
        this.executed = true;
    }

    @Override
    public String[] getDebugState() {
        return executed ? new String[] {"Already Executed!"} : new String[] {"Did not Execute Yet!"};
    }

    /**
     * Schedule to run every minute
     * @see ScheduledTask#getNextExecutionTime()
     */
    @Override
    protected Date getNextExecutionTime() {
        long millisToExecution = ONE_MINUTE;
        long timestamp = millisToExecution + new Date().getTime();
        return new Date(timestamp);
    }

}
