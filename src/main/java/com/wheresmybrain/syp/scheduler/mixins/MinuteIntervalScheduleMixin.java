package com.wheresmybrain.syp.scheduler.mixins;

import com.wheresmybrain.syp.scheduler.ScheduledTask;
import com.wheresmybrain.syp.scheduler.SypScheduler;
import com.wheresmybrain.syp.scheduler.Task;

import java.util.Date;

/**
 * This mixin-style class lets any <code>Task</code> run inside the {@link SypScheduler}
 * on a minute-interval schedule.
 *
 * @author Chris McFarland
 */
public class MinuteIntervalScheduleMixin extends AbstractMixin {

    private int intervalInMinutes;

    /**
     * Creates a "schedule-able" task that executes periodically according to the
     * specified intervalInMinutes.
     *
     * @param task the task to execute on a schedule.
     * @param intervalInMinutes the number of minutes in the interval - MUST be > 0.
     * @throws IllegalArgumentException if any invalid values are passed
     */
    public MinuteIntervalScheduleMixin(Task task, int intervalInMinutes) {
        super(task);
        if (intervalInMinutes > 0) {
            this.intervalInMinutes = intervalInMinutes;
        } else {
            String err = "intervalInMinutes value ("+intervalInMinutes+") cannot be zero or negative!";
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
        long millisToExecution = this.intervalInMinutes * 1000 * 60;
        long timestamp = millisToExecution + new Date().getTime();
        return new Date(timestamp);
    }

}
