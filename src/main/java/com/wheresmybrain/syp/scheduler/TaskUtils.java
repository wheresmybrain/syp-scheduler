package com.wheresmybrain.syp.scheduler;

import com.wheresmybrain.syp.scheduler.events.EventListener;
import com.wheresmybrain.syp.scheduler.events.TaskProxy;
import com.wheresmybrain.syp.scheduler.events.errorhandler.TaskErrorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class defines public utility methods useful to the developer of
 * {@link Task Scheduled Tasks} to get information and access to functions that would
 * not be otherwise accessible. <b>Note that these utility methods can
 * only be called when the task is executed!</b>
 *
 * @author @author <a href="mailto:chris.mcfarland@gmail.com">Chris McFarland</a>
 */
public final class TaskUtils {

    //-- static

    private static final Logger log = LoggerFactory.getLogger(TaskUtils.class);

    // thread-local var associates a task w/ its Thread
    private static InheritableThreadLocal<ScheduledTask> taskForThread = new InheritableThreadLocal<>();

    // maintaining a list of event listeners
    private static List<EventListener> listeners = Collections.synchronizedList(new ArrayList<>());

    /**
     * Associates the task with the current (task) Thread so it
     * can be retrieved any time by the same Thread.
     */
    static void setTask(ScheduledTask task) {
        log.debug("Associating task with its Thread ("+Thread.currentThread().getName()+"): "+task);
        taskForThread.set(task);
    }

    /**
     * Disassociates the task from the task Thread.
     */
    static void clearTask() {
        log.debug("Disassociating task from its Thread ("+Thread.currentThread().getName()+")");
        taskForThread.remove();
    }

    /**
     * Tasks (both {@link Task} implementers and custom {@link ScheduledTask} tasks)
     * can execute this method to transmit {@link TaskEvent events} to all the registered
     * event listeners. You can create custom events for externalizing real-time information
     * from your tasks by extending TaskEvent, and implementing {@link EventListener} and
     * registering your listener with the SypScheduler.
     * <p/>
     * Note that only tasks can call this method or a runtime exception is thrown.
     * <p/>
     * It is important for developers to know that this method sets the {@link TaskProxy}
     * that calls this method on the event transparently. This gives the event handler
     * access to the ScheduledTask public API. If you implemented Task, then your task
     * object is also accessible from the TaskProxy so the event handler has access to it.
     * <p/>
     * Don't use this method to send {@link TaskErrorEvent error events}, because
     * that is already being done automatically by the framework!
     *
     * @param event the event to fire to listeners
     * @see EventListener
     */
    public static void fireEvent(TaskEvent event) {
        TaskProxy taskProxy = event.getTaskProxy();
        if (taskProxy == null) {
            ScheduledTask currentTask = taskForThread.get();
            if (currentTask != null) {
                taskProxy = currentTask.getTaskProxy();
                event.setTaskProxy(taskProxy);
            } else {
                throw new IllegalStateException("Only tasks (Task, custom tasks) can call this method!");
            }
        }
        // let listeners handle the event
        for (EventListener listener : listeners) {
            listener.handleEvent(event);
        }
    }

    /**
     * Returns the task id for the task that calls this method. This is used by the
     * {@link Task} implementations, which don't have any other means of knowing the
     * task id of the "mixin" object that wraps the Task.
     * <p/>
     * Note that only tasks can call this method or a runtime exception is thrown.
     */
    public static int getTaskId() {
        ScheduledTask currentTask = taskForThread.get();
        if (currentTask != null) {
            return currentTask.getTaskId();
        } else {
            throw new IllegalStateException("Only tasks can call this method!");
        }
    }

    /**
     * Returns the task info (toString) for the task that calls this method. This is used
     * by the {@link Task} implementations, which don't have any other means of knowing
     * information about the "mixin" (ScheduledTask) object that wraps them.
     * <p/>
     * Note that only tasks can call this method or a runtime exception is thrown.
     */
    public static String getTaskInfo() {
        ScheduledTask currentTask = taskForThread.get();
        if (currentTask != null) {
            return currentTask.getTaskInfo();
        } else {
            throw new IllegalStateException("Only tasks can call this method!");
        }
    }

    /**
     * Called by a task to add a custom event listener at runtime. Use the
     * {@link SypScheduler#addEventListener(EventListener)} method to add event
     * listeners during setup.
     */
    public static void addEventListener(EventListener eventListener) {
        listeners.add(eventListener);
    }

    //-- instance

    /**
     * Private constructor so this class cannot be instantiated.
     */
    private TaskUtils() {
    }

}
