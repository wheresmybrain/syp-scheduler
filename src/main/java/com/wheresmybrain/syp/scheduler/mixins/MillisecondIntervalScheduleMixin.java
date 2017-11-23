package com.wheresmybrain.syp.scheduler.mixins;

import com.wheresmybrain.syp.scheduler.ScheduledTask;
import com.wheresmybrain.syp.scheduler.SypScheduler;
import com.wheresmybrain.syp.scheduler.Task;

import java.util.Date;

/**
 * This mixin-style class lets any <code>Task</code> run inside the {@link SypScheduler}
 * on a millisecond-interval schedule.
 *
 * @author Chris McFarland
 */
public class MillisecondIntervalScheduleMixin extends AbstractMixin {

    private int intervalInMilliseconds;

    /**
     * Creates a "schedule-able" task that executes periodically according to the
     * specified intervalInMilliseconds.
     *
     * @param task the task to execute on a schedule.
     * @param intervalInMilliseconds the number of milliseconds in the interval - MUST be > 0.
     * @throws IllegalArgumentException if any invalid values are passed
     */
    public MillisecondIntervalScheduleMixin(Task task, int intervalInMilliseconds) {
        super(task);
        if (intervalInMilliseconds > 0) {
            this.intervalInMilliseconds = intervalInMilliseconds;
        } else {
            String err = "intervalInMilliseconds value ("+intervalInMilliseconds+") cannot be zero or negative!";
            throw new IllegalArgumentException(err);
        }
    }

    /**
     * Called by the framework to determine the next time this task should execute.
     *
     * @see ScheduledTask#getNextExecutionTime()
     */
    @Override
    protected final Date getNextExecutionTime() {
        long timestamp = this.intervalInMilliseconds + new Date().getTime();
        return new Date(timestamp);
    }

}
