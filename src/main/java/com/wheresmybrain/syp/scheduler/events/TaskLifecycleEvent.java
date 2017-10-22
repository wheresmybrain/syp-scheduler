package com.wheresmybrain.syp.scheduler.events;

import com.wheresmybrain.syp.scheduler.TaskEvent;

/**
 * This informational event is fired automatically by all
 * tasks as they move through their lifecycle. These events
 * are used by the framework to track task progress.
 *
 * @see TaskInternalState
 * @author @author <a href="mailto:chris.mcfarland@gmail.com">Chris McFarland</a>
 */
public class TaskLifecycleEvent extends TaskEvent {

    /**
     * This event is only instantiated by base ScheduledTask,
     * which sets its proxy on the event directly (unlike
     * other events, which get the proxy associated
     * with the current Thread.
     */
    public TaskLifecycleEvent(TaskProxy taskProxy) {
        this.setTaskProxy(taskProxy);
    }

    /**
     * Returns the new state the task is transitioning to
     */
    public TaskInternalState getNextState() {
        return this.getTaskProxy().getInternalState();
    }

}
