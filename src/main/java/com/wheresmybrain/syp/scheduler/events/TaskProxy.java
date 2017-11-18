package com.wheresmybrain.syp.scheduler.events;

import com.wheresmybrain.syp.scheduler.ScheduledTask;
import com.wheresmybrain.syp.scheduler.TaskEvent;
import com.wheresmybrain.syp.scheduler.enums.TaskInternalState;
import com.wheresmybrain.syp.scheduler.iTask;
import com.wheresmybrain.syp.scheduler.mixins.AbstractMixin;

/**
 * The TaskProxy is passed inside a {@link TaskEvent}, and can be used by
 * the event handler to execute methods on the {@link ScheduledTask task} that
 * fired the event. Not all the methods of a ScheduledTask are made available,
 * but only the ones that might be useful for event handling (thus, a proxy
 * is passed, and not the actual task).
 * <p/>
 * This class also defines one additional method ({@link #getInnerTask()})
 * that returns the {@link iTask} that fired the event if the ScheduledTask
 * is a {@link AbstractMixin mixin}. This will provide access to any special
 * methods added to the iTask implementation for event handling purposes.
 *
 * @author <a href="mailto:chris.mcfarland@gmail.com">chris.mcfarland</a>
 */
public class TaskProxy {

    private ScheduledTask task;

    /**
     * @param task the ScheduledTask that fired this event.
     */
    public TaskProxy(ScheduledTask task) {
        this.task = task;
    }

    /**
     * If the ScheduledTask that fired this event is a {@link AbstractMixin mixin},
     * which is a ScheduledTask subclass, then this method returns the mixin's inner
     * {@link iTask} that fired the event. But if the task that fired the event is
     * a "custom" task (non-mixin subclass), then this method returns null.
     */
    public iTask getInnerTask() {
        if (this.task instanceof AbstractMixin) {
            return ((AbstractMixin)task).getInternalTask();
        } else {
            return null;
        }
    }

    /**
     * Returns the task id of the task that fired the event.
     */
    public int getTaskId() {
        return this.task.getTaskId();
    }

    /**
     * Returns the {@link TaskInternalState} of the task that fired the event.
     */
    public TaskInternalState getInternalState() {
        return this.task.getInternalState();
    }

    /**
     * Returns the current state of the task at the time it failed. This
     * is an optionally-implemented method on tasks, and might return an
     * empty array, or even null.
     */
    public String[] getDebugState() {
        return this.task.getDebugState();
    }

    /**
     * Returns the email addresses to contact when task fails.
     * @return email addresses to contact on failure, returns null or
     *   empty array if no task-specific addresses were specified.
     */
    public String[] getEmails() {
        return this.task.getEmails();
    }

    /**
     * If the ScheduledTask that fired this event is a {@link AbstractMixin mixin},
     * which is a ScheduledTask subclass, then this method returns the mixin's inner
     * {@link iTask} fully qualified class name. But if the task that fired the event
     * is a "custom" task (non-mixin subclass), then this method returns the custom
     * task class name.
     */
    public Class<?> getTaskClass() {
        return this.task.getTaskClass();
    }

    /**
     * Returns an information String containing information on the task
     * that fired the event. For logging/debugging purposes.
     */
    public String getTaskInfo() {
        return this.task.getTaskInfo();
    }

    /**
     * Returns the toString() for task that fired the event.
     */
    @Override
    public String toString() {
        return this.task.toString();
    }
}
