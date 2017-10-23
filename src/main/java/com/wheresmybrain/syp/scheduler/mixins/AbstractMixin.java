package com.wheresmybrain.syp.scheduler.mixins;

import com.wheresmybrain.syp.scheduler.ScheduledTask;
import com.wheresmybrain.syp.scheduler.SchedulerContext;
import com.wheresmybrain.syp.scheduler.iTask;
import com.wheresmybrain.syp.scheduler.tasks.RecurringTask;
import com.wheresmybrain.syp.scheduler.utils.TimeUtils;

import java.util.concurrent.TimeUnit;

/**
 * Abstract base class for all the "Mixin" classes in this package. These are
 * called "Mixin" because the subclasses of this base class "mix in" scheduling
 * capability to any class that implements {@link iTask}.
 *
 * @author Chris McFarland
 */
public abstract class AbstractMixin extends RecurringTask {

    private iTask task;

    protected AbstractMixin(iTask task) {
        if (task != null) {
            this.task = task;
        } else {
            throw new IllegalArgumentException("task cannot be null!");
        }
    }

    /**
     * Calls the <code>executeTask()</code> method on the wrapped task object.
     * @see iTask#executeTask(SchedulerContext)
     */
    @Override
    public final void executeTask(SchedulerContext schedulerContext) throws Throwable {
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
     * Called by the framework for troubleshooting purposes.
     */
    public iTask getInternalTask() {
        return this.task;
    }

    /**
     * Returns an information String containing containing the internal task class,
     * task id and internal state.
     */
    @Override
    public final String getTaskInfo() {
        StringBuilder sb = new StringBuilder(this.task.getClass().getSimpleName())
                .append(" (Task #").append(this.getTaskId()).append(") execution{")
                .append(TimeUtils.getTimeDescription(this.getDelay(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS))
                .append("} internal state: ").append(this.getInternalState());
        if (this.isPaused()) sb.append(" (PAUSED)");
        return sb.toString();
    }

    /**
     * Overrides superclass method to return the class of the internal task. This method
     * is used for reporting purposes.
     */
    @Override
    public Class<?> getTaskClass() {
        return this.task.getClass();
    }

    /**
     * @see ScheduledTask
     */
    @Override
    public String toString() {
        return this.getTaskInfo();
    }

}
