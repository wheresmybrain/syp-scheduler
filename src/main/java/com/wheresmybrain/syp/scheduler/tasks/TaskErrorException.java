package com.wheresmybrain.syp.scheduler.tasks;

/**
 * This Exception class is used by the framework and should not be used directly
 * by the Task developer. Instead, the developer should <b>not catch any Exceptions
 * inside the <code>executeTask</code> method</b> (not even to log them), unless
 * the code can handle and recover from the error!
 *
 * @author <a href="mailto:chris.mcfarland@gmail.com">Chris McFarland</a>
 */
public class TaskErrorException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TaskErrorException() {
    }

    public TaskErrorException(String message) {
        super(message);
    }

    public TaskErrorException(Throwable cause) {
        super(cause);
    }

    public TaskErrorException(String message, Throwable cause) {
        super(message, cause);
    }

}
