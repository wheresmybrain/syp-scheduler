package com.wheresmybrain.syp.scheduler;

import com.wheresmybrain.syp.scheduler.events.TaskLifecycleEvent;
import com.wheresmybrain.syp.scheduler.events.TaskProxy;
import com.wheresmybrain.syp.scheduler.events.errorhandler.TaskErrorEvent;
import com.wheresmybrain.syp.scheduler.utils.TimeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Date;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * Abstract base class for tasks executed by the {@link TaskScheduler}. The actual
 * work that a task executes is implemented by the {@link #executeTask(SchedulerContext)} method.
 * <p/>
 * This class can be used to create a <i>custom task</i> that executes one time (see next paragraph).
 * However, this class is not meant to be extended by the developer to create a schedule-able task. It
 * is much easier to create a task by implementing {@link iTask}, and then schedule the task using one
 * of the {@link TaskScheduler} <code>schedule...</code> methods.
 * <p/>
 * This class can be extended to create a one-time execution Task by following two steps:
 * <ol>
 *   <li>
 *       Implement the abstract {@link #executeTask(SchedulerContext)} method to execute the task's custom
 *   logic.
 *   </li>
 *   <li>
 *       Implement the abstract {@link #getNextExecutionTime()} method to return the <code>Date</code>
 *       object corresponding to when the task is scheduled to execute next.</li>
 * </ol>
 * <p/>
 * Also, to create a concrete task that has custom scheduling behavior and executes more than once,
 * extend the {@link RecurringTask} class and follow the same two steps. All executions of a scheduled
 * task are performed by the same task instance, so you can code your task as a stateful object that
 * can share its state information across all executions. As already mentioned though, it's easier to
 * create tasks by implementing <code>iTask</code> and scheduling the task through <code>TaskScheduler</code>.
 *
 * @see TaskScheduler
 * @author @author <a href="mailto:chris.mcfarland@gmail.com">Chris McFarland</a>
 */
public abstract class ScheduledTask implements Runnable, Delayed {

    private static Log log = LogFactory.getLog(ScheduledTask.class);

    // holds an internal reference to scheduler so tasks
    // can schedule subsequent executions
    private TaskScheduler scheduler;

    // context object stores data to share with other tasks
    // or different executions of the same task
    private SchedulerContext schedulerContext;

    // scheduled when init() is called
    private Date executionTime;

    // emails to notify when this task fails
    private String[] emails;

    // taskId makes a task unique
    private int taskId = -1;

    // a paused task can continue to schedule, but not execute
    private boolean paused;

    // tracks internal state for "lifecycle" events
    private TaskInternalState internalState = TaskInternalState.INACTIVE;

    /**
     *
     */
    public ScheduledTask() {
        this.internalState = TaskInternalState.ACTIVE;
        TaskUtils.fireEvent(new TaskLifecycleEvent(this.getTaskProxy()));
    }

    /**
     * Implement this method to execute the task logic.
     *
     * @param schedulerContext allows task to store data shared with other tasks or different
     *   executions of the same task.
     * @throws Throwable handled by the framework. Task developers should throw any unexpected
     *   errors as Runtime Exceptions and it will be handled automatically.
     */
    public abstract void executeTask(SchedulerContext schedulerContext) throws Throwable;

    /**
     * Implement this method to provide the current state of your task
     * at the time it fails. This is optional (you can just return an empty array
     * or null), but implementing it can make troubleshooting significantly easier
     * when an error occurs.
     * <p/>
     * This method is executed by the TaskScheduler when a task fails and is
     * used by the internal error handler to report information about the failure.
     */
    public abstract String[] getDebugState();

    /**
     * Executed by the framework to prepare the task for a new execution.
     *
     * @param scheduler sets an internal reference to the TaskScheduler so
     *   the task can reschedule itself.
     * @param schedulerContext allows task to store data shared with other tasks or different
     *   executions of the same task.
     * @param initialDelayInMillis the delay (in milliseconds) before the very first
     *   execution. If the initialDelayInMillis=0, then the task executes immediately
     *   when it is initialized, or when the TaskScheduler first starts, whichever
     *   comes last. If initialDelayInMillis is < 0, then the initial delay is
     *   ignored and the task's internal scheduling interval is used instead - so
     *   an hourly task set to execute once per hour would execute one hour after
     *   this method is called, and a task set to execute the first day of every month
     *   will wait until next month to execute.
     */
    void prepare(TaskScheduler scheduler, SchedulerContext schedulerContext, long initialDelayInMillis) {
        this.scheduler = scheduler;
        this.schedulerContext = schedulerContext;
        // schedule next execution
        if (initialDelayInMillis >= 0) {
            // execute after specified delay
            long executionTimeInMillis = new Date().getTime() + initialDelayInMillis;
            this.executionTime = new Date(executionTimeInMillis);
        } else {
            // execute according to internal schedule
            this.executionTime = this.getNextExecutionTime();
        }
        // this task will now be scheduled for execution
        this.internalState = TaskInternalState.SCHEDULED;
        log.debug("task ("+this+") prepared for next execution: "+
                TimeUtils.getTimeDescription(this.getDelay(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS));
        TaskUtils.fireEvent(new TaskLifecycleEvent(this.getTaskProxy()));
    }

    /**
     * Returns the time until the next scheduled execution. The framework calls this method.
     *
     * @param unit the {@link TimeUnit} constant corresponding to the desired units for
     *   the returned delay time.
     * @return time until the next scheduled execution in the specified units. If the
     * scheduled execution is in the past, then the returned value will be negative. If
     * this task object has not been assigned an execution time yet, then the returned
     * value will be -1.
     * @see java.util.concurrent.Delayed#getDelay(java.util.concurrent.TimeUnit)
     */
    public final long getDelay(TimeUnit unit) {
        long delayInMillis = -1L;
        if (this.executionTime != null) {
            delayInMillis = this.executionTime.getTime() - new Date().getTime();
        }
        return unit.convert(delayInMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Returns the date & time for the next scheduled execution. This method is
     * executed by the framework when a task is scheduled.
     */
    protected abstract Date getNextExecutionTime();

    /**
     * Tasks sort by delay, with the smallest delay first.
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public final int compareTo(Delayed otherTask) {
        if (otherTask == null) return 1;
        if (otherTask == this) return 0;
        if (!(otherTask instanceof ScheduledTask)) {
            throw new ClassCastException("not a ScheduledTask subclass!");
        }
        // compare to the other task's delay
        long comparison = this.getDelay(TimeUnit.MILLISECONDS) - otherTask.getDelay(TimeUnit.MILLISECONDS);
        if (comparison > 0) {
            return 1;
        } else if (comparison < 0) {
            return -1;
        } else {
            return 0;
        }
    }

    /**
     * This method is called by the TaskScheduler when it's time to execute a task.
     * All non-Error Exceptions (except InterruptedException) are caught and processed
     * so the Thread doesn't die.
     *
     * @throws RuntimeException (for handling by the scheduler) if the task
     *   throws any exceptions.
     */
    public void run() {
        try {
            if (!this.paused && this.internalState == TaskInternalState.SCHEDULED) {
                this.internalState = TaskInternalState.EXECUTING;
                TaskUtils.setTask(this); //associate task with Thread
                TaskUtils.fireEvent(new TaskLifecycleEvent(this.getTaskProxy()));
                this.executeTask(this.schedulerContext);
            } else if (this.paused) {
                log.debug("not executing paused task: "+this.toString());
            } else {
                TaskErrorException te = new TaskErrorException("illegal to attempt to execute task in non-SCHEDULED state");
                TaskUtils.fireEvent(new TaskErrorEvent(te));
            }
        } catch (TaskErrorException ex) {
            // handle the error
            this.handleError(ex);
        } catch (InterruptedException ex) {
            // task Thread was interrupted while blocking
            String message = "Task interrupted - THIS TASK WILL END";
            TaskErrorException te = new TaskErrorException(message, ex);
            this.handleError(te);
            // restore interrupted status so task is not rescheduled
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            // wrap in TaskErrorException
            // process this error
            String message = "Task error while executing";
            TaskErrorException te = new TaskErrorException(message, ex);
            this.handleError(te);
        } catch (Throwable ex) {
            // other Throwables (Errors) are fatal
            String message = "FATAL ERROR - THIS TASK WILL END";
            TaskErrorException te = new TaskErrorException(message, ex);
            this.handleError(te);
            // set interrupted status so task is not rescheduled
            Thread.currentThread().interrupt();
        } finally {
            // done executing - switch back to inactive
            this.internalState = TaskInternalState.INACTIVE;
            TaskUtils.clearTask(); //disassociate task from Thread
            TaskUtils.fireEvent(new TaskLifecycleEvent(this.getTaskProxy()));
        }
    }

    /**
     * Returns a reference to the parent TaskScheduler in case a task needs to
     * reschedule itself (recurring tasks do this).
     */
    protected final TaskScheduler getScheduler() {
        return scheduler;
    }

    /**
     * Sets the email addresses to contact if this task fails. Passing
     * null or empty array means only "global" support will be notified.
     */
    protected final void setEmails(String[] emails) {
        this.emails = emails;
    }

    /**
     * Returns the email addresses to contact if this task fails.
     * @return email addresses to contact on failure, returns null or
     *   empty array if no task-specific addresses were specified.
     */
    public String[] getEmails() {
        return this.emails;
    }

    /**
     * Executed by the TaskScheduler to assign a
     * task id to this task.
     */
    void setTaskId(final int taskId) {
        if (this.taskId < 0) {
            this.taskId = taskId;
        }
    }

    /**
     * Returns the task id for this task.
     */
    public final int getTaskId() {
        return this.taskId;
    }

    /**
     * Pauses this task so it does not execute when
     * its scheduled time arrives.
     */
    protected final void pause() {
        log.info("pausing task: "+this);
        this.paused = true;
    }

    /**
     * Resumes a paused task so it resumes its normal
     * execution when its scheduled time arrives.
     */
    protected final void resume() {
        log.info("resuming task: "+this);
        this.paused = false;
    }

    /**
     * This method lets subclasses set internal state as needed.
     */
    protected final void setInternalState(TaskInternalState internalState) {
        this.internalState = internalState;
    }

    /**
     * Returns the current {@link TaskInternalState}.
     */
    public synchronized TaskInternalState getInternalState() {
        return this.internalState;
    }

    /**
     * Returns true if this task is paused. Call
     * {@link #resume()} to un-pause the task.
     */
    protected final boolean isPaused() {
        return this.paused;
    }

    /**
     * Returns an information String containing the task class, task id and internal state.
     */
    public String getTaskInfo() {
        StringBuilder sb = new StringBuilder(this.getClass().getSimpleName())
                .append(" (Task #").append(this.taskId).append(") execution{")
                .append(TimeUtils.getTimeDescription(this.getDelay(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS))
                .append("} internal state: ").append(this.internalState);
        if (this.paused) sb.append(" (PAUSED)");
        return sb.toString();
    }

    /**
     * Returns the task class, which is normally the <code>ScheduledTask</code> subclass,
     * but in the case of specialized subclasses, could be different. This method is used
     * by the framework for reporting purposes.
     */
    public Class<?> getTaskClass() {
        return this.getClass();
    }

    /**
     * Returns a proxy to this task, which exposes a small subset of
     * methods from the task's API.
     */
    TaskProxy getTaskProxy() {
        return new TaskProxy(this);
    }

    /**
     * Returns {@link #getTaskInfo()} by default
     */
    @Override
    public String toString() {
        return this.getTaskInfo();
    }

    /**
     * Equal if other object instance of same class and has same task id.
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (obj.getClass() != this.getClass()) return false;
        ScheduledTask other = (ScheduledTask)obj;
        return (this.taskId == other.getTaskId());
    }

    @Override
    public int hashCode() {
        int hc = this.getClass().getName().hashCode();
        if (this.taskId >= 0) {
            hc = 37*hc + this.taskId;
        } else {
            hc = 37 * hc;
        }
        return hc;
    }

    /**
     * This method is executed internally any time the task execution
     * fails. The default behavior of this method is to wrap the
     * <code>TaskErrorException</code> in an <code>TaskErrorEvent</code>
     * and fire it to the registered listeners.
     *
     * @param taskException special TaskErrorException to handle
     */
    protected void handleError(TaskErrorException taskException) {
        // fire error to external event listeners for
        // processing by an error handler
        TaskUtils.fireEvent(new TaskErrorEvent(taskException));
    }

}
