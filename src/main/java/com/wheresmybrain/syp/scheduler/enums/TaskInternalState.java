package com.wheresmybrain.syp.scheduler.enums;

/**
 * Represents the internal states
 * in a task's "lifecycle". The task's
 * state changes are reported externally
 * with {@link LifecycleEvent events}.
 */
public enum TaskInternalState {
    INACTIVE,
    ACTIVE,
    SCHEDULED,
    EXECUTING
}
