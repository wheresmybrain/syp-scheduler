package com.wheresmybrain.syp.scheduler.mixins;

import com.wheresmybrain.syp.scheduler.ScheduledTask;
import com.wheresmybrain.syp.scheduler.SypScheduler;
import com.wheresmybrain.syp.scheduler.enums.DayOccurrence;
import com.wheresmybrain.syp.scheduler.enums.DayOfWeek;
import com.wheresmybrain.syp.scheduler.Task;
import com.wheresmybrain.syp.scheduler.tasks.TaskErrorException;
import com.wheresmybrain.syp.scheduler.utils.TimeUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * This mixin-style class lets any <code>Task</code> run inside the {@link SypScheduler}
 * on a monthly schedule, either on a specified dayOfMonth or dayOccurrence.
 * <p/>
 * This is a monthly execution task, which means it should execute once per month, so if it
 * fails the task will automatically retry every hour until it succeeds, or when it hits the
 * next scheduled time.
 *
 * @author Chris McFarland
 */
public class MonthlyScheduleMixin extends AbstractMixin {

    private int dayOfMonth;    //dayOfMonth mutually exclusive with dayOccurrence+dayOfWeek
    private DayOccurrence dayOccurrence;
    private DayOfWeek dayOfWeek;
    private int hourOfDay;
    private int minuteOfHour;

    // for retries on failure
    private boolean okToRetry = true; //default=retry is ok
    private boolean retry;

    /**
     * @param task the task to execute on a schedule.
     * @param dayOfMonth set to execute on day 1-31 (29-31 will execute on last day of month
     *   for months with less than those number of days), or set to constant SypScheduler.LAST_DAY_OF_MONTH
     *   with (optionally) a day offset. Examples: dayOfMonth=16 (executes on 16th of every
     *   month), dayOfMonth=31 (executes on last day of every month),
     *   dayOfMonth=SypScheduler..LAST_DAY_OF_MONTH (executes on last day of every month),
     *   dayOfMonth=SypScheduler..LAST_DAY_OF_MONTH-1 (executes on next-to-last day of month).
     * @param hourOfDay hour (0-23) to execute on the specified dayOfMonth
     * @param minuteOfHour minute (0-59) to execute in the specified hourOfDay
     * @throws IllegalArgumentException if any invalid values are passed
     */
    public MonthlyScheduleMixin(
            Task task,
            int dayOfMonth,
            int hourOfDay,
            int minuteOfHour)
    {
        super(task);
        if ((dayOfMonth < SypScheduler.LAST_DAY_OF_MONTH-30) || (dayOfMonth > SypScheduler.LAST_DAY_OF_MONTH && dayOfMonth < 1) || (dayOfMonth > 31)) {
            String msg = "'dayOfMonth' must be either 1-31 or set to constant LAST_DAY_OF_MONTH minus some day offset (up to 30)";
            throw new IllegalArgumentException(msg);
        } else if (hourOfDay < 0 || hourOfDay > 23 || minuteOfHour < 0 || minuteOfHour > 59) {
            throw new IllegalArgumentException("'hourOfDay' must be specified 0-23 and 'minuteOfHour' 0-59");
        }
        this.dayOfMonth = dayOfMonth;
        this.hourOfDay = hourOfDay;
        this.minuteOfHour = minuteOfHour;
    }

    /**
     * @param task the task to execute on a schedule.
     * @param dayOfWeek enum constant for day-of-week to schedule as an occurrence in the month
     * @param dayOccurrence enum constant representing the occurrence of the 'dayOfWeek'.
     *   For instance, setting dayOccurrence=DayOccurrence.LAST and dayOfWeek=SUNDAY will
     *   execute the task the last Sunday of every month.
     * @param hourOfDay hour (0-23) to execute on the specified dayOfMonth
     * @param minuteOfHour minute (0-59) to execute in the specified hourOfDay
     * @throws IllegalArgumentException if any invalid values are passed
     */
    public MonthlyScheduleMixin(
            Task task,
            DayOfWeek dayOfWeek,
            DayOccurrence dayOccurrence,
            int hourOfDay,
            int minuteOfHour)
    {
        super(task);
        if (dayOccurrence == null || dayOfWeek == null) {
            String doString = (dayOccurrence != null) ? dayOccurrence.name() : "null";
            String dowString = (dayOfWeek != null) ? dayOfWeek.name() : "null";
            throw new IllegalArgumentException("Both 'dayOccurrence' ("+doString+") and 'dayOfWeek' ("+dowString+") must be specified");
        } else if (hourOfDay < 0 || hourOfDay > 23 || minuteOfHour < 0 || minuteOfHour > 59) {
            throw new IllegalArgumentException("'hourOfDay' must be specified 0-23 and 'minuteOfHour' 0-59");
        }
        this.dayOfWeek = dayOfWeek;
        this.dayOccurrence = dayOccurrence;
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
    public MonthlyScheduleMixin setOkToRetry(boolean okToRetry) {
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
        // first calculate the date to set on this month
        Date currentTime = new Date();
        Calendar scheduled = null;
        if (this.dayOfMonth != 0) {
            // we are scheduling a particular day of month each month
            int dateToSet = MonthUtils.calculateDayOfMonth(this.dayOfMonth, currentTime);
            // create a Calendar instance pointing to the
            // first day of this month
            Date firstDayOfMonth = TimeUtils.getFirstDayOfMonth(currentTime);
            scheduled = new GregorianCalendar();
            scheduled.setTime(firstDayOfMonth);
            // set day of month and time info, then check
            scheduled.set(Calendar.DAY_OF_MONTH, dateToSet);
            scheduled.set(Calendar.HOUR_OF_DAY, this.hourOfDay);
            scheduled.set(Calendar.MINUTE, this.minuteOfHour);
            if (scheduled.getTime().before(currentTime)) {
                // execution time this month has passed - schedule next execution for next month
                Date date = TimeUtils.addMonths(scheduled.getTime(), 1); //safe method won't roll past end of month
                scheduled.clear();
                scheduled.setTime(date);
                // add time info
                scheduled.set(Calendar.HOUR_OF_DAY, this.hourOfDay);
                scheduled.set(Calendar.MINUTE, this.minuteOfHour);
            }
        } else {
            // we are scheduling a particular day of week occurrence each month (i.e. 2nd Tuesday)
            Date firstDayOfMonth = TimeUtils.getFirstDayOfMonth(currentTime);
            scheduled = this.getOccurrenceDate(firstDayOfMonth);
            // add time info
            scheduled.set(Calendar.HOUR_OF_DAY, this.hourOfDay);
            scheduled.set(Calendar.MINUTE, this.minuteOfHour);
            if (scheduled.getTime().before(currentTime)) {
                // execution time this month has passed - schedule next execution for next month
                firstDayOfMonth = TimeUtils.addMonths(firstDayOfMonth, 1); //safe method won't roll past end of month
                scheduled = this.getOccurrenceDate(firstDayOfMonth);
                // add time info
                scheduled.set(Calendar.HOUR_OF_DAY, this.hourOfDay);
                scheduled.set(Calendar.MINUTE, this.minuteOfHour);
            }
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

    /*
     * Determine which day in month associated with 'firstDayOfMonth' matches
     * the specified 'dayOfWeek' and 'dayOccurrence' settings.
     */
    private Calendar getOccurrenceDate(Date firstDayOfMonth) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(firstDayOfMonth);
        Date lastDayOfMonth = TimeUtils.getLastDayOfMonth(firstDayOfMonth);
        // set the occurrence parameter relative to the beginning
        // (if increment=1) or end (if increment=-1) of the month
        int increment = 1;
        int occurrence = 0;
        switch (this.dayOccurrence) {
            case FIRST :
                // searching for 1st occurrence of the day
                occurrence = 1;
                break;
            case SECOND :
                // searching for 2nd occurrence of the day
                occurrence = 2;
                break;
            case THIRD :
                // searching for 3rd occurrence of the day
                occurrence = 3;
                break;
            case FOURTH :
                // searching for 4th occurrence of the day
                occurrence = 4;
                break;
            case LAST :
                // we will search backward starting from
                // the last day of the month
                occurrence = 1;
                increment = -1;
                cal.setTime(lastDayOfMonth);
                break;
        }
        // set the day to match parameter
        int dayToMatch = -1;
        switch (this.dayOfWeek) {
            case SUNDAY :
                dayToMatch = Calendar.SUNDAY;
                break;
            case MONDAY :
                dayToMatch = Calendar.MONDAY;
                break;
            case TUESDAY :
                dayToMatch = Calendar.TUESDAY;
                break;
            case WEDNESDAY :
                dayToMatch = Calendar.WEDNESDAY;
                break;
            case THURSDAY :
                dayToMatch = Calendar.THURSDAY;
                break;
            case FRIDAY :
                dayToMatch = Calendar.FRIDAY;
                break;
            case SATURDAY :
                dayToMatch = Calendar.SATURDAY;
                break;
        }
        // loop to find the correct occurrence of the day
        int count = 0;
        int incrementedField = Calendar.DAY_OF_MONTH; //start by incrementing a day at a time
        while (true) {
            if (cal.get(Calendar.DAY_OF_WEEK) == dayToMatch) {
                count++;
                if (count == occurrence) {
                    // we found the occurrence
                    break;
                } else {
                    // we found the day, just not the occurrence
                    // - now we can increment a week at a time
                    if (count == 1) incrementedField = Calendar.WEEK_OF_MONTH;
                }
            }
            // increment to next day/week
            cal.add(incrementedField, increment);
        }

        return cal;
    }

}
