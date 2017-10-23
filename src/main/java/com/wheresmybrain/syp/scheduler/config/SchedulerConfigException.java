package com.wheresmybrain.syp.scheduler.config;

/**
 * Unchecked exception thrown when there is a configuration error. The developer should
 * call the {@link #setCurrentState(String)} method whenever this exception is thrown.
 */
public class SchedulerConfigException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private static final String EOL = System.getProperty("line.separator");
    private String currentState;

    /**
     * Constructs a new configuration exception - the developer should call the
     * {@link #setCurrentState(String)} method whenever this exception is thrown.
     */
    public SchedulerConfigException(String message) {
        super(message);
    }

    /**
     * Constructs a new configuration exception - the developer should call the
     * {@link #setCurrentState(String)} method whenever this exception is thrown.
     */
    public SchedulerConfigException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new configuration exception - the developer should call the
     * {@link #setCurrentState(String)} method whenever this exception is thrown.
     */
    public SchedulerConfigException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Sets the "current state" at the time of the error. This is reported
     * when the exception is logged or when toString() is called.
     *
     * @param currentState represents the current state when the error occurs, and should
     *   include all information that would help debugging.
     * @return reference to this exception object so this method call can be chained.
     */
    public SchedulerConfigException setCurrentState(String currentState) {
        this.currentState = currentState;
        return this;
    }

    /**
     * Returns the "current state" at the time of the error.
     */
    public String getCurrentState() {
        return this.currentState;
    }

    @Override
    public String toString() {
        if (this.currentState != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("CONFIGURATION ERROR:").append(EOL);
            sb.append("State at time of error: ").append(this.currentState).append(EOL);
            sb.append(super.toString());
            return sb.toString();
        } else {
            return super.toString();
        }
    }

}
