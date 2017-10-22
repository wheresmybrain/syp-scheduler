package com.wheresmybrain.syp.scheduler.events.errorhandler;

import com.wheresmybrain.syp.scheduler.TaskEvent;
import com.wheresmybrain.syp.scheduler.events.iEventListener;

/**
 * Signals a task error to {@link iEventListener listeners} that are interested in this
 * type of event. This event is generated when any Exception is thrown, but not handled
 * while the task is executing.
 *
 * @see TaskErrorHandler
 * @see TaskEvent
 * @author @author <a href="mailto:chris.mcfarland@gmail.com">Chris McFarland</a>
 */
public class TaskErrorEvent extends TaskEvent {

    private Exception taskException;

    /**
     * @param taskException the Exception that was thrown when the
     *   task was executing.
     */
    public TaskErrorEvent(Exception taskException) {
        super();
        this.taskException = taskException;
    }

    /**
     * Returns the Exception that was thrown when
     * the task was executing.
     */
    public Exception getTaskException() {
        return taskException;
    }

    /**
     * Returns the email addresses to contact for the failing task.
     * @return email addresses to contact on failure, returns null or
     *   empty array if no task-specific addresses were specified.
     */
    public String[] getEmails() {
        return super.getTaskProxy().getEmails();
    }

}
