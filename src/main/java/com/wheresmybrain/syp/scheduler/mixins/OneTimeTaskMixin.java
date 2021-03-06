package com.wheresmybrain.syp.scheduler.mixins;

import com.wheresmybrain.syp.scheduler.ScheduledTask;
import com.wheresmybrain.syp.scheduler.SchedulerContext;
import com.wheresmybrain.syp.scheduler.Task;

import java.util.Date;

/**
 * Executes a task one time only.
 *
 * @author Chris McFarland
 */
public class OneTimeTaskMixin extends ScheduledTask {

    private Task task;

    public OneTimeTaskMixin(Task task) {
        this.task = task;
    }

    /**
     * @see Task#executeTask(SchedulerContext)
     */
    @Override
    public void executeTask(SchedulerContext schedulerContext) throws Throwable {
        this.task.executeTask(schedulerContext);
    }

    /**
     * Calls <code>getDebugState()</code> method on the wrapped task object.
     * @see Task#getDebugState()
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
