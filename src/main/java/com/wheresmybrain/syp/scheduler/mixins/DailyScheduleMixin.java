package com.wheresmybrain.syp.scheduler.mixins;

import com.wheresmybrain.syp.scheduler.ScheduledTask;
import com.wheresmybrain.syp.scheduler.SypScheduler;
import com.wheresmybrain.syp.scheduler.Task;
import com.wheresmybrain.syp.scheduler.tasks.TaskErrorException;
import com.wheresmybrain.syp.scheduler.utils.TruncatedDate;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * This mixin-style class lets any <code>Task</code> run inside the {@link SypScheduler}
 * on a daily schedule at a specified time.
 * <p/>
 * This is a daily execution task, which means it should execute once per day, so if it
 * fails the task will automatically retry every hour until it succeeds, or when it hits
 * the next scheduled time.
 *
 * @author Chris McFarland
 */
public class DailyScheduleMixin extends AbstractMixin {

    private int hourOfDay;
    private int minuteOfHour;
    private int secondOfMinute;

    // for retries on failure
    private boolean okToRetry = true; //default=retry is ok
    private boolean retry;

    /**
     * Creates a "schedule-able" task that executes the task every day at the
     * specified time of day.
     *
     * @param task the task to execute on a schedule.
     * @param hourOfDay hour (0-23) to execute every day
     * @param minuteOfHour minute (0-59) to execute in the specified hourOfDay
     * @param secondOfMinute second (0-59) to execute in the specified secondOfMinute
     * @throws IllegalArgumentException if any invalid values are passed
     */
    public DailyScheduleMixin(Task task, int hourOfDay, int minuteOfHour, int secondOfMinute) {
        super(task);
        if (hourOfDay < 0 || hourOfDay > 23 || minuteOfHour < 0 || minuteOfHour > 59 || secondOfMinute < 0 || secondOfMinute > 59) {
            throw new IllegalArgumentException("'hourOfDay' must be specified 0-23 and 'minuteOfHour' 0-59 and 'secondOfMinute' 0-59");
        }
        this.hourOfDay = hourOfDay;
        this.minuteOfHour = minuteOfHour;
        this.secondOfMinute = secondOfMinute;
    }

    /**
     * Sets the 'okToRetry' flag to true or false. If true (the default), then
     * this daily task will execute again every hour until it succeeds (or the
     * next scheduled time occurs. Since 'okToRetry' defaults to true, then this
     * method is mainly to disable it.
     *
     * @param okToRetry flag value determines whether this task attempts to
     *   execute again after a failure
     * @return reference to this task so it can be chained with the constructor
     */
    public DailyScheduleMixin setOkToRetry(boolean okToRetry) {
        this.okToRetry = okToRetry;
        return this;
    }

    /**
     * Override to add retry (after failure) behavior if specified.
     * @see ScheduledTask#handleError(TaskErrorException)
     */
    @Override
    protected void handleError(TaskErrorException ex) {
        super.handleError(ex);
        if (this.okToRetry) {
            // set flag to retry
            // execution
            this.retry = true;
        }
    }

    /**
     * Called by the framework to determine the next time this task should execute.
     * If the 'retry' flag is set (because execution failed), then next execution will
     * be rescheduled in an hour.
     *
     * @see ScheduledTask#getNextExecutionTime()
     */
    @Override
    protected final Date getNextExecutionTime() {

        // calculate next scheduled execution
        TruncatedDate truncated = new TruncatedDate();
        truncated.truncate(TruncatedDate.TruncateLevel.DAY); //trim to beginning of today
        Calendar scheduled = new GregorianCalendar();
        scheduled.setTime(truncated);
        scheduled.set(Calendar.HOUR_OF_DAY, this.hourOfDay);
        scheduled.set(Calendar.MINUTE, this.minuteOfHour);
        scheduled.set(Calendar.SECOND, this.secondOfMinute);
        if (scheduled.getTime().before(new Date())) {
            // next execution needs to be tomorrow
            scheduled.add(Calendar.DAY_OF_MONTH, 1);
        }
        Date nextScheduledExecution = scheduled.getTime();

        // determine if task needs to retry sooner
        if (this.retry) {
            this.retry = false; //clear the flag
            // retry in an hour if it's before the
            // regularly scheduled time
            Calendar retry = new GregorianCalendar();
            retry.add(Calendar.HOUR, 1);
            if (retry.before(scheduled)) {
                nextScheduledExecution = retry.getTime();
            }
        }

        return nextScheduledExecution;
    }
}
