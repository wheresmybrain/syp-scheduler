package com.wheresmybrain.syp.scheduler.testevents;

import com.wheresmybrain.syp.scheduler.TaskEvent;
import com.wheresmybrain.syp.scheduler.TaskScheduler;
import com.wheresmybrain.syp.scheduler.events.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pausing tasks for TEST purposes: The way this works for this event handler,
 * and accompanying {@link PauseEvent}, is that any task can pause itself by passing
 * its taskId inside the PauseEvent.
 *
 * @author Chris McFarland
 */
public class PauseEventHandler implements EventListener {

    private static Logger log = LoggerFactory.getLogger(PauseEventHandler.class);

    private TaskScheduler taskScheduler;

    public PauseEventHandler(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    /**
     * Handles {@link PauseEvent} events by pausing the task that
     * fired the event.
     *
     * @see EventListener#handleEvent(TaskEvent)
     */
    public void handleEvent(TaskEvent event) {
        if (event instanceof PauseEvent) {
            int taskId = event.getTaskProxy().getTaskId();
            log.debug("XX PauseEventHandler - pausing task #"+taskId);
            this.taskScheduler.pauseTask(taskId);
        }
    }

}
