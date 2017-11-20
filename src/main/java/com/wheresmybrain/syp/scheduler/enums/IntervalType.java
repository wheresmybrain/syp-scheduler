package com.wheresmybrain.syp.scheduler.enums;

import com.wheresmybrain.syp.scheduler.TaskScheduler;
import com.wheresmybrain.syp.scheduler.Task;

/**
 * Defines interval types available for scheduling recurring interval tasks.
 * This enum type is used by the {@link TaskScheduler#scheduleIntervalExecution(Task, long, int, IntervalType)}
 * method.
 */
public enum IntervalType {
    HOURS,
    MINUTES,
    SECONDS,
    MILLISECONDS
}
