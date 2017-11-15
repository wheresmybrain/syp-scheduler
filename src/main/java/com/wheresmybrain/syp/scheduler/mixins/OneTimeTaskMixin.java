package com.wheresmybrain.syp.scheduler.mixins;

import com.wheresmybrain.syp.scheduler.ScheduledTask;
import com.wheresmybrain.syp.scheduler.SchedulerContext;
import com.wheresmybrain.syp.scheduler.iTask;

import java.util.Date;

/**
 * Executes a task one time only.
 *
 * @author Chris McFarland
 */
public class OneTimeTaskMixin extends ScheduledTask {

    private iTask task;

    public OneTimeTaskMixin(iTask task) {
        this.task = task;
    }

    /**
     * @see iTask#executeTask(SchedulerContext)
     */
    @Override
    public void executeTask(SchedulerContext schedulerContext) throws Throwable {
        this.task.executeTask(schedulerContext);
    }

    /**
     * Calls <code>getDebugState()</code> method on the wrapped task object.
     * @see iTask#getDebugState()
     */
    @Override
    public final String[] getDebugState() {
        return this.task.getDebugState();
    }

    /**
     * @see ScheduledTask#getNextExecutionTime()
     */
    @Override
    protected Date getNextExecutionTime() {
        throw new IllegalStateException("illegal to call this method");
    }

}
