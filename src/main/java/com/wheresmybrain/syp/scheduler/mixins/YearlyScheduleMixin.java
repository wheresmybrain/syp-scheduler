package com.wheresmybrain.syp.scheduler.mixins;

import com.wheresmybrain.syp.scheduler.ScheduledTask;
import com.wheresmybrain.syp.scheduler.TaskScheduler;
import com.wheresmybrain.syp.scheduler.enums.MonthOfYear;
import com.wheresmybrain.syp.scheduler.Task;
import com.wheresmybrain.syp.scheduler.tasks.TaskErrorException;
import com.wheresmybrain.syp.scheduler.utils.TruncatedDate;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * This mixin-style class lets any <code>Task</code> run inside the {@link TaskScheduler}
 * on a yearly schedule on the specified monthOfYear and dayOfMonth.
 * <p/>
 * This is a yearly execution task, which means it should execute once per year, so if it
 * fails the task will automatically retry every hour until it succeeds, or when it hits
 * the next scheduled time.
 *
 * @author Chris McFarland
 */
public class YearlyScheduleMixin extends AbstractMixin {

    private MonthOfYear monthOfYear;
    private int dayOfMonth;
    private int hourOfDay;
    private int minuteOfHour;

    // for retries on failure
    private boolean okToRetry = true; //default=retry is ok
    private boolean retry;

    /**
     * @param task the task to execute on a schedule.
     * @param monthOfYear enum constant representing the month to execute in every year.
     * @param dayOfMonth set to execute on day 1-31 (29-31 will execute on last day of month
     *   for months with less than those number of days), or set to constant TaskScheduler.LAST_DAY_OF_MONTH
     *   with (optionally) a day offset. Examples: dayOfMonth=16 (executes on 16th of every
     *   month), dayOfMonth=31 (executes on last day of every month),
     *   dayOfMonth=TaskScheduler..LAST_DAY_OF_MONTH (executes on last day of every month),
     *   dayOfMonth=TaskScheduler..LAST_DAY_OF_MONTH-1 (executes on next-to-last day of month).
     * @param hourOfDay hour (0-23) to execute on the specified dayOfMonth
     * @param minuteOfHour minute (0-59) to execute in the specified hourOfDay
     * @throws IllegalArgumentException if any invalid values are passed
     */
    public YearlyScheduleMixin(
            Task task,
            MonthOfYear monthOfYear,
            int dayOfMonth,
            int hourOfDay,
            int minuteOfHour)
    {
        super(task);
        if (monthOfYear == null) {
            throw new IllegalArgumentException("'monthOfYear' (null) must be specified");
        } else if ((dayOfMonth < TaskScheduler.LAST_DAY_OF_MONTH-30) || (dayOfMonth > TaskScheduler.LAST_DAY_OF_MONTH && dayOfMonth < 1) || (dayOfMonth > 31)) {
            String msg = "'dayOfMonth' must be either 1-31 or set to constant LAST_DAY_OF_MONTH minus some day offset (up to 30)";
            throw new IllegalArgumentException(msg);
        } else if (hourOfDay < 0 || hourOfDay > 23 || minuteOfHour < 0 || minuteOfHour > 59) {
            throw new IllegalArgumentException("'hourOfDay' must be specified 0-23 and 'minuteOfHour' 0-59");
        }
        this.monthOfYear = monthOfYear;
        this.dayOfMonth = dayOfMonth;
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
    public YearlyScheduleMixin setOkToRetry(boolean okToRetry) {
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
        switch (monthOfYear) {
            case JANUARY :
                scheduled.set(Calendar.MONTH, Calendar.JANUARY);
                break;
            case FEBRUARY :
                scheduled.set(Calendar.MONTH, Calendar.FEBRUARY);
                break;
            case MARCH :
                scheduled.set(Calendar.MONTH, Calendar.MARCH);
                break;
            case APRIL :
                scheduled.set(Calendar.MONTH, Calendar.APRIL);
                break;
            case MAY :
                scheduled.set(Calendar.MONTH, Calendar.MAY);
                break;
            case JUNE :
                scheduled.set(Calendar.MONTH, Calendar.JUNE);
                break;
            case JULY :
                scheduled.set(Calendar.MONTH, Calendar.JULY);
                break;
            case AUGUST :
                scheduled.set(Calendar.MONTH, Calendar.AUGUST);
                break;
            case SEPTEMBER :
                scheduled.set(Calendar.MONTH, Calendar.SEPTEMBER);
                break;
            case OCTOBER :
                scheduled.set(Calendar.MONTH, Calendar.OCTOBER);
                break;
            case NOVEMBER :
                scheduled.set(Calendar.MONTH, Calendar.NOVEMBER);
                break;
            case DECEMBER :
                scheduled.set(Calendar.MONTH, Calendar.DECEMBER);
                break;
        }
        // we are scheduling a particular day of month each year
        this.dayOfMonth = MonthUtils.calculateDayOfMonth(this.dayOfMonth, scheduled.getTime());
        scheduled.set(Calendar.DAY_OF_MONTH, this.dayOfMonth);
        scheduled.set(Calendar.HOUR_OF_DAY, this.hourOfDay);
        scheduled.set(Calendar.MINUTE, this.minuteOfHour);
        if (scheduled.getTime().before(new Date())) {
            // next execution needs to be next year
            scheduled.add(Calendar.YEAR, 1);
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
