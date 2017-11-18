package com.wheresmybrain.syp.scheduler.config;

import com.wheresmybrain.syp.scheduler.ScheduledTask;
import com.wheresmybrain.syp.scheduler.TaskScheduler;
import com.wheresmybrain.syp.scheduler.enums.DayOccurrence;
import com.wheresmybrain.syp.scheduler.enums.DayOfWeek;
import com.wheresmybrain.syp.scheduler.enums.MonthOfYear;
import com.wheresmybrain.syp.scheduler.iTask;

import java.util.Arrays;

/**
 * This class represents the &lt;task&gt; tag in the scheduler-config.xml and is
 * used as a throw-away class for parsing the xml file by Commons Digester.
 * <p/>
 * The parent ({@link SchedulerConfig}) class will call the {@link #validate(ClassLoader)}
 * method on this class to catch configuration errors before adding the associated task.
 */
public class TaskConfig {

    private iTask task;
    private String className;
    private String interval;
    private int year;
    private String monthOfYear;
    /* dayOfMonth includes patterns like: "1", "31", "LAST_DAY_OF_MONTH - n" */
    private String dayOfMonth;
    private int dayOfMonthInt;
    private String dayOfWeek;
    private String dayOccurrence;
    private int hours;
    private int minutes;
    private int seconds;
    private int milliseconds;
    private long initialDelayInMillis = -1; //default to not have an initial delay

    /**
     * Returns task instance if the scheduler-config file (optionally) declared
     * custom attributes on the task, or returns null if it did not. If custom
     * attributes are declared, then the parser will automatically instantiate
     * the task to set them.
     */
    public iTask getTask() {
        return this.task;
    }

    /**
     * Sets the instantiated task on this config if the scheduler-config file
     * (optionally) declared custom attributes on the task. If custom
     * attributes are declared, then the parser will automatically instantiate
     * the task to set them.
     */
    public void setTask(iTask task) {
        this.task = task;
    }

    /**
     * classname for scheduled task
     */
    public String getClassName() {
        return className;
    }

    /**
     * classname for scheduled task
     */
    public void setClassName(String string) {
        className = string;
    }

    /**
     * interval = {"ONE_TIME", "HOURS", "MINUTES", "SECONDS", "MILLISECONDS", "DAILY", "WEEKLY", "MONTHLY", "YEARLY"}
     */
    public String getInterval() {
        return interval;
    }

    /**
     * interval = {"ONE_TIME", "HOURS", "MINUTES", "SECONDS", "MILLISECONDS", "DAILY", "WEEKLY", "MONTHLY", "YEARLY"}
     */
    public void setInterval(String interval) {
        this.interval = interval;
    }

    /**
     * year = 4-digit int
     */
    public int getYear() {
        return year;
    }

    /**
     * year = 4-digit int
     */
    public void setYear(int year) {
        this.year = year;
    }

    /**
     * monthOfYear = {"JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE", "JULY", "AUGUST",
     *   "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"}
     */
    public String getMonthOfYear() {
        return monthOfYear;
    }

    /**
     * monthOfYear = {"JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE", "JULY", "AUGUST",
     *   "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"}
     */
    public void setMonthOfYear(String month) {
        this.monthOfYear = month;
    }

    /**
     * <li>dayOfMonth = 1-31 (if specified dayOfMonth > monthOfYear's last day, then monthOfYear's last day is used)</li>
     * <li>TaskScheduler.LAST_DAY_OF_MONTH (can specify: "LAST_DAY_OF_MONTH - n" for dates relative to last day)
     */
    public String getDayOfMonth() {
        return dayOfMonth;
    }

    /**
     * <li>dayOfMonth = 1-31 (if specified dayOfMonth > monthOfYear's last day, then monthOfYear's last day is used)</li>
     * <li>TaskScheduler.LAST_DAY_OF_MONTH (can specify: "LAST_DAY_OF_MONTH - n" for dates relative to last day)
     */
    public int getDayOfMonthInt() {
        return dayOfMonthInt;
    }

    /**
     * <li>dayOfMonth = 1-31 (if specified dayOfMonth > monthOfYear's last day, then monthOfYear's last day is used)</li>
     * <li>TaskScheduler.LAST_DAY_OF_MONTH (can specify: "LAST_DAY_OF_MONTH - n" for dates relative to last day)
     */
    public void setDayOfMonth(String dayOfMonthString) {
        this.dayOfMonth = dayOfMonthString;
        dayOfMonthString = dayOfMonthString.trim();
        int index = dayOfMonthString.indexOf("LAST_DAY_OF_MONTH");
        if (index == 0) {
            if (dayOfMonthString.length() > 17) {
                String remainder = dayOfMonthString.substring(17, dayOfMonthString.length());
                int adjustment = this.extractEndOfMonthAdjustment(remainder);
                this.dayOfMonthInt = TaskScheduler.LAST_DAY_OF_MONTH - adjustment;
            } else {
                this.dayOfMonthInt = TaskScheduler.LAST_DAY_OF_MONTH;
            }
        } else {
            try {
                this.dayOfMonthInt = Integer.parseInt(dayOfMonthString);
            } catch (NumberFormatException ex) {
                throw new SchedulerConfigException("dayOfMonth not numeric: "+dayOfMonthString)
                        .setCurrentState(this.toString());
            }
        }
    }

    private int extractEndOfMonthAdjustment(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c >= '0' && c <= '9') {
                sb.append(c);
            } else {
                if (c != '-' && !Character.isWhitespace(c)) {
                    throw new SchedulerConfigException("illegal LAST_DAY_OF_MONTH String: "+s)
                            .setCurrentState(this.toString());
                }
            }
        }
        try {
            return Integer.parseInt(sb.toString());
        } catch (NumberFormatException ex) {
            // error - expecting another digit
            throw new SchedulerConfigException("illegal LAST_DAY_OF_MONTH String: "+s)
                    .setCurrentState(this.toString());
        }
    }

    /**
     * dayOfWeek = {"SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"}
     */
    public String getDayOfWeek() {
        return dayOfWeek;
    }

    /**
     * dayOfWeek = {"SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"}
     */
    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    /**
     * dayOccurrence = {"FIRST", "SECOND", "THIRD", "FOURTH", "LAST"}
     */
    public String getDayOccurrence() {
        return dayOccurrence;
    }

    /**
     * dayOccurrence = {"FIRST", "SECOND", "THIRD", "FOURTH", "LAST"}
     */
    public void setDayOccurrence(String dayOccurrence) {
        this.dayOccurrence = dayOccurrence;
    }

    /**
     * initialDelayInMillis = delay before first execution
     */
    public long getInitialDelayInMillis() {
        return initialDelayInMillis;
    }

    /**
     * initialDelayInMillis = delay before first execution
     */
    public void setInitialDelayInMillis(long initialDelayInMillis) {
        this.initialDelayInMillis = initialDelayInMillis;
    }

    /**
     * hours = 0-23
     */
    public int getHours() {
        return hours;
    }

    /**
     * hours = 0-23
     */
    public void setHours(int hour) {
        this.hours = hour;
    }

    /**
     * minutes = 0-59
     */
    public int getMinutes() {
        return minutes;
    }

    /**
     * minutes = 0-59
     */
    public void setMinutes(int minute) {
        this.minutes = minute;
    }

    /**
     * seconds = 0-59
     */
    public int getSeconds() {
        return seconds;
    }

    /**
     * seconds = 0-59
     */
    public void setSeconds(int second) {
        this.seconds = second;
    }

    /**
     * milliseconds = 0-999
     */
    public int getMilliseconds() {
        return milliseconds;
    }

    /**
     * milliseconds = 0-999
     */
    public void setMilliseconds(int millisecond) {
        this.milliseconds = millisecond;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        if (task!=null) {
            String[] debugState = task.getDebugState();
            if (debugState!=null) sb.append("task state:").append(Arrays.toString(debugState)).append(',');
        }
        sb.append("className:").append((className!=null)?className:"null").append(',');
        sb.append("interval:").append((interval!=null)?interval:"null");
        if (year > 0) sb.append(',').append("year:").append(year);
        if (monthOfYear!=null) sb.append(',').append("monthOfYear:").append(monthOfYear);
        if (dayOfMonthInt!=0) sb.append(',').append("dayOfMonth:").append(dayOfMonth);
        if (dayOfWeek!=null) sb.append(',').append("dayOfWeek:").append(dayOfWeek);
        if (dayOccurrence!=null) sb.append(',').append("dayOccurrence:").append(dayOccurrence);
        if (hours!=0) sb.append(',').append("hours:").append(hours);
        if (minutes!=0) sb.append(',').append("minutes:").append(minutes);
        if (seconds!=0) sb.append(',').append("seconds:").append(seconds);
        if (milliseconds!=0) sb.append(',').append("milliseconds:").append(milliseconds);
        if (initialDelayInMillis!=0) sb.append(',').append("initialDelayInMillis:").append(initialDelayInMillis);
        sb.append('}');
        return sb.toString();
    }

    /**
     * Executed during or after parsing to validate each configured task.
     * @throws SchedulerConfigException (unchecked) if any information is missing or wrong.
     */
    public void validate(ClassLoader classLoader) {

        try {
            // first verify globally-required attributes were defined
            if (this.className == null) {
                String msg = "CONFIGURATION ERROR: Required task property 'className' is missing";
                throw new SchedulerConfigException(msg).setCurrentState(this.toString());
            }
            if (this.interval == null) {
                String msg = "CONFIGURATION ERROR: Required task property 'interval' is missing for class: "+className;
                throw new SchedulerConfigException(msg).setCurrentState(this.toString());
            }

            // validate the class is actually a valid task class
            if (this.task == null) {
                // validate className
                Class<?> c = null;
                try {
                    c = classLoader.loadClass(this.className);
                    if (this.interval.equals("CUSTOM")) {
                        // CUSTOM tasks must extend ScheduledTask
                        if (!ScheduledTask.class.isAssignableFrom(c)) {
                            String msg = "CONFIGURATION ERROR: CUSTOM task class (" + className +
                                    ") does not extend ScheduledTask - declare a different interval=\"...\" if this is an iTask";
                            throw new SchedulerConfigException(msg).setCurrentState(this.toString());
                        }
                    } else {
                        // non-CUSTOM tasks must implement iTask
                        if (!iTask.class.isAssignableFrom(c)) {
                            String msg = "CONFIGURATION ERROR: task class ("+className+") must implement iTask!";
                            throw new SchedulerConfigException(msg).setCurrentState(this.toString());
                        }
                    }
                } catch (ClassNotFoundException e) {
                    String msg = "CONFIGURATION ERROR: task class ("+className+") is not in the classpath!";
                    throw new SchedulerConfigException(msg).setCurrentState(this.toString());
                }
            }

            // validate configured attributes according to defined interval
            // - if there are no properties to validate, then the other input values
            //   will be validated when the task gets scheduled
            if (this.interval.equals("CUSTOM")) {
                // no properties to validate
            } else if (this.interval.equals("ONE_TIME")) {
                // no properties to validate
            } else if (this.interval.equals("HOURS")) {
                // no properties to validate
            } else if (this.interval.equals("MINUTES")) {
                // no properties to validate
            } else if (this.interval.equals("SECONDS")) {
                // no properties to validate
            } else if (this.interval.equals("MILLISECONDS")) {
                // no properties to validate
            } else if (this.interval.equals("DAILY")) {
                // no properties to validate
            } else if (this.interval.equals("WEEKLY")) {
                // WEEKLY interval - validate dayOfWeek corresponds to DayOfWeek enum
                if (this.dayOfWeek != null) {
                    try {
                        // use enum for validation
                        DayOfWeek.valueOf(dayOfWeek);
                    } catch (IllegalArgumentException ex) {
                        String msg = "CONFIGURATION ERROR: task property 'dayOfWeek' is invalid ("+dayOfWeek+") for class: "+className;
                        throw new SchedulerConfigException(msg).setCurrentState(this.toString());
                    }
                } else {
                    String msg = "CONFIGURATION ERROR: task property 'dayOfWeek' is missing for class: "+className;
                    throw new SchedulerConfigException(msg).setCurrentState(this.toString());
                }
            } else if (this.interval.equals("MONTHLY")) {
                // MONTHLY interval - validate dayOfWeek and dayOccurrence values correspond to
                // legal enum types, unless the dayOfMonth technique is being used
                if (this.dayOfMonthInt == 0) {
                    // dayOfMonth not being used, validate the alternative attrs
                    if (this.dayOfWeek != null) {
                        try {
                            // use enum for validation
                            DayOfWeek.valueOf(dayOfWeek);
                        } catch (IllegalArgumentException ex) {
                            String msg = "CONFIGURATION ERROR: task property 'dayOfWeek' is invalid ("+dayOfWeek+") for class: "+className;
                            throw new SchedulerConfigException(msg).setCurrentState(this.toString());
                        }
                    } else {
                        String msg = "CONFIGURATION ERROR: task property 'dayOfWeek' is missing for class: "+className;
                        throw new SchedulerConfigException(msg).setCurrentState(this.toString());
                    }
                    if (this.dayOccurrence != null) {
                        try {
                            // use enum for validation
                            DayOccurrence.valueOf(dayOccurrence);
                        } catch (IllegalArgumentException ex) {
                            String msg = "CONFIGURATION ERROR: task property 'dayOccurrence' is invalid ("+dayOccurrence+") for class: "+className;
                            throw new SchedulerConfigException(msg).setCurrentState(this.toString());
                        }
                    } else {
                        String msg = "CONFIGURATION ERROR: task property 'dayOccurrence' is missing for class: "+className;
                        throw new SchedulerConfigException(msg).setCurrentState(this.toString());
                    }
                }
            } else if (this.interval.equals("YEARLY")) {
                // YEARLY interval - validate monthOfYear corresponds to MonthOfYear enum
                if (this.monthOfYear != null) {
                    try {
                        // use enum for validation
                        MonthOfYear.valueOf(monthOfYear);
                    } catch (IllegalArgumentException ex) {
                        String msg = "CONFIGURATION ERROR: task property 'monthOfYear' is invalid ("+monthOfYear+") for class: "+className;
                        throw new SchedulerConfigException(msg).setCurrentState(this.toString());
                    }
                } else {
                    String msg = "CONFIGURATION ERROR: task property 'monthOfYear' is missing for class: "+className;
                    throw new SchedulerConfigException(msg).setCurrentState(this.toString());
                }
            } else {
                String msg = "CONFIGURATION ERROR: task property 'interval' is invalid ("+interval+") for class: "+className;
                throw new SchedulerConfigException(msg).setCurrentState(this.toString());
            }
        } catch (SchedulerConfigException ex) {
            throw ex;
        } catch (Exception ex) {
            String msg = "Unexpected error while validating xml configuration for task";
            throw new SchedulerConfigException(msg).setCurrentState(this.toString());
        }
    }

}
