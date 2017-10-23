package com.wheresmybrain.syp.scheduler.enums;

/**
 * Defines day occurrence in month constants for Monthly and Yearly scheduling.
 * For instance, setting dayOccurrence=DayOccurrence.FIRST and dayOfWeek=DayOfWeek.TUESDAY
 * on a Monthly schedule will execute the task on the first Tuesday of every month.
 */
public enum DayOccurrence {
    FIRST, SECOND, THIRD, FOURTH, LAST
}
