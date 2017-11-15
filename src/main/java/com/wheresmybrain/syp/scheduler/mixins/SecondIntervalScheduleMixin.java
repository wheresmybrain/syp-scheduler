package com.wheresmybrain.syp.scheduler.mixins;

import com.wheresmybrain.syp.scheduler.ScheduledTask;
import com.wheresmybrain.syp.scheduler.TaskScheduler;
import com.wheresmybrain.syp.scheduler.iTask;

import java.util.Date;

/**
 * This mixin-style class lets any <code>iTask</code> run inside the {@link TaskScheduler}
 * on a second-interval schedule.
 *
 * @author Chris McFarland
 */
public class SecondIntervalScheduleMixin extends AbstractMixin {

    private int intervalInSeconds;

    /**
     * Creates a "schedule-able" task that executes periodically according to the
     * specified intervalInSeconds.
     *
     * @param task the task to execute on a schedule.
     * @param intervalInSeconds the number of seconds in the interval - MUST be > 0.
     * @throws IllegalArgumentException if any invalid values are passed
     */
    public SecondIntervalScheduleMixin(iTask task, int intervalInSeconds) {
        super(task);
        if (intervalInSeconds > 0) {
            this.intervalInSeconds = intervalInSeconds;
        } else {
            String err = "intervalInSeconds value ("+intervalInSeconds+") cannot be zero or negative!";
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
        long millisToExecution = this.intervalInSeconds * 1000;
        long timestamp = millisToExecution + new Date().getTime();
        return new Date(timestamp);
    }

}
