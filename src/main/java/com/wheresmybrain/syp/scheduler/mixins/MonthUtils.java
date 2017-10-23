package com.wheresmybrain.syp.scheduler.mixins;

import com.wheresmybrain.syp.scheduler.TaskScheduler;
import com.wheresmybrain.syp.scheduler.utils.TimeUtils;

import java.util.Calendar;
import java.util.Date;

/**
 * These utility methods are related to Month calculations.
 */
public class MonthUtils {

    /**
     * Returns the numeric date of the last day of the month (28, 29, 30, 31)
     * that the specified Date object falls under.
     *
     * @param date any Date in the month in which find the last day
     */
    public static int getLastDateOfMonth(Date date) {
        Date last = TimeUtils.getLastDayOfMonth(date);
        Calendar cal = Calendar.getInstance();
        cal.setTime(last);
        return cal.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * The 'dayOfMonth' parameter for some <i>Mixins</i> can be entered as
     * either a numeric date, or a date relative to {@link TaskScheduler#LAST_DAY_OF_MONTH}.
     * This method returns the actual numeric date corresponding to whichever 'dayOfMonth'
     * technique is used.
     *
     * @param specifiedDayOfMonth the 'dayOfMonth' passed into the Mixin. Cannot be zero.
     * @param date any Date object in the month to return a numeric date for
     * @return the actual numeric day-of-month corresponding to the 'dayOfMonth' value
     *   passed into a Mixin
     */
    public static int calculateDayOfMonth(int specifiedDayOfMonth, Date date) {
        if (specifiedDayOfMonth == 0) {
            throw new IllegalArgumentException("invalid dayOfMonth: "+specifiedDayOfMonth);
        }
        int dateToSet = -1;
        if (specifiedDayOfMonth > 0) {
            if (specifiedDayOfMonth <= 28) {
                dateToSet = specifiedDayOfMonth;
            } else {
                int lastDateOfMonth = MonthUtils.getLastDateOfMonth(date);
                dateToSet = (specifiedDayOfMonth <= lastDateOfMonth) ? specifiedDayOfMonth : lastDateOfMonth;
            }
        } else if (specifiedDayOfMonth == TaskScheduler.LAST_DAY_OF_MONTH) {
            dateToSet = MonthUtils.getLastDateOfMonth(date);
        } else {
            // day of month is offset from last day of month
            int lastDateOfMonth = MonthUtils.getLastDateOfMonth(date);
            int offset = TaskScheduler.LAST_DAY_OF_MONTH - specifiedDayOfMonth;
            dateToSet = lastDateOfMonth - offset;
            if (dateToSet < 1) dateToSet = 1;
        }
        return dateToSet;
    }

}
