package com.wheresmybrain.syp.scheduler.enums;

import com.wheresmybrain.syp.scheduler.SypScheduler;
import com.wheresmybrain.syp.scheduler.Task;

/**
 * Defines interval types available for scheduling recurring interval tasks.
 * This enum type is used by the {@link SypScheduler#scheduleIntervalExecution(Task, long, int, IntervalType)}
 * method.
 */
public enum IntervalType {
    HOURS,
    MINUTES,
    SECONDS,
    MILLISECONDS
}
