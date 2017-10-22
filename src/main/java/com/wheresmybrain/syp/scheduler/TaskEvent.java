package com.wheresmybrain.syp.scheduler;

import com.wheresmybrain.syp.scheduler.events.TaskProxy;

import java.util.Date;

/**
 * Abstract base class for events that are fired by scheduler tasks and handled by
 * {@link iEventListener listeners}. Tasks fire events to either communicate or have
 * a situation handled outside the task. How the event is handled is dependent on the
 * corresponding listener implemented specifically for handling the event.
 * <p/>
 * Any application can create any event type by extending this class, as
 * long as it also creates a {@link iTask scheduledTask} to fire it, and a
 * {@link iEventListener listener} to handle it.
 * <p/>
 * <b>HOW TO CREATE, FIRE AND HANDLE AN EVENT:</b>
 * <ol>
 * <li>
 * Create the event class - A <i>TaskEvent</i> always stores some sort of information. This is
 * the reason for creating the event - to transport information from your scheduledTask's execution deep in
 * the bowels of the Scheduler framework to the outside world. Create your event class by
 * extending <code>TaskEvent</code> and adding one or more properties (w/ getters and setters)
 * to transport the information you need access to. Also, a reference to the scheduledTask is automatically
 * added by the framework to the event, and is accessible via the event's getTask() method.
 * </li>
 * <li>
 * Fire the event - It's super easy to "fire" an event from inside your {@link iTask scheduledTask's}
 * <code>executeTask()</code> method - just instantiate and populate the data in your event object,
 * and execute this the method: {@link TaskUtils#fireEvent(TaskEvent)}
 * </li>
 * <li>
 * Handle the event - Handling the event involves 2 steps: (a) create the "listener" class, and
 * (b) add the listener to the <i>TaskScheduler</i>. To create the listener class, just create a
 * new class that implements {@link iEventListener} and implement the <code>handleEvent()</code> method to
 * get the information to pass in your event and use it as needed. <b>Make sure you only handle *your*
 * event because every event fired by every scheduledTask comes through this method!</b>. You can
 * do it like this:
 * <pre>
 *     public final void handleEvent(TaskEvent event) {
 *         if (event instanceof MyEvent) {
 *             MyEvent myEvent = (MyEvent) event;
 *             // do something with the event
 *             ...
 *         }
 *     }
 * </pre>
 * Once you create your listener, you can add it to TaskScheduler with the following method:
 * {@link TaskScheduler#addEventListener(iEventListener)}
 * </li>
 * </ol>
 *
 * @author @author <a href="mailto:chris.mcfarland@gmail.com">Chris McFarland</a>
 */
public abstract class TaskEvent {

    private TaskProxy taskProxy;
    private Date timestamp = new Date();
    private String taskThreadName = Thread.currentThread().getName();

    /**
     * This method is called by the framework to set the
     * {@link TaskProxy} on this event.
     *
     * @param taskProxy a proxy (facade) for the ScheduledTask that fired the event.
     *   The taskProxy provides access to a subset of api methods from the
     *   ScheduledTask that fired this event.
     */
    protected final void setTaskProxy(TaskProxy taskProxy) {
        this.taskProxy = taskProxy;
    }

    /**
     * Returns a reference to the {@link TaskProxy} injected by
     * the framework on this event. The TaskProxy provides access to a
     * wealth of information about the task that fired this event.
     */
    public final TaskProxy getTaskProxy() {
        return this.taskProxy;
    }

    /**
     * Returns the timestamp for when the event occurred.
     */
    public final Date getTimestamp() {
        return this.timestamp;
    }

    /**
     * Returns the task's Thread name.
     */
    public String getTaskThreadName() {
        return taskThreadName;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append(" [scheduledTask=").append(taskProxy.getTaskClass().getSimpleName()).append("]");
        return sb.toString();
    }

}
