package com.wheresmybrain.syp.scheduler.utils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Useful utility methods related to dates and times.
 *
 * @author @author <a href="mailto:chris.mcfarland@gmail.com">Chris McFarland</a>
 */
public class TimeUtils {

    // time interval values (expressed in milliseconds)
    private static final long HOUR = 60 * 60 * 1000;
    private static final long MINUTE = 60 * 1000;
    private static final long SECOND = 1000;

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

        int hours = (int) Math.floor(milliseconds/HOUR);
        if (hours > 0) {
            if (hours == 1) {
                sb.append(hours).append(" hour");
            } else {
                sb.append(hours).append(" hours");
            }
            // update
            milliseconds = milliseconds - (hours * HOUR);
        }

        int minutes = (int) Math.floor(milliseconds/MINUTE);
        if (minutes > 0) {
            if (sb.length() > 0) sb.append(", ");
            if (minutes == 1) {
                sb.append(minutes).append(" minute");
            } else {
                sb.append(minutes).append(" minutes");
            }
            // update
            milliseconds = milliseconds - (minutes * MINUTE);
        }

        int seconds = (int) Math.floor(milliseconds/SECOND);
        if (seconds > 0) {
            if (sb.length() > 0) sb.append(", ");
            if (seconds == 1) {
                sb.append(seconds).append(" second");
            } else {
                sb.append(seconds).append(" seconds");
            }
            // update
            milliseconds = milliseconds - (seconds * SECOND);
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

}
