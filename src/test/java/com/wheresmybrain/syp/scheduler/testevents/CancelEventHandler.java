package com.wheresmybrain.syp.scheduler.testevents;

import com.wheresmybrain.syp.scheduler.TaskEvent;
import com.wheresmybrain.syp.scheduler.TaskScheduler;
import com.wheresmybrain.syp.scheduler.events.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Canceling tasks for TEST purposes: The way this works for this event handler,
 * and accompanying {@link CancelEvent}, is that any task can cancel itself by passing
 * its taskId inside the CancelEvent. It is also possible for a task to fire an event
 * to cancel other tasks, but that task would need to know the id's of the
 * task(s) to cancel - probably by storing the ids in context.
 *
 * @author Chris McFarland
 */
public class CancelEventHandler implements EventListener {

    private static Logger log = LoggerFactory.getLogger(CancelEventHandler.class);

    private TaskScheduler taskScheduler;

    public CancelEventHandler(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    /**
     * Handles {@link CancelEvent} events by canceling the task that
     * fired the event.
     *
     * @see EventListener#handleEvent(TaskEvent)
     */
    public void handleEvent(TaskEvent event) {
        if (event instanceof CancelEvent) {
            int taskId = event.getTaskProxy().getTaskId();
            log.debug("XX CancelEventHandler - canceling task #"+taskId);
            this.taskScheduler.cancelTask(taskId);
        }
    }
}
