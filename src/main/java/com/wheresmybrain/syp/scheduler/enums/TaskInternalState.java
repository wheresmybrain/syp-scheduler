package com.wheresmybrain.syp.scheduler.enums;

import com.wheresmybrain.syp.scheduler.events.TaskLifecycleEvent;

/**
 * Represents the internal states
 * in a task's "lifecycle". The task's
 * state changes are reported externally
 * with {@link TaskLifecycleEvent events}.
 */
public enum TaskInternalState {
    INACTIVE,
    ACTIVE,
    SCHEDULED,
    EXECUTING
}
