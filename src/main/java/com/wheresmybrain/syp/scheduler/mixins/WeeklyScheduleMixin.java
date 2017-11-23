package com.wheresmybrain.syp.scheduler.mixins;

import com.wheresmybrain.syp.scheduler.ScheduledTask;
import com.wheresmybrain.syp.scheduler.SypScheduler;
import com.wheresmybrain.syp.scheduler.enums.DayOfWeek;
import com.wheresmybrain.syp.scheduler.Task;
import com.wheresmybrain.syp.scheduler.tasks.TaskErrorException;
import com.wheresmybrain.syp.scheduler.utils.TruncatedDate;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * This mixin-style class lets any <code>Task</code> run inside the {@link SypScheduler}
 * on a weekly schedule on a specified dayOfWeek and time.
 * <p/>
 * This is a weekly execution task, which means it should execute once per week, so if it
 * fails the task will automatically retry every hour until it succeeds, or when it hits the
 * next scheduled time.
 *
 * @author Chris McFarland
 */
public class WeeklyScheduleMixin extends AbstractMixin {

    private DayOfWeek dayOfWeek;
    private int hourOfDay;
    private int minuteOfHour;

    // for retries on failure
    private boolean okToRetry = true; //default=retry is ok
    private boolean retry;

    /**
     * Creates a "schedule-able" task that executes the task every week on the
     * specified day and time.
     *
     * @param task the task to execute on a schedule.
     * @param dayOfWeek enum constant for day-of-week
     * @param hourOfDay hour (0-23) to execute on the specified dayOfWeek
     * @param minuteOfHour minute (0-59) to execute in the specified hourOfDay
     * @throws IllegalArgumentException if any invalid values are passed
     */
    public WeeklyScheduleMixin(Task task, DayOfWeek dayOfWeek, int hourOfDay, int minuteOfHour) {
        super(task);
        if (dayOfWeek == null) {
            throw new IllegalArgumentException("'dayOfWeek' (null) must be specified");
        } else if (hourOfDay < 0 || hourOfDay > 23 || minuteOfHour < 0 || minuteOfHour > 59) {
            throw new IllegalArgumentException("'hourOfDay' must be specified 0-23 and 'minuteOfHour' 0-59");
        }
        this.dayOfWeek = dayOfWeek;
        this.hourOfDay = hourOfDay;
        this.minuteOfHour = minuteOfHour;
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
    public WeeklyScheduleMixin setOkToRetry(boolean okToRetry) {
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
    protected Date getNextExecutionTime() {

        // calculate next scheduled execution
        TruncatedDate truncated = new TruncatedDate();
        truncated.truncate(TruncatedDate.TruncateLevel.DAY); //trim to beginning of today
        Calendar scheduled = new GregorianCalendar();
        scheduled.setTime(truncated);
        scheduled.set(Calendar.HOUR_OF_DAY, this.hourOfDay);
        scheduled.set(Calendar.MINUTE, this.minuteOfHour);
        switch (this.dayOfWeek) {
            case SUNDAY:
                scheduled.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                break;
            case MONDAY:
                scheduled.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                break;
            case TUESDAY:
                scheduled.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
                break;
            case WEDNESDAY:
                scheduled.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
                break;
            case THURSDAY:
                scheduled.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY);
                break;
            case FRIDAY:
                scheduled.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
                break;
            case SATURDAY:
                scheduled.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
                break;
        }
        if (scheduled.getTime().before(new Date())) {
            // execution time this week has passed - execute task next week
            scheduled.add(Calendar.WEEK_OF_YEAR, 1);
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
                // execute retry instead of scheduled
                nextScheduledExecution = retry.getTime();
            }
        }

        return nextScheduledExecution;
    }

}
