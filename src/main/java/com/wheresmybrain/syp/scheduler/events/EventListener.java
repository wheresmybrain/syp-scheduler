package com.wheresmybrain.syp.scheduler.events;

import com.wheresmybrain.syp.scheduler.SchedulerContext;
import com.wheresmybrain.syp.scheduler.TaskEvent;
import com.wheresmybrain.syp.scheduler.TaskScheduler;
import com.wheresmybrain.syp.scheduler.TaskUtils;

/**
 * Implement this interface to create a custom event handling mechanism to
 * handle events fired from your tasks. Tasks run inside their own Thread,
 * so setting up a custom event handling mechanism is the way to externalize
 * event information (as it occurs). The other way to externalize, or share,
 * information is by adding data to the {@link SchedulerContext}, which is
 * also accessible to your application.
 * <p/>
 * Here are the steps to set up a custom event-handling mechanism:
 * <ol>
 * <li>Extend {@link TaskEvent} to create a custom event</li>
 * <li>Implement this interface to create an event listener to handle the
 *   events real-time as they come in. Since all scheduler events will be
 *   passed into this method, the best way to identify the events you care
 *   about is the Java <code>instanceof</code> operator</li>
 * <li>Code your task to instantiate your custom event object and execute the
 *   {@link TaskUtils#fireEvent(TaskEvent)} method from your task
 *   to "fire" the event to the listener(s)</li>
 * <li>Code your application to register your custom event listener using
 *   the {@link TaskScheduler#addEventListener(EventListener)} method</li>
 *
 * @see TaskEvent
 * @author @author <a href="mailto:chris.mcfarland@gmail.com">Chris McFarland</a>
 */
public interface EventListener {

    /**
     * Implement this method to handle any type of event fired by the tasks.
     *
     * @param event subclass of base {@link TaskEvent} type.
     */
    void handleEvent(TaskEvent event);

}
