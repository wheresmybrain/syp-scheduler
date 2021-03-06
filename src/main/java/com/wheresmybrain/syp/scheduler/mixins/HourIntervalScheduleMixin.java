package com.wheresmybrain.syp.scheduler.mixins;

import com.wheresmybrain.syp.scheduler.ScheduledTask;
import com.wheresmybrain.syp.scheduler.SypScheduler;
import com.wheresmybrain.syp.scheduler.Task;

import java.util.Date;

/**
 * This mixin-style class lets any <code>Task</code> run inside the {@link SypScheduler}
 * on an hour-interval schedule.
 *
 * @author Chris McFarland
 */
public class HourIntervalScheduleMixin extends AbstractMixin {

    private int intervalInHours;

    /**
     * Creates a "schedule-able" task that executes periodically according to the
     * specified intervalInHours.
     *
     * @param task the task to execute on a schedule.
     * @param intervalInHours the number of hours in the interval.
     * @throws IllegalArgumentException if any invalid values are passed
     */
    public HourIntervalScheduleMixin(Task task, int intervalInHours) {
        super(task);
        if (intervalInHours > 0) {
            this.intervalInHours = intervalInHours;
        } else {
            String err = "intervalInHours value ("+intervalInHours+") cannot be zero or negative!";
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
        long millisToExecution = this.intervalInHours * 1000 * 60 * 60;
        long timestamp = millisToExecution + new Date().getTime();
        return new Date(timestamp);
    }

}
