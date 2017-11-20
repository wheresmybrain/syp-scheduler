package com.wheresmybrain.syp.scheduler;

import com.wheresmybrain.syp.scheduler.events.EventListener;

/**
 * Implement this interface to create a schedule-able task. Task implementations perform
 * work. The scheduling part is separate, so the Task developer only needs to think about
 * implementing this interface and writing the business logic that gets executed every time
 * the Task is run. The same task can be scheduled to execute in many different ways. See
 * the {@link TaskScheduler} for how to schedule your task.
 * <p/>
 * If a task is scheduled to execute more than one time, it is important to know that all
 * subsequent executions of the same scheduled task are performed by the same task object,
 * so you can code your task as a stateful object that can share its state information from
 * run to run. Tasks can also share information with other scheduled tasks, or with the
 * application itself, by writing data to the {@link SchedulerContext}. Tasks can also
 * transmit real-time {@link TaskEvent event} information externally to event
 * {@link EventListener listeners} using the {@link TaskUtils#fireEvent(TaskEvent)} method.
 * See {@link TaskEvent} for more information on creating events and listeners to
 * externalize data.
 *
 * @author @author <a href="mailto:chris.mcfarland@gmail.com">Chris McFarland</a>
 */
public interface Task {

    /**
     * Implement this method to execute the task logic.
     *
     * @param schedulerContext allows task to store data shared with other tasks or different
     *   executions of the same task.
     * @throws Throwable handled by the framework. Task developers should throw any unexpected
     *   errors as Runtime Exceptions and it will be handled automatically.
     */
    void executeTask(SchedulerContext schedulerContext) throws Throwable;

    /**
     * Implement this method to provide the current state of your task
     * at the time it fails. This is optional (you can just return an empty array
     * or null), but implementing this method should make troubleshooting easier.
     * <p/>
     * This method is executed by the framework when a task fails and is
     * used by the internal error handler to report information about the failure.
     */
    String[] getDebugState();

}
