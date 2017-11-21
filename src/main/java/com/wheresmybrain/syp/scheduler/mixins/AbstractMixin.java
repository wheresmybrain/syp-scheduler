package com.wheresmybrain.syp.scheduler.mixins;

import com.wheresmybrain.syp.scheduler.ScheduledTask;
import com.wheresmybrain.syp.scheduler.SchedulerContext;
import com.wheresmybrain.syp.scheduler.Task;
import com.wheresmybrain.syp.scheduler.tasks.RecurringTask;
import com.wheresmybrain.syp.scheduler.utils.TimeUtils;

import java.util.concurrent.TimeUnit;

/**
 * Abstract base class for all the "Mixin" classes in this package. These are
 * called "Mixin" because the subclasses of this base class "mix in" scheduling
 * capability to any class that implements {@link Task}.
 *
 * @author Chris McFarland
 */
public abstract class AbstractMixin extends RecurringTask {

    private Task task;

    protected AbstractMixin(Task task) {
        if (task != null) {
            this.task = task;
        } else {
            throw new IllegalArgumentException("task cannot be null!");
        }
    }

    /**
     * Calls the <code>executeTask()</code> method on the wrapped task object.
     * @see Task#executeTask(SchedulerContext)
     */
    @Override
    public final void executeTask(SchedulerContext schedulerContext) throws Throwable {
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
     * Called by the framework for troubleshooting purposes.
     */
    public Task getInternalTask() {
        return this.task;
    }

    /**
     * Returns an information String containing containing the internal task class,
     * task id and internal state.
     */
    @Override
    public final String getTaskInfo() {
        long delay = getDelay(TimeUnit.MILLISECONDS);
        StringBuilder sb = new StringBuilder(task.getClass().getSimpleName())
                .append(" (Task #").append(getTaskId()).append(") ");
        if (delay > 0) {
            sb.append("next execution[")
                    .append(TimeUtils.getTimeDescription(delay, TimeUnit.MILLISECONDS))
                    .append("] ");
        }
        sb.append("internal state: ").append(getInternalState());
        if (isPaused()) sb.append(" (PAUSED)");
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
