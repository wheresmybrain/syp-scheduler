package com.wheresmybrain.syp.scheduler.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Useful utility methods related to dates and times.
 *
 * @author @author <a href="mailto:chris.mcfarland@gmail.com">Chris McFarland</a>
 */
public class TimeUtils {

    private static Logger log = LoggerFactory.getLogger(TimeUtils.class);

    private static final SimpleDateFormat timeOnlyFormatter = new SimpleDateFormat("h:mm a");

    // time interval values (expressed in milliseconds)
    private static final long MILLIS_IN_A_DAY = 1000 * 60 * 60 * 24;
    private static final long MILLIS_IN_A_WEEK = 1000 * 60 * 60 * 24 * 7;
    private static final long MILLIS_IN_AN_HOUR = 60 * 60 * 1000;
    private static final long MILLIS_IN_A_MINUTE = 60 * 1000;
    private static final long MILLIS_IN_A_SECOND = 1000;

    /**
     * Returns a String description of a time interval in terms of
     * hours, minutes, seconds and milliseconds.
     *
     * @param startTime the beginning of the interval
     * @param endTime the end of the interval
     */
    public static String getIntervalDescription(Date startTime, Date endTime) {
        long milliseconds = endTime.getTime() - startTime.getTime();
        return getTimeDescription(milliseconds, TimeUnit.MILLISECONDS);
    }

    /**
     * Returns a String description of a time duration in terms of
     * hours, minutes, seconds and milliseconds.
     *
     * @param duration time duration for which to return a text description
     * @param unit the units of the passed duration
     */
    public static String getTimeDescription(long duration, TimeUnit unit) {

        long milliseconds = unit.convert(duration, TimeUnit.MILLISECONDS);

        StringBuilder sb = new StringBuilder();

        boolean inPast = false;
        if (milliseconds < 0) {
            inPast = true;
            sb.append("-(");
        }

        int hours = (int) Math.floor(milliseconds/ MILLIS_IN_AN_HOUR);
        if (hours > 0) {
            if (hours == 1) {
                sb.append(hours).append(" hour");
            } else {
                sb.append(hours).append(" hours");
            }
            // update
            milliseconds = milliseconds - (hours * MILLIS_IN_AN_HOUR);
        }

        int minutes = (int) Math.floor(milliseconds/ MILLIS_IN_A_MINUTE);
        if (minutes > 0) {
            if (sb.length() > 0) sb.append(", ");
            if (minutes == 1) {
                sb.append(minutes).append(" minute");
            } else {
                sb.append(minutes).append(" minutes");
            }
            // update
            milliseconds = milliseconds - (minutes * MILLIS_IN_A_MINUTE);
        }

        int seconds = (int) Math.floor(milliseconds/ MILLIS_IN_A_SECOND);
        if (seconds > 0) {
            if (sb.length() > 0) sb.append(", ");
            if (seconds == 1) {
                sb.append(seconds).append(" second");
            } else {
                sb.append(seconds).append(" seconds");
            }
            // update
            milliseconds = milliseconds - (seconds * MILLIS_IN_A_SECOND);
        }

        if (milliseconds > 0) {
            if (sb.length() > 0) sb.append(", ");
            if (seconds == 1) {
                sb.append(milliseconds).append(" millisecond");
            } else {
                sb.append(milliseconds).append(" milliseconds");
            }
        }

        if (inPast) {
            sb.append(")");
        }

        return sb.toString();
    }

    /**
     * Returns the number of full minutes between two times, rounded down
     * to the minute. For example, if two times are 55 seconds apart, then this
     * method would return a zero minute interval between them, because a full
     * minute did not pass.
     *
     * @throws IllegalArgumentException if either time is null, or if startTime
     *   is after endTime.
     */
    public static int getIntervalInMins(Date startTime, Date endTime) {

        if (startTime == null) {
            throw new IllegalArgumentException("startTime is null.");
        } else if (endTime == null) {
            throw new IllegalArgumentException("endTime is null.");
        } else if (startTime.after(endTime)) {
            String start = getTimeOnlyString(startTime);
            String end = getTimeOnlyString(endTime);
            throw new IllegalArgumentException("startTime("+start+") is after endTime ("+end+".");
        }

        return (int) Math.round((endTime.getTime() - startTime.getTime()) / 60000);
    }

    /**
     * Returns the number of days between the beginDate to the endDate. For instance, there
     * is a one day interval between today and tomorrow. This method performs calculations
     * to the day, so that a time just before midnight and a time just after midnight are
     * considered one day apart because they occur in different days.
     *
     * @param beginDate any Date object.
     * @param endDate any Date object.
     * @return endDate - beginDate (in days). This could be negative if endDate is before beginDate.
     * @throws IllegalArgumentException if either Date is null.
     */
    public static int getIntervalInDays(Date beginDate, Date endDate) {

        // check for errors
        StringBuffer buf = null;
        if (beginDate == null) {
            buf = new StringBuffer("error: beginDate is null");
        }
        if (endDate == null) {
            if (buf == null) {
                buf = new StringBuffer("error: endDate is null");
            } else {
                buf.append(", endDate is null");
            }
        }
        if (buf != null) throw new IllegalArgumentException(buf.toString());

        // round the result to factor out losing or gaining a second
        // for switches to/from Daylight Savings Time
        TruncatedDate t1 = new TruncatedDate(beginDate).truncate(TruncatedDate.TruncateLevel.DAY);
        TruncatedDate t2 = new TruncatedDate(endDate).truncate(TruncatedDate.TruncateLevel.DAY);
        float diffInDays = ((float)(t2.getTime() - t1.getTime())) / MILLIS_IN_A_DAY;
        return (int) Math.round(diffInDays);
    }

    /**
     * Returns the number of weeks between the beginDate to the endDate. This method performs
     * calculations to the week, so that a day just before the end of the week and a day just
     * after the end of the week are considered one week apart because they occur in different
     * weeks.
     * <p/>
     * This method does not work for calculating UI Benefits weeks. Use the {@link
     * #getIntervalInBenefitWeeks(Date, Date)} method to calculate benefits weeks.
     *
     * @param beginDate any Date object.
     * @param endDate any Date object.
     * @return endDate - beginDate (in weeks). This could be negative if endDate is in a week
     *   before the beginDate week.
     * @throws IllegalArgumentException if either Date is null.
     */
    public static int getIntervalInWeeks(Date beginDate, Date endDate) {

        // check for errors
        StringBuffer buf = null;
        if (beginDate == null) {
            buf = new StringBuffer("error: beginDate is null");
        }
        if (endDate == null) {
            if (buf == null) {
                buf = new StringBuffer("error: endDate is null");
            } else {
                buf.append(", endDate is null");
            }
        }
        if (buf != null) throw new IllegalArgumentException(buf.toString());

        // round the result to factor out losing or gaining a second
        // for switches to/from Daylight Savings Time
        TruncatedDate t1 = new TruncatedDate(beginDate).truncate(TruncatedDate.TruncateLevel.WEEK);
        TruncatedDate t2 = new TruncatedDate(endDate).truncate(TruncatedDate.TruncateLevel.WEEK);
        float diffInWeeks = ((float)(t2.getTime() - t1.getTime())) / MILLIS_IN_A_WEEK;
        return (int) Math.round(diffInWeeks);
    }

    /**
     * Returns the number of <b>UI benefits</b> weeks from the periodBeginDate to the periodEndDate.
     * The periodBeginDate must be a Sunday and the periodEndDate must be a Saturday, such that if
     * the specified periodEndDate is the Saturday on the following week after the periodBeginDate,
     * then the returned interval in weeks will be 2 (i.e. one benefit period). The implication
     * is that periodBeginDate is the very beginning of the day Sunday, and the periodEndDate is
     * the very end of the day Saturday, so that Sunday to Saturday of the same week is considered
     * one full week. This differs from the {@link #getIntervalInWeeks(Date, Date)} method in this
     * class, which considers a full week as being the beginning of the day Sunday to the beginning
     * of the day of the following Sunday.
     *
     * @param periodBeginDate java.util.Date object for a benefits period begin date. This must
     *   be a Sunday, but any time during that day will work the same.
     * @param periodEndDate java.util.Date object for a benefits period end date. This must
     *   be a Saturday, but any time during that day will work the same. The other requirement
     *   is that the periodEndDate must be after periodBeginDate.
     * @return the number of UI benefits weeks from the periodBeginDate to the periodEndDate.
     * @throws IllegalArgumentException if periodBeginDate is not a Sunday, or periodEndDate
     *   is not a Saturday, or periodBeginDate is after periodEndDate.
     */
    public static int getIntervalInBenefitWeeks(Date periodBeginDate, Date periodEndDate) {

        // check for errors
        StringBuffer buf = null;
        if (periodBeginDate == null) {
            buf = new StringBuffer("error: periodBeginDate is null");
        }
        if (periodEndDate == null) {
            if (buf == null) {
                buf = new StringBuffer("error: periodEndDate is null");
            } else {
                buf.append(", periodEndDate is null");
            }
        }
        if (buf != null) throw new IllegalArgumentException(buf.toString());

        // begin must be Sunday, end must be Saturday, and begin must occur before end
        Calendar cal = Calendar.getInstance();
        cal.setTime(periodBeginDate);
        if (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            String msg = "periodBeginDate must be a Sunday: "+periodBeginDate;
            throw new IllegalArgumentException(msg);
        }
        cal.setTime(periodEndDate);
        if (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
            String msg = "periodEndDate must be a Saturday: "+periodEndDate;
            throw new IllegalArgumentException(msg);
        }
        if (periodBeginDate.after(periodEndDate)) {
            String msg = "periodBeginDate ("+periodBeginDate+") must be before periodEndDate ("+periodEndDate+")";
            throw new IllegalArgumentException(msg);
        }

        // add a day to periodEndDate to finish the week (and account
        // for the difference in the definition of a benefits week)
        periodEndDate = addDays(periodEndDate, 1);

        return getIntervalInWeeks(periodBeginDate, periodEndDate);
    }

    /**
     * Returns the number of months between the beginDate to the endDate. This method performs
     * calculations to the month, so that a day just before the end of the month and a day just
     * after the end of the month are considered one month apart because they occur in different
     * months.
     *
     * @param beginDate any Date object.
     * @param endDate any Date object.
     * @return endDate - beginDate (in months). This could be negative if endDate is in a month
     *   before the beginDate month.
     * @throws IllegalArgumentException if either Date is null.
     */
    public static int getIntervalInMonths(Date beginDate, Date endDate) {

        // check for errors
        StringBuffer buf = null;
        if (beginDate == null) {
            buf = new StringBuffer("error: beginDate is null");
        }
        if (endDate == null) {
            if (buf == null) {
                buf = new StringBuffer("error: endDate is null");
            } else {
                buf.append(", endDate is null");
            }
        }
        if (buf != null) throw new IllegalArgumentException(buf.toString());

        int MAX_MONTHS = Integer.MAX_VALUE;

        // truncate the dates to compare by month
        // use Calendar instances to manipulate the (truncated) dates
        Calendar c1 = Calendar.getInstance();
        c1.setTime(new TruncatedDate(beginDate).truncate(TruncatedDate.TruncateLevel.MONTH));
        Calendar c2 = Calendar.getInstance();
        c2.setTime(new TruncatedDate(endDate).truncate(TruncatedDate.TruncateLevel.MONTH));
        if (c1.compareTo(c2) == 0) {
            // beginDate is in same month as endDate
            return 0;
        } else if (c1.compareTo(c2) < 0) {
            // beginDate month occurs before endDate month
            for (int count = 1; count<MAX_MONTHS; count++) {
                c1.add(Calendar.MONTH, 1);
                log.debug("Comparing dates: "+c1.getTime()+" TO "+c2.getTime());
                if (c1.compareTo(c2) == 0) {
                    return count;
                }
            }
        } else {
            // beginDate month occurs after endDate month
            for (int count = -1; count>(-MAX_MONTHS); count--) {
                c1.add(Calendar.MONTH, -1);
                log.debug("Comparing dates: "+c1.getTime()+" TO "+c2.getTime());
                if (c1.compareTo(c2) == 0) {
                    return count;
                }
            }
        }
        // if get to here, then we reached MAX_MONTHS, which should never happen
        String msg = "error getting Month interval from beginDate ("+beginDate+") to endDate ("+endDate+")";
        throw new IllegalStateException(msg);
    }

    /**
     * Returns the result of adding the specified number of days to the specified beginDate.
     * The returned date is truncated to the day (all time information removed), so that calling
     * this method to add one day to a Date object from this morning, and adding one day to a
     * Date object from this afternoon, will return equivilent Date objects (date1.equals(date2)),
     * both representing tomorrow.
     *
     * @param beginDate the starting date before adding any days.
     * @param daysToAdd the number of days to add (or subtract if less than zero) to the beginDate.
     * @return Date object truncated to the day, so that even adding 0 days to today will
     *   return today's date with all time info (hours, minutes, etc.) removed.
     */
    public static Date addDays(Date beginDate, int daysToAdd) {

        if (beginDate == null) {
            throw new IllegalArgumentException("error: beginDate is null");
        }

        TruncatedDate truncated = new TruncatedDate(beginDate).truncate(TruncatedDate.TruncateLevel.DAY);
        Calendar cal = Calendar.getInstance();
        cal.setTime(truncated);
        cal.add(Calendar.DATE, daysToAdd);

        return cal.getTime();
    }

    /**
     * Returns the result of adding the specified number of weeks to the specified beginDate.
     * The returned date is truncated to the day (all time information removed), so that calling
     * this method to add one week to 2 Date objects from 2 different times today will yield
     * equivilent Date objects (date1.equals(date2)) representing one week from today.
     *
     * @param beginDate the starting date before adding any weeks.
     * @param weeksToAdd the number of weeks to add (or subtract if less than zero) to the beginDate.
     * @return Date object truncated to the day, so that even adding 0 weeks to today will
     *   return today's date with all time info (hours, minutes, etc.) removed.
     */
    public static Date addWeeks(Date beginDate, int weeksToAdd) {

        if (beginDate == null) {
            throw new IllegalArgumentException("error: beginDate is null");
        }

        TruncatedDate truncated = new TruncatedDate(beginDate).truncate(TruncatedDate.TruncateLevel.DAY);
        Calendar cal = Calendar.getInstance();
        cal.setTime(truncated);
        cal.add(Calendar.WEEK_OF_YEAR, weeksToAdd);

        return cal.getTime();
    }

    /**
     * Returns the result of adding the specified number of months to the specified beginDate.
     * The returned date is truncated to the day (all time information removed), so that calling
     * this method to add one month to 2 Date objects from 2 different times today will yield
     * equivilent Date objects (date1.equals(date2)) representing one month from today.
     * <p/>
     * THIS METHOD SELF-ADJUSTS when adding a month takes the date outside the next month.
     * For instance, if the date is March 31st, then the raw process of adding a month would
     * result in the date April 31st, which would actually equate to May 1st. <b>Using this method,
     * adding a month to March 31st will result in April 30th</b>.
     *
     * @param beginDate the starting date before adding any weeks.
     * @param monthsToAdd the number of month to add (or subtract if less than zero) to the beginDate.
     * @return Date object truncated to the day, so that even adding 0 months to today will
     *   return today's date with all time info (hours, minutes, etc.) removed.
     */
    public static Date addMonths(Date beginDate, int monthsToAdd) {

        if (beginDate == null) {
            throw new IllegalArgumentException("error: beginDate is null");
        }

        // first get the day of month for beginDate
        Calendar cal = Calendar.getInstance();
        cal.setTime(beginDate);
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        // now get first day of beginDate's month and add months to that
        Date firstDayOfMonth = getFirstDayOfMonth(beginDate);
        cal.setTime(firstDayOfMonth);
        cal.add(Calendar.MONTH, monthsToAdd);
        // set the day in new month
        if (dayOfMonth <= 28) {
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        } else {
            // check for possible rollover past
            // the end of the month
            int lastDateOfMonth = getLastDateOfMonth(cal.getTime());
            int dateToSet = (dayOfMonth <= lastDateOfMonth) ? dayOfMonth : lastDateOfMonth;
            cal.set(Calendar.DAY_OF_MONTH, dateToSet);
        }

        return cal.getTime();
    }

    /**
     * Returns the first day (Sunday) of the week that the
     * specified date falls under. The time is the very beginning
     * of the day (hours, minutes, seconds, millis zeroed out).
     */
    public static Date getFirstDayOfWeek(Date date) {
        TruncatedDate truncated = new TruncatedDate(date).truncate(TruncatedDate.TruncateLevel.WEEK);
        return truncated;
    }

    /**
     * Returns the last day (Saturday) of the week that the
     * specified date falls under. The time is the very beginning
     * of the day (hours, minutes, seconds, millis zeroed out).
     */
    public static Date getLastDayOfWeek(Date date) {
        TruncatedDate truncated = new TruncatedDate(date).truncate(TruncatedDate.TruncateLevel.WEEK);
        Calendar cal = Calendar.getInstance();
        cal.setTime(truncated);
        cal.add(Calendar.WEEK_OF_YEAR, 1);
        cal.add(Calendar.DATE, -1);
        return cal.getTime();
    }

    /**
     * Returns the first day of the month that the specified
     * date falls under. The time is the very beginning of the
     * day (hours, minutes, seconds, millis zeroed out).
     */
    public static Date getFirstDayOfMonth(Date date) {
        TruncatedDate truncated = new TruncatedDate(date).truncate(TruncatedDate.TruncateLevel.MONTH);
        return truncated;
    }

    /**
     * Returns the last day of the month that the specified
     * date falls under. The time is the very beginning of the
     * day (hours, minutes, seconds, millis zeroed out).
     */
    public static Date getLastDayOfMonth(Date date) {
        TruncatedDate truncated = new TruncatedDate(date).truncate(TruncatedDate.TruncateLevel.MONTH);
        Calendar cal = Calendar.getInstance();
        cal.setTime(truncated);
        cal.add(Calendar.MONTH, 1);
        cal.add(Calendar.DATE, -1);
        return cal.getTime();
    }

    /*
     * Returns the date of the last day of the month (28, 29, 30, 31)
     */
    private static int getLastDateOfMonth(Date d) {
        Date date = getLastDayOfMonth(d);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * Returns time String formatted like: "h:mm a"
     */
    private static String getTimeOnlyString(Date date) {
        return timeOnlyFormatter.format(date);
    }

}
