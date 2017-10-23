package com.wheresmybrain.syp.scheduler.utils;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 *  This class makes comparing two dates by year, month, week, day, hour, minute
 *  and second possible. This class is the same as java.util.Date but its value
 *  is truncated on different levels of resolution to make comparing dates/times
 *  across a wide array of applications easier. This class has many other uses in
 *  addition to date/time comparisons - essentially any application involving the
 *  manipulation of dates and times benefit from TruncatedDate.
 *  <p>
 *  After truncation, the original date is retained internally, but
 *  the TruncatedDate object's date value represents the truncated value. Any truncate
 *  action operates on the original date, not the current setting, so using a date
 *  already reduced to month and truncating it to hour does not result in any loss
 *  of information.
 *  <p>
 *  As one example, truncating a date to the MONTH results in a Date object with
 *  "zeroed-out" information in the millisecond, second, minute, hour of day, day
 *  of month and week of month fields. Two <i>TruncatedDate</i> objects from
 *  different days in March 2015 are equivalent if both are truncated on MONTH.
 *  This class uses the following enum constants:<br>
 *  <b>YEAR</b> - truncating to year sets date to beginning of 1st day of the current
 *  year so that two different dates in the same year will be equivalent.<br>
 *  <b>MONTH</b> - truncating to month sets date to beginning of 1st day of the current
 *  month so that two different dates in the same month will be equivalent.<br>
 *  <b>WEEK</b> - truncating to week sets date to the beginning of the first day of
 *  the week for that date, so that two different days in the same week will be
 *  equivalent. <b>NOTE 1</b>: the first day of the week is locale-specific
 *  (e.g. Sunday is first day of week in US) and truncating to week automatically honors
 *  the first day according to the default locale. <b>NOTE 2</b>: different days in the
 *  first and last weeks of the month can be in different months, and different days in
 *  the first and last weeks of the year can be in different years, but after truncation
 *  all days in the week will become part of the month or year associated with the first
 *  day of the week.<br>
 *  <b>DAY</b> - truncating to day sets time to beginning of the current day for that
 *  date so that two different times in the same day will be equivalent.<br>
 *  <b>HOUR</b> - truncating to hour sets time to beginning of the current hour for the
 *  date, so that two different times in the same hour will be equivalent.<br>
 *  <b>MINUTE</b> - truncating to minute sets time to beginning of the current minute
 *  for that date so that two different times in the same minute will be equivalent.<br>
 *  <b>SECOND</b> - truncating to second sets time to beginning of the current second
 *  for that date, so that two different times in the same second will be equivalent.<br>
 *  <b>MILLISECOND</b> - truncating to millisecond is the same as resetting the
 *  truncatedDate (to the original baseDate).
 *  <p>
 *  <b>USAGE NOTES</b><br>
 *  *
 *
 * @author <a href="mailto:chris.mcfarland@gmail.com">Chris McFarland</a>
 */
public class TruncatedDate extends Date {

    private static final long serialVersionUID = 1L;

    /*
     * Defines the truncate options
     */
    public static enum TruncateLevel {
        YEAR,       //for comparing dates by year
        MONTH,      //for comparing dates by month
        WEEK,       //for comparing dates by week
        DAY,        //for comparing dates by day
        HOUR,       //for comparing dates by hour
        MINUTE,     //for comparing dates by minute
        SECOND,     //for comparing dates by second
        MILLISECOND //same as resetting to the original baseDate
    }

    private Date baseDate;
    private Date truncatedDate;
    private TruncateLevel truncateLevel = TruncateLevel.MILLISECOND;

    /** initializes to now */
    public TruncatedDate() {
        this.baseDate = new Date();
        this.truncatedDate = this.baseDate;
    }

    /** initializes to milliseconds since Jan. 1, 1970 */
    public TruncatedDate(long milliseconds) {
        this.baseDate = new Date(milliseconds);
        this.truncatedDate = this.baseDate;
    }

    /** initializes to date represented by specified Date */
    public TruncatedDate(Date date) {
        this.baseDate = (Date)date.clone();
        this.truncatedDate = this.baseDate;
    }

    //-- public

    /**
     * Truncates this date object to the specified resolution. See the
     * class javadoc for a more detailed explanation.
     * <p>
     * Invalid truncate levels throw runtime exception.
     *
     * @param truncateLevel enum representing the level to trim the date to,
     * such that two non-equivalent dates prior to truncation will be considered
     * equal after truncation if they are inside the same time block at the new
     * truncation level. For example, two dates from different days in March 2015
     * will become equivalent if they are both truncated to MONTH, and two times
     * in the same day will become equivalent after they are truncated on DAY.
     * @return this object so the method can be chained like:
     *       <code>new TruncatedDate().truncate(DAY)</code>
     */
    public TruncatedDate truncate(TruncateLevel truncateLevel) {
        this.truncateLevel = truncateLevel;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(this.baseDate);
        switch(truncateLevel) {
            case YEAR:
                // truncating to year sets date to beginning of 1st day of the current year
                calendar.set(Calendar.MONTH, Calendar.JANUARY);
                calendar.set(Calendar.WEEK_OF_YEAR, 1);
            case MONTH:
                // truncating to month sets date to beginning of 1st day of the current month
                calendar.set(Calendar.WEEK_OF_MONTH, 1);
                calendar.set(Calendar.DAY_OF_MONTH, 1);
            case DAY:
                // truncating to day sets time to beginning of the current day
                calendar.set(Calendar.HOUR_OF_DAY, 0);
            case HOUR:
                // truncating to hour sets time to beginning of the current hour
                calendar.set(Calendar.MINUTE, 0);
            case MINUTE:
                // truncating to minute sets time to the beginning of the current minute
                calendar.set(Calendar.SECOND, 0);
            case SECOND:
                // truncating to minute sets time to the beginning of the current second
                calendar.set(Calendar.MILLISECOND, 0);
                this.truncatedDate = calendar.getTime();
                this.truncateLevel = truncateLevel;
                break;
            case WEEK:
                // truncating to week sets date to beginning of the first day of the current week
                calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                this.truncatedDate = calendar.getTime();
                this.truncateLevel = truncateLevel;
                break;
            case MILLISECOND:
                // truncating to millisecond resets the truncatedDate to the baseDate
                this.truncatedDate = this.baseDate;
                this.truncateLevel = truncateLevel;
                break;
            default:
                //invalid cases will throw developer exception
                StringBuffer msg = new StringBuffer("Invalid 'truncateLevel' value: (");
                msg.append(truncateLevel).append(")");
                throw new IllegalArgumentException(msg.toString());
        }

        return this;
    }

    /**
     * Removes the truncation level and resets truncated
     * date to equal the original date.
     */
    public void removeTruncation() {
        this.truncate(TruncateLevel.MILLISECOND);
    }

    /**
     *  Changes the date that the TruncateDate
     *  object is based on, but retains the current
     *  truncation level.
     */
    public void changeBaseDate(Date date) {
        this.baseDate = (Date)date.clone();
        this.truncate(this.truncateLevel);
    }

    /**
     *  Returns a pure Date object representing the
     *  truncated date. The returned object is no
     *  longer a TruncatedDate object.
     */
    public Date getTruncatedDate() {
        return (Date)this.truncatedDate.clone();
    }

    /**
     * Returns enum corresponding to level of truncation.
     */
    public TruncateLevel getTruncateLevel() {
        return this.truncateLevel;
    }

    //---------- methods overriding superclass Date ----------

    /**
     * Returns milliseconds since 1/1/1970 for the truncated value.
     */
    public long getTime() {
        return this.truncatedDate.getTime();
    }

    /**
     * @see java.util.Date#after(Date)
     */
    public boolean after(Date when) {
        return (this.compareTo(when) > 0);
    }

    /**
     * @see java.util.Date#before(Date)
     */
    public boolean before(Date when) {
        return (this.compareTo(when) < 0);
    }

    /**
     * Overrides {@link Date#compareTo(Date)} to compare with other
     * dates at the current truncation level. As a convenience, the
     * other Date does not have to be a TruncatedDate, as this method
     * performs the truncation internally, according to this object's
     * current truncation setting.
     * <p>
     * On the other hand, this method is not useful for sorting
     * ({@link java.util.Collections#sort(java.util.List)} unless the
     * other objects are also TruncateDate type.
     */
    public int compareTo(Date otherDate) {
        long thisMillis = this.truncatedDate.getTime();
        TruncatedDate otherTruncated = (otherDate instanceof TruncatedDate) ?
                (TruncatedDate) otherDate : new TruncatedDate().truncate(this.getTruncateLevel());
        long otherMillis = otherTruncated.getTime();
        long result = thisMillis - otherMillis;
        int comparison = 0;
        if (result > 0) {
            comparison = 1;
        } else if (result < 0) {
            comparison = -1;
        }
        return comparison;
    }

    /**
     * Overrides {@link Date#equals(Object)} to use the truncated value
     * for comparison. Unlike {@link #compareTo(Date)} in this class,
     * does not truncate Date object being compared to. Works best
     * when comparing with other TruncatedDate objects!
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof Date)) return false;
        return this.truncatedDate.equals((Date)obj);
    }

    /**
     * Returns truncated date value formatted to current locale.
     */
    public String toString() {
        return DateFormat.getDateInstance().format(this.truncatedDate);
    }

}
