package com.wheresmybrain.syp.scheduler;

import com.wheresmybrain.syp.scheduler.events.TaskLifecycleEvent;
import com.wheresmybrain.syp.scheduler.events.TaskProxy;
import com.wheresmybrain.syp.scheduler.events.errorhandler.TaskErrorHandler;
import com.wheresmybrain.syp.scheduler.events.errorhandler.iErrorEmailer;
import com.wheresmybrain.syp.scheduler.events.iEventListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * This is the controller class for the SyP Scheduler component. The Scheduler enables
 * developers to create <i>Tasks</i> for performing work, and then schedule them to execute
 * one time or repeatedly on any type of schedule. The Task and the schedule are kept separate
 * so the same Task can be scheduled several different ways. Follow these steps
 * to execute tasks on a recurring schedule:
 * <ol>
 * <li>
 *   Create one or more Tasks that implement {@link iTask}.
 * </li>
 * <li>
 *   Use one of the constructors to get an instance of TaskScheduler.
 * </li>
 * <li>
 *   Optionally inject an "error emailer" (if you want to receive error notifications via
 *   email) with the following method: {@link #injectErrorEmailer(iErrorEmailer, String, String, String...)}.
 * </li>
 * <li>
 *   Start the TaskScheduler with its <code>start()</code> method (there's a stop() method too
 *   in case you need to pause or shut down the scheduler).
 * </li>
 * <li>
 *   Add the task(s) to the TaskScheduler at runtime using any of the <i>schedule..</i> methods.
 *   You have the option of scheduling tasks either in your code, from a properly-formatted
 *   configuration file (via {@link #scheduleTasks(InputStream)}, or both.
 * </li>
 * </ol>
 * The tasks will automatically execute on their corresponding schedules. If any task encounters
 * an error, it will log the details. If you inject an "error emailer" (via {@link
 * #injectErrorEmailer(iErrorEmailer, String, String, String...)}), an error notification will
 * be emailed to the support address(es) designated in the inject method. Also you can add
 * task-specific email addresses to notify with this method:
 * {@link #setTaskSpecificAddresses(int, String...)}. For tasks with frequent intervals
 * (execute several times an hour), error emails are "throttled", so that no more than one email
 * is sent per hour for the same error.
 * </p>
 * This scheduler is highly efficient and very accurate (w/in ~4ms). All tasks execute in their
 * own Threads to enable tasks to execute asynchronously from the application's main Thread, so
 * that the application does not have to wait for a task to execute, and so that all tasks scheduled
 * to execute at the same time do not have to wait on each other.
 * </p>
 * To schedule a task to execute one time only, follow the same procedure but use one of the
 * TaskScheduler <code>scheduleOneTimeExecution()</code> methods.
 *
 * @author <a href="mailto:chris.mcfarland@gmail.com">Chris McFarland</a>
 */
public class TaskScheduler implements iEventListener {

    //-- static

    private static Log log = LogFactory.getLog(TaskScheduler.class);

    /**
     * Use this constant for scheduling a monthly or yearly task to schedule a day relative to
     * the last day of the month. This example schedules a monthly task on the 2nd to the last day of every month:
     * <p/>
     * <code>
     *   <task className="org.mypkg.Task1" intervalType="MONTHLY" dayOfMonth="LAST_DAY_OF_MONTH-1" hour="02" minute="00"/>
     * </code>
     */
    public static final int LAST_DAY_OF_MONTH = -1000;

    //-- instance

    private DelayQueue<ScheduledTask> internalQueue = new DelayQueue<>(); //thread-safe
    private ProcessingThread taskProcessor;
    private SchedulerContext schedulerContext = new SchedulerContext(); //default ctx
    private TaskErrorHandler errorHandler;
    private int nextTaskId;

    // thread-safe maps
    private Map<Integer,ScheduledTask> taskMap = new ConcurrentHashMap<>();
    private Map<Integer,Future<?>> executingTasksMap = new ConcurrentHashMap<>();

    /**
     * Creates a task scheduler instance
     */
    public TaskScheduler() {
        this("TASK-SCHEDULER");
    }

    /**
     * Creates a task scheduler instance with the specified name. Use this
     * constructor if the application will create multiple instance of the
     * task scheduler (not recommended).
     */
    public TaskScheduler(String name) {
        this.taskProcessor = new ProcessingThread(name, this.internalQueue);
        // set the default error handler
        this.errorHandler = new TaskErrorHandler();
        // the scheduler listens for task events
        TaskUtils.addEventListener(this);
    }

    //-- Setup (before start) methods -----------------

    /**
     * Use this method to set a custom SchedulerContext. This method *must* be
     * called before the Scheduler is started.
     *
     * @param schedulerContext custom SchedulerContext subclass to replace the
     *   default context.
     * @throws IllegalStateException if this method is called after start()
     */
    public void setSchedulerContext(SchedulerContext schedulerContext) {
        if (this.isRunning()) {
            String msg = "cannot set scheduler context after scheduler starts!";
            throw new IllegalStateException(msg);
        }
        // replace default context with custom context
        log.info("replacing default scheduler context class with: "+schedulerContext.getClass().getName());
        this.schedulerContext = schedulerContext;
    }

    /**
     * This method injects a class that can be used to send error notifications via email.
     * This method *must* be called before the scheduler is started.
     * Use this method if you want to receive email notifications when tasks fail. See
     * {@link iErrorEmailer} for instructions for implementing the class to inject. If
     * no "error emailer" is injected, then the errors will only be logged by the
     * internal {@link TaskErrorHandler}.
     *
     * @param errorEmailer implementation of the iErrorEmailer interface
     * @param appName Sets the name of the application running this scheduler. Setting this
     *   attribute is optional, but troubleshooting errors will be much easier if this is
     *   set, especially if errors from different applications are emailed.
     * @param platformName Sets the name of the platform or server that the application is
     *   running on. Setting this attribute is optional, but troubleshooting errors will be
     *   much easier if this is set, especially if errors from different servers are emailed.
     * @param supportAddresses the set of global support email addresses to receive
     *   notification of errors for every task. Alternatively, or in addition to, addresses
     *   can be added by declaring the 'emails' attribute for the &lt; scheduler &gt; element
     *   in the xml config file, or emails can be added on a task-by-task basis via the
     *   {@link #setTaskSpecificAddresses(int, String...)} method. Email addresses must
     *   follow the correct format.
     * @throws IllegalStateException if this method is called after the scheduler is started.
     */
    public void injectErrorEmailer(
            iErrorEmailer errorEmailer,
            String appName,
            String platformName,
            String... supportAddresses)
    {
        if (this.isRunning()) {
            String msg = "cannot set error handler after scheduler starts!";
            throw new IllegalStateException(msg);
        }
        // add to error handler
        this.errorHandler.setErrorEmailer(errorEmailer, supportAddresses);
        this.errorHandler.setAppName(appName);
        this.errorHandler.setPlatformName(platformName);
        this.errorHandler.setErrorEmailer(errorEmailer, supportAddresses);
    }

    /**
     * This is the best way to specify email address(es) to notify if a
     * task fails. This is optional, as "global support" will be notified
     * regardless of whether task-specific addresses are declared. This
     * method can be called before or after starting the Scheduler, but is
     * most appropriately called during the Setup phase.
     *
     * @param taskId task id returned from executing the schedule... method
     * @param supportAddresses email addresses to notify if the specified task fails
     */
    public final void setTaskSpecificAddresses(int taskId, String... supportAddresses) {
        if (supportAddresses.length > 0) {
            ScheduledTask task = this.getScheduledTask(taskId);
            if (task != null) {
                task.setEmails(supportAddresses);
            }
        }
    }

    /**
     * Registers an event Listener with the Scheduler. An event listeners can either be
     * added before or after starting the Scheduler. Implement the {@link iEventListener}
     * interface to create an event listener to handle task events.
     * <p/>
     * Any application using the SyP Scheduler component can create and register a listener to
     * handle any {@link TaskEvent event} subclass fired by a running task. The application
     * can create its own event by extending {@link TaskEvent}) and coding its task(s) to
     * fire the events, which are caught and handled by the application's own event listener
     * (registered with this method).
     *
     * @param eventListener class implementing {@link iEventListener} to handle one
     *   or more types of events fired from the tasks.
     * @see iEventListener
     */
    public void addEventListener(iEventListener eventListener) {
        TaskUtils.addEventListener(eventListener);
    }

    //-- API methods -----------------

    /**
     * Exposes the {@link SchedulerContext context} object outside
     * the TaskScheduler. Tasks have access to this context, so data can be
     * stored by the tasks and accessed externally by the application that
     * uses the Scheduler.
     * <p/>
     * If a custom SchedulerContext class was set on this using the
     * {@link #setSchedulerContext(SchedulerContext)} method, then that context
     * object is returned from this method.
     */
    public SchedulerContext getSchedulerContext() {
        return this.schedulerContext;
    }

    /**
     * Returns the scheduled task corresponding to the taskId. This method issed
     * by subclasses to access tasks.
     *
     * @param taskId task id for the desired task
     * @return task corresponding to the id, or null if the task does not exist or
     *   is executing
     * @see #getExecutingTask(int)
     */
    protected final ScheduledTask getScheduledTask(int taskId) {
        Integer id = new Integer(taskId);
        return this.taskMap.get(id);
    }

    /**
     * Schedules the task to run one time only after the specified delay. This is
     * a really easy way for an application to schedule an asynchronous task.
     * For web applications this is a way to decouple a task's execution from the
     * request, so that the request does not wait until the task is completed. One
     * example of this is a request to upload a file, which then starts a task to
     * process the file in the background.
     * <p/>
     * To use this task the developer will need to create a <i>task</i> class by
     * implementing the {@link iTask} interface, then call this method to
     * execute the task after the specified delay.
     *
     * @param task the class that performs the work. This can be any class
     *   that implements iTask, and the same task object can be scheduled multiple times.
     * @param delayInMillis the delay (in milliseconds) before the task executes. If the
     *   delayInMillis<=0, then the task executes immediately when it is initialized, or
     *   when the TaskScheduler first starts, whichever comes last.
     * @return task id for the scheduled task. The task id can be used to un-schedule
     *   the task at any time.
     */
    public final int scheduleOneTimeExecution(iTask task, long delayInMillis) {

        if (delayInMillis < 0) delayInMillis = 0; //convert negative delay to immediate execution

        // make the task schedule-able
        ScheduledTask scheduledTask = new OneTimeTaskMixin(task);

        // schedule the task for execution
        return this.scheduleTask(scheduledTask, delayInMillis);
    }

    /**
     * Schedules the task to run one time only on the specified date and time.
     * If the specified time is in the past, then the task will execute immediately.
     * <p/>
     * To use this task the developer will need to create a <i>task</i> class by
     * implementing the {@link iTask} interface, then call this method to
     * execute the task after the specified delay.
     *
     * @param task the class that performs the work. This can be any class
     *   that implements iTask, and the same task object can be scheduled multiple times.
     * @param year required param specifies the year to execute as a 4-digit number.
     * @param monthOfYear enum constant representing one of the months in the year
     * @param dayOfMonth set to execute on day 1-31 (29-31 will execute on last day of month
     *   for months with less than those number of days), or set to constant TaskScheduler.LAST_DAY_OF_MONTH
     *   with (optionally) a day offset. Examples: dayOfMonth=16 (executes on 16th of every
     *   month), dayOfMonth=31 (executes on last day of every month),
     *   dayOfMonth=TaskScheduler..LAST_DAY_OF_MONTH (executes on last day of every month),
     *   dayOfMonth=TaskScheduler..LAST_DAY_OF_MONTH-1 (executes on next-to-last day of month).
     * @param hourOfDay hour (0-23) to execute on the specified dayOfMonth
     * @param minuteOfHour minute (0-59) to execute on the specfied hourOfDay
     * @return task id for the scheduled task. The task id can be used to un-schedule
     *   the task ahead of time.
     */
    public final int scheduleOneTimeExecution(
            iTask task,
            final int year,
            final MonthOfYear monthOfYear,
            final int dayOfMonth,
            final int hourOfDay,
            final int minuteOfHour)
    {
        // do some validation
        if (year < 2000) {
            throw new IllegalArgumentException("param 'year' must be 4 digit year");
        } else if (monthOfYear == null) {
            throw new IllegalArgumentException("'monthOfYear' is null, but must be specified");
        } else if ((dayOfMonth < TaskScheduler.LAST_DAY_OF_MONTH-30) || (dayOfMonth > TaskScheduler.LAST_DAY_OF_MONTH && dayOfMonth < 1) || (dayOfMonth > 31)) {
            String msg = "'dayOfMonth' must be either 1-31 or set to constant LAST_DAY_OF_MONTH minus some day offset (up to 30)";
            throw new IllegalArgumentException(msg);
        } else if (hourOfDay < 0 || hourOfDay > 23 || minuteOfHour < 0 || minuteOfHour > 59) {
            throw new IllegalArgumentException("'hourOfDay' must be specified 0-23 and 'minuteOfHour' 0-59");
        }

        // make the task schedule-able
        ScheduledTask scheduledTask = new OneTimeTaskMixin(task) {
            @Override
            protected Date getNextExecutionTime() {
                Calendar cal = new GregorianCalendar();
                cal.clear();
                cal.set(Calendar.YEAR, year);
                switch (monthOfYear) {
                    case JANUARY :
                        cal.set(Calendar.MONTH, Calendar.JANUARY);
                        break;
                    case FEBRUARY :
                        cal.set(Calendar.MONTH, Calendar.FEBRUARY);
                        break;
                    case MARCH :
                        cal.set(Calendar.MONTH, Calendar.MARCH);
                        break;
                    case APRIL :
                        cal.set(Calendar.MONTH, Calendar.APRIL);
                        break;
                    case MAY :
                        cal.set(Calendar.MONTH, Calendar.MAY);
                        break;
                    case JUNE :
                        cal.set(Calendar.MONTH, Calendar.JUNE);
                        break;
                    case JULY :
                        cal.set(Calendar.MONTH, Calendar.JULY);
                        break;
                    case AUGUST :
                        cal.set(Calendar.MONTH, Calendar.AUGUST);
                        break;
                    case SEPTEMBER :
                        cal.set(Calendar.MONTH, Calendar.SEPTEMBER);
                        break;
                    case OCTOBER :
                        cal.set(Calendar.MONTH, Calendar.OCTOBER);
                        break;
                    case NOVEMBER :
                        cal.set(Calendar.MONTH, Calendar.NOVEMBER);
                        break;
                    case DECEMBER :
                        cal.set(Calendar.MONTH, Calendar.DECEMBER);
                        break;
                }
                int calculatedDay = MonthUtils.calculateDayOfMonth(dayOfMonth, cal.getTime());
                cal.set(Calendar.DAY_OF_MONTH, calculatedDay);
                cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                cal.set(Calendar.MINUTE, minuteOfHour);
                cal.set(Calendar.MILLISECOND, 0);
                Date scheduleDate = cal.getTime();
                if (scheduleDate.before(new Date())) {
                    // ignore dates in the past - this task will execute immediately
                    scheduleDate = new Date();
                }
                return scheduleDate;
            }
        };

        // schedule the task for execution
        return this.scheduleTask(scheduledTask, -1);
    }

    /**
     * Thread-safe method schedules the task to run periodically according to the
     * specified interval and intervalType (units). For example, to schedule a task
     * to run once every ten minutes, you would specify IntervalType=MINUTES with
     * interval=10. Tasks scheduled with this method will be rescheduled for the next
     * interval once they are done - for example, if a task is scheduled to run once
     * every ten minutes, but it takes five minutes to run, then that task will follow
     * this sequence: run for 5 minutes - wait 10 minutes - run for 5 minutes - wait 10 minutes - ...
     * <p/>
     * To use this task the developer will need to create a <i>Task</i> class by
     * implementing the {@link iTask} interface, then call this method to
     * execute the task on the specified interval. Any Task can be scheduled multiple
     * times with any interval.
     * <p/>
     * Email address(es) to notify if the task fails can be associated with this task by
     * calling the TaskScheduler {@link #setTaskSpecificAddresses(int, String...) method with
     * the taskId returned from this method.
     *
     * @param task the class that performs the work. This can be any class
     *   that implements iTask, and the same task object can be scheduled multiple times.
     * @param initialDelayInMillis the delay (in milliseconds) before the very first
     *   execution. If the initialDelayInMillis=0, then the task executes immediately
     *   when it is initialized, or when the TaskScheduler first starts, whichever
     *   comes last. If initialDelayInMillis is < 0, then the initial delay is
     *   ignored and the task's internal scheduling interval is used instead.
     * @param interval the length of time (in the specified intervalType units) between
     *   executions. For example an interval=5 with intervalType=MINUTES creates a task
     *   that runs every 5 minutes.
     * @param intervalType enum constant that determines the units of the
     *   interval: {HOURS, MINUTES, SECONDS, MILLISECONDS} are the only valid intervals
     *   for this method. See: {@link IntervalType}.
     * @return task id for the scheduled task. (Note: the task id can be used to un-schedule
     *   the task at any time).
     * @throws IllegalArgumentException if an invalid intervalType is specified.
     */
    public final int scheduleIntervalExecution(
            iTask task,
            long initialDelayInMillis,
            int interval,
            IntervalType intervalType) {

        // make the task schedule-able w/ one of the mixins
        RecurringTask intervalTask = null;
        switch (intervalType) {
            case HOURS:
                intervalTask = new HourIntervalScheduleMixin(task, interval);
                break;
            case MINUTES:
                intervalTask = new MinuteIntervalScheduleMixin(task, interval);
                break;
            case SECONDS:
                intervalTask = new SecondIntervalScheduleMixin(task, interval);
                break;
            case MILLISECONDS:
                intervalTask = new MillisecondIntervalScheduleMixin(task, interval);
                break;
        }

        // schedule the task for execution
        return this.scheduleTask(intervalTask, initialDelayInMillis);
    }

    /**
     * Thread-safe method schedules the task to run every day at the specified time of day.
     * If the task is unable to execute at the scheduled time, then TaskScheduler will
     * continue to try until successful.
     * <p/>
     * To use this task the developer will need to create a <i>Task</i> class by
     * implementing the {@link iTask} interface, then call this method to execute that task
     * every day. Any Task can be scheduled multiple times with any interval.
     * <p/>
     * Unlike the {@link #scheduleIntervalExecution(iTask, long, int, IntervalType, String...)
     * schedule interval} method, this method does not have a mechanism for executing
     * the task immediately (or with a short delay). The purpose of <code>scheduleDailyExecution</code>
     * is to execute at a scheduled time. If you need to perform an initial execution before
     * letting the task run on its schedule, then code an additional one-time execution
     * of the same task using {@link #scheduleOneTimeExecution(iTask, long, String...).
     * <p/>
     * Email address(es) to notify if the task fails can be associated with this task by
     * calling the TaskScheduler {@link #setTaskSpecificAddresses(int, String...) method with
     * the taskId returned from this method.
     *
     * @param task the class that performs the work. This can be any class
     *   that implements iTask, and the same task object can be scheduled multiple times.
     * @param hourOfDay hour (0-23) to execute each day
     * @param minuteOfHour minute (0-59) to execute each hour
     * @param secondOfMinute minute (0-59) to execute each minute
     * @return task id for the scheduled task. The task id can be used to un-schedule
     *   the task at any time.
     */
    public final int scheduleDailyExecution(iTask task, int hourOfDay, int minuteOfHour, int secondOfMinute) {

        // make the task schedule-able w/ one of the mixins
        RecurringTask dailyTask = new DailyScheduleMixin(task, hourOfDay, minuteOfHour, secondOfMinute);

        // schedule the task for execution
        return this.scheduleTask(dailyTask, -1);
    }

    /**
     * Thread-safe method schedules the task to run every week at the specified day and time.
     * If the task is unable to execute at the scheduled time, then TaskScheduler will
     * continue to try until successful.
     * <p/>
     * To use this task the developer will need to create a <i>Task</i> class by implementing
     * the {@link iTask} interface, then call this method to execute that task every week.
     * Any Task can be scheduled multiple times with any interval.
     * <p/>
     * Unlike the {@link #scheduleIntervalExecution(iTask, long, int, IntervalType, String...)
     * schedule interval} method, this method does not have a mechanism for executing
     * the task immediately (or with a short delay). The purpose of <code>scheduleWeeklyExecution</code>
     * is to execute at a scheduled time. If you need to perform an initial execution before
     * letting the task run on its schedule, then code an additional one-time execution
     * of the same task using {@link #scheduleOneTimeExecution(iTask, long, String...).
     * <p/>
     * Email address(es) to notify if the task fails can be associated with this task by
     * calling the TaskScheduler {@link #setTaskSpecificAddresses(int, String...) method with
     * the taskId returned from this method.
     *
     * @param task the class that performs the work. This can be any class
     *   that implements iTask, and the same task object can be scheduled multiple times.
     * @param dayOfWeek enum constant for day-of-week
     * @param hourOfDay hour (0-23) to execute each day
     * @param minuteOfHour minute (0-59) to execute each hour
     * @return task id for the scheduled task. The task id can be used to un-schedule
     *   the task at any time.
     * @see DayOfWeek
     */
    public final int scheduleWeeklyExecution(
            iTask task,
            DayOfWeek dayOfWeek,
            int hourOfDay,
            int minuteOfHour)
    {
        // make the task schedule-able w/ one of the mixins
        RecurringTask weeklyTask = new WeeklyScheduleMixin(task, dayOfWeek, hourOfDay, minuteOfHour);

        // schedule the task for execution
        return this.scheduleTask(weeklyTask, -1);
    }

    /**
     * Thread-safe method schedules the task to run every month at the specified day and time.
     * If the task is unable to execute at the scheduled time, then TaskScheduler will
     * continue to try until successful.
     * <p/>
     * To use this task the developer will need to create a <i>Task</i> class by implementing
     * the {@link iTask} interface, then call this method to execute that task every month.
     * Any Task can be scheduled multiple times with any interval.
     * <p/>
     * Unlike the {@link #scheduleIntervalExecution(iTask, long, int, IntervalType, String...)
     * schedule interval} method, this method does not have a mechanism for executing
     * the task immediately (or with a short delay). The purpose of <code>scheduleMonthlyExecution</code>
     * is to execute at a scheduled time. If you need to perform an initial execution before
     * letting the task run on its schedule, then code an additional one-time execution
     * of the same task using {@link #scheduleOneTimeExecution(iTask, long, String...).
     * <p/>
     * Email address(es) to notify if the task fails can be associated with this task by
     * calling the TaskScheduler {@link #setTaskSpecificAddresses(int, String...) method with
     * the taskId returned from this method.
     *
     * @param task the class that performs the work. This can be any class
     *   that implements iTask, and the same task object can be scheduled multiple times.
     * @param dayOfMonth set to execute on day 1-31 (29-31 will execute on last day of month
     *   for months with less than those number of days), or set to constant LAST_DAY_OF_MONTH
     *   with (optionally) a day offset. Examples: dayOfMonth=16 (executes on 16th of every
     *   month), dayOfMonth=31 (executes on last day of every month),
     *   dayOfMonth=MonthlyScheduleMixin.LAST_DAY_OF_MONTH (executes on last day of every month),
     *   dayOfMonth=MonthlyScheduleMixin.LAST_DAY_OF_MONTH-1 (executes on next to last day of month).
     * @param hourOfDay hour (0-23) to execute each day
     * @param minuteOfHour minute (0-59) to execute each hour
     * @return task id for the scheduled task. The task id can be used to un-schedule
     *   the task at any time.
     */
    public final int scheduleMonthlyExecution(
            iTask task,
            int dayOfMonth,
            int hourOfDay,
            int minuteOfHour)
    {
        // make the task schedule-able w/ one of the mixins
        RecurringTask monthlyTask = new MonthlyScheduleMixin(task, dayOfMonth, hourOfDay, minuteOfHour);

        // schedule the task for execution
        return this.scheduleTask(monthlyTask, -1);
    }

    /**
     * Thread-safe method schedules the task to run monthly on the specified occurrence
     * of the specified day of week.
     * If the task is unable to execute at the scheduled time, then TaskScheduler will
     * continue to try until successful.
     * <p/>
     * To use this task the developer will need to create a <i>Task</i> class by
     * implementing the {@link iTask} interface, then call this method to execute that
     * task every day. Any Task can be scheduled multiple times with any interval.
     * <p/>
     * Unlike the {@link #scheduleIntervalExecution(iTask, long, int, IntervalType, String...)
     * schedule interval} method, this method does not have a mechanism for executing
     * the task immediately (or with a short delay). The purpose of <code>scheduleMonthlyExecution</code>
     * is to execute at a scheduled time. If you need to perform an initial execution before
     * letting the task run on its schedule, then code an additional one-time execution
     * of the same task using {@link #scheduleOneTimeExecution(iTask, long, String...).
     * <p/>
     * Email address(es) to notify if the task fails can be associated with this task by
     * calling the TaskScheduler {@link #setTaskSpecificAddresses(int, String...) method with
     * the taskId returned from this method.
     *
     * @param task the class that performs the work. This can be any class
     *   that implements iTask, and the same task object can be scheduled multiple times.
     * @param dayOfWeek enum constant for day-of-week to schedule as an occurrence in the month
     * @param dayOccurrence enum constant representing the occurrence w/in the month of the 'dayOfWeek'.
     *   For instance, setting dayOccurrence=DayOccurrence.LAST and dayOfWeek=SUNDAY will
     *   execute the task the last Sunday of every month.
     * @param hourOfDay hour (0-23) to execute each day
     * @param minuteOfHour minute (0-59) to execute each hour
     * @return task id for the scheduled task. The task id can be used to un-schedule
     *   the task at any time.
     * @see DayOfWeek
     * @see DayOccurrence
     */
    public final int scheduleMonthlyExecution(
            iTask task,
            DayOfWeek dayOfWeek,
            DayOccurrence dayOccurrence,
            int hourOfDay,
            int minuteOfHour)
    {
        // make the task schedule-able w/ one of the mixins
        RecurringTask monthlyTask = new MonthlyScheduleMixin(task, dayOfWeek, dayOccurrence, hourOfDay, minuteOfHour);

        // schedule the task for execution
        return this.scheduleTask(monthlyTask, -1);
    }

    /**
     * Thread-safe method schedules the task to run every year at the specified day and time.
     * If the task is unable to execute at the scheduled time, then TaskScheduler will
     * continue to try until successful.
     * <p/>
     * To use this task the developer will need to create a <i>Task</i> class by implementing
     * the {@link iTask} interface, then call this method to execute that task every year.
     * Any Task can be scheduled multiple times with any interval.
     * <p/>
     * Unlike the {@link #scheduleIntervalExecution(iTask, long, int, IntervalType, String...)
     * schedule interval} method, this method does not have a mechanism for executing
     * the task immediately (or with a short delay). The purpose of <code>scheduleYearlyExecution</code>
     * is to execute at a scheduled time. If you need to perform an initial execution before
     * letting the task run on its schedule, then code an additional one-time execution
     * of the same task using {@link #scheduleOneTimeExecution(iTask, long, String...).
     * <p/>
     * Email address(es) to notify if the task fails can be associated with this task by
     * calling the TaskScheduler {@link #setTaskSpecificAddresses(int, String...) method with
     * the taskId returned from this method.
     *
     * @param task the class that performs the work. This can be any class
     *   that implements iTask, and the same task object can be scheduled multiple times.
     * @param monthOfYear enum constant representing one of the months in the year
     * @param dayOfMonth set to execute on day 1-31 (29-31 will execute on last day of month
     *   for months with less than those number of days), or set to constant TaskScheduler.LAST_DAY_OF_MONTH
     *   with (optionally) a day offset. Examples: dayOfMonth=16 (executes on 16th of every
     *   month), dayOfMonth=31 (executes on last day of every month),
     *   dayOfMonth=TaskScheduler..LAST_DAY_OF_MONTH (executes on last day of every month),
     *   dayOfMonth=TaskScheduler..LAST_DAY_OF_MONTH-1 (executes on next-to-last day of month).
     * @param hourOfDay hour (0-23) to execute on the specified dayOfMonth
     * @param minuteOfHour minute (0-59) to execute on the specfied hour
     * @return task id for the scheduled task. The task id can be used to un-schedule
     *   the task at any time.
     */
    public final int scheduleYearlyExecution(
            iTask task,
            MonthOfYear monthOfYear,
            int dayOfMonth,
            int hourOfDay,
            int minuteOfHour)
    {
        // make the task schedule-able w/ one of the mixins
        RecurringTask yearlyTask = new YearlyScheduleMixin(task, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour);

        // schedule the task for execution
        return this.scheduleTask(yearlyTask, -1);
    }

    /**
     * This is a general purpose method that schedules any subclass of {@link ScheduledTask},
     * especially including custom written task classes (when implementing iTask doesn't suffice).
     * It is generally recommended to use one of the other <i>schedule</i> methods in this class
     * for scheduling {@link iTask} tasks that do not extend ScheduledTask.
     * <p/>
     * To use this method, the developer will need to extend either ScheduledTask, for a one-time
     * execution task, or {@link RecurringTask} for tasks that execute on a recurring schedule.
     * But it is much easier for the developer to just implement {@link iTask} and schedule it
     * using the other <code>schedule...</code> methods in this class.
     * <p/>
     * Email address(es) to notify if the task fails can be associated with this task by
     * calling the TaskScheduler {@link #setTaskSpecificAddresses(int, String...) method with
     * the taskId returned from this method.
     *
     * @param task any ScheduledTask subclass.
     * @param initialDelayInMillis the delay (in milliseconds) before the very first
     *   execution. If the initialDelayInMillis=0, then the task executes immediately
     *   when it is initialized, or when the TaskScheduler first starts, whichever
     *   comes last. If initialDelayInMillis is < 0, then the initial delay is
     *   ignored and the task's internal scheduling interval is used instead (if
     *   an internal scheduling was coded).
     * @return task id for the scheduled task. The task id can be used to reference the
     *   task to pause or remove the task, and perform other actions.
     */
    public final int scheduleCustomTaskExecution(ScheduledTask task, long initialDelayInMillis) {
        return this.scheduleTask(task, initialDelayInMillis);
    }

    /**
     * Schedules the tasks defined in a properly-formatted XML configuration file.
     * The required format is described in the {@link SchedulerConfig} class javadoc.
     *
     * @param is InputStream for reading the XML configuration file.
     * @return array of task ids for the scheduled tasks
     * @throws SchedulerConfigException if there's an error creating tasks from the InputStream
     * @see SchedulerConfig
     */
    public final int[] scheduleTasks(InputStream is) {
        // read config and schedule the tasks
        SchedulerConfig schedulerConfig = new SchedulerConfig(is);
        return this.scheduleTasks(schedulerConfig);
    }

    /**
     * This method can be used by subclasses to pass in a different "flavor"
     * SchedulerConfig object. The {@link #scheduleTasks(InputStream)} method
     * uses the SchedulerConfig with the empty constructor. If the application
     * only needs to schedule tasks defined in the xml config file, then the
     * public <code>scheduleTasks(InputStream)</code> method is the one to use.
     *
     * @param schedulerConfig SchedulerConfig object created by the TaskScheduler
     * @return array of task ids for the scheduled tasks
     * @throws SchedulerConfigException if there's an error creating tasks
     * @see SchedulerConfig
     */
    protected int[] scheduleTasks(SchedulerConfig schedulerConfig) {
        List<TaskConfig> tasks = schedulerConfig.getTaskConfigs();
        ClassLoader classLoader = schedulerConfig.getClassLoader();
        int taskId = -1;
        int index = 0;
        int[] taskIds = new int[tasks.size()];
        try {
            for (TaskConfig taskConfig : tasks) {
                // schedule task
                taskId = this.scheduleTask(taskConfig, classLoader);

                // associate email support addresses
                String commaDelimitedString = schedulerConfig.getEmails();
                String[] supportEmails = JavaUtils.convertFromCommaDelimitedString(commaDelimitedString);
                if (supportEmails != null && supportEmails.length > 0) {
                    this.setTaskSpecificAddresses(taskId, supportEmails);
                }

                taskIds[index++] = taskId;
            }
        } catch (SchedulerConfigException ex) {
            // cancel the previously scheduled tasks
            for (int i=0; i<index; i++) {
                try { this.cancelTask(taskIds[i]); } catch (Exception ignore) {}
            }
            // re-throw the exception
            throw ex;
        }

        return taskIds;
    }

    /**
     * This method can be used (or overridden) by subclasses to schedule a task with
     * a {@link TaskConfig} object, which can parsed from any config file
     * that declares a scheduler task.
     *
     * @param taskConfig configuration object contains a task classname along
     *   with the information for scheduling it.
     * @return the task id after the task is scheduled.
     */
    protected int scheduleTask(TaskConfig taskConfig, ClassLoader classLoader) {

        int taskId = -1;

        String interval = taskConfig.getInterval();
        long delay = taskConfig.getInitialDelayInMillis();
        if (interval.equals("CUSTOM")) {
            // all custom tasks have ScheduledTask as an ancestor
            ScheduledTask task = null;
            String classname = null;
            try {
                classname = taskConfig.getClassName();
                Class<?> taskClass = classLoader.loadClass(classname);
                task = (ScheduledTask) taskClass.newInstance();
            } catch (Exception ex) {
                String msg = "error instantiating custom task: "+classname;
                throw new SchedulerConfigException(msg, ex);
            }
            // schedule custom task
            if (delay < 0) delay = 0L; //ignore invalid values
            log.debug("scheduling custom task: "+classname+" with delay: "+delay+" ms");
            taskId = this.scheduleTask(task, delay);
        } else {
            // all non-custom tasks implement iTask
            // if <task> is declared in scheduler-config.xml, then the task
            // is already instantiated and saved inside the taskConfig
            iTask task = taskConfig.getTask();
            if (task == null) {
                // otherwise, create the task here using its classname
                String classname = null;
                try {
                    classname = taskConfig.getClassName();
                    Class<?> taskClass = classLoader.loadClass(classname);
                    task = (iTask) taskClass.newInstance();
                } catch (Exception ex) {
                    String msg = "error instantiating iTask task: "+classname;
                    throw new SchedulerConfigException(msg, ex);
                }
            }
            if (log.isDebugEnabled()) {
                String msg = "scheduling task ("+task.getClass().getName()+") with interval: "+interval;
                if (delay >= 0) {
                    msg += " and initial delay: "+delay+" ms";
                }
                log.debug(msg);
            }
            MonthOfYear monthOfYear = null;
            DayOfWeek dayOfWeek = null;
            DayOccurrence dayOccurrence = null;
            if (interval.equals("ONE_TIME")) {
                // schedule one time execution
                int year = taskConfig.getYear();
                if (year > 0) {
                    // scheduling one-time execution by date-time
                    try {
                        monthOfYear = MonthOfYear.valueOf(taskConfig.getMonthOfYear());
                    } catch (NullPointerException ex) {
                        throw new SchedulerConfigException("required 'monthOfYear' not specified: "+taskConfig, ex);
                    }
                    int day = taskConfig.getDayOfMonthInt();
                    int hour = taskConfig.getHours();
                    int minute = taskConfig.getMinutes();
                    try {
                        taskId = this.scheduleOneTimeExecution(task, year, monthOfYear, day, hour, minute);
                    } catch (IllegalArgumentException ex) {
                        throw new SchedulerConfigException("config error for task: "+taskConfig, ex);
                    }
                } else {
                    // scheduling one-time execution by delay-in-millis
                    if (delay < 0) delay = 0L; //ignore invalid values
                    taskId = this.scheduleOneTimeExecution(task, delay);
                }
            } else if (interval.equals("HOURS")) {
                // schedule HOUR interval task
                int hours = taskConfig.getHours();
                taskId = this.scheduleIntervalExecution(task, delay, hours, IntervalType.HOURS);
            } else if (interval.equals("MINUTES")) {
                // schedule MINUTE interval task
                int minutes = taskConfig.getMinutes();
                taskId = this.scheduleIntervalExecution(task, delay, minutes, IntervalType.MINUTES);
            } else if (interval.equals("SECONDS")) {
                // schedule second interval task
                int seconds = taskConfig.getSeconds();
                taskId = this.scheduleIntervalExecution(task, delay, seconds, IntervalType.SECONDS);
            } else if (interval.equals("MILLISECONDS")) {
                // schedule millisecond interval task
                int millis = taskConfig.getMilliseconds();
                taskId = this.scheduleIntervalExecution(task, delay, millis, IntervalType.MILLISECONDS);
            } else if (interval.equals("DAILY")) {
                // schedule DAILY interval task
                int hour = taskConfig.getHours();
                int minute = taskConfig.getMinutes();
                int second = taskConfig.getSeconds();
                taskId = this.scheduleDailyExecution(task, hour, minute, second);
            } else if (interval.equals("WEEKLY")) {
                // schedule WEEKLY interval task
                try {
                    dayOfWeek = DayOfWeek.valueOf(taskConfig.getDayOfWeek());
                } catch (NullPointerException ex) {
                    throw new SchedulerConfigException("required 'dayOfWeek' not specified: "+taskConfig, ex);
                }
                int hour = taskConfig.getHours();
                int minute = taskConfig.getMinutes();
                taskId = this.scheduleWeeklyExecution(task, dayOfWeek, hour, minute);
            } else if (interval.equals("MONTHLY")) {
                // schedule MONTHLY interval task
                int day = taskConfig.getDayOfMonthInt();
                int hour = taskConfig.getHours();
                int minute = taskConfig.getMinutes();
                if (day > 0 || day <= TaskScheduler.LAST_DAY_OF_MONTH) {
                    // using 1st Monthly scheduler technique - dayOfMonth
                    taskId = this.scheduleMonthlyExecution(task, day, hour, minute);
                } else {
                    // using 2nd Monthly scheduler technique - dayOfWeek occurrence w/in month
                    try {
                        dayOfWeek = DayOfWeek.valueOf(taskConfig.getDayOfWeek());
                    } catch (NullPointerException ex) {
                        throw new SchedulerConfigException("required 'dayOfWeek' not specified: "+taskConfig, ex);
                    }
                    try {
                        dayOccurrence = DayOccurrence.valueOf(taskConfig.getDayOccurrence());
                    } catch (NullPointerException ex) {
                        throw new SchedulerConfigException("required 'dayOccurrence' not specified: "+taskConfig, ex);
                    }
                    taskId = this.scheduleMonthlyExecution(task, dayOfWeek, dayOccurrence, hour, minute);
                }
            } else if (interval.equals("YEARLY")) {
                // schedule YEARLY interval task
                try {
                    monthOfYear = MonthOfYear.valueOf(taskConfig.getMonthOfYear());
                } catch (NullPointerException ex) {
                    throw new SchedulerConfigException("required 'monthOfYear' not specified: "+taskConfig);
                }
                int day = taskConfig.getDayOfMonthInt();
                int hour = taskConfig.getHours();
                int minute = taskConfig.getMinutes();
                taskId = this.scheduleYearlyExecution(task, monthOfYear, day, hour, minute);
            }
        }

        return taskId;
    }

    /*
     * All of the schedule... methods lead to here, so all tasks are scheduled
     * and rescheduled through this method.
     *
     * @param task any ScheduledTask subclass.
     * @param initialDelayInMillis the delay (in milliseconds) before the very first
     *   execution. If the initialDelayInMillis=0, then the task executes immediately
     *   when it is initialized, or when the TaskScheduler first starts, whichever
     *   comes last. If initialDelayInMillis is < 0, then the initial delay is
     *   ignored and the task's internal scheduling interval is used instead.
     * @return task id for the scheduled task. The task id can be used to reference the
     *   task to pause or remove the task, and perform other actions.
     */
    private int scheduleTask(ScheduledTask task, long initialDelayInMillis) {
        int taskId = task.getTaskId();
        boolean newTask = (taskId < 0);
        if (newTask) {
            // create and set a new id on the task
            this.assignTaskId(task);
        }
        // scheduled tasks are put on the map until they finish executing
        this.taskMap.put(new Integer(taskId), task);
        // call getter method in case it was overridden by TaskScheduler subclass
        SchedulerContext context = this.getSchedulerContext();
        // prepare the task and add it to the execution queue
        task.prepare(this, context, initialDelayInMillis);
        if (newTask) {
            log.info("scheduling new TASK for execution: "+task);
        } else {
            log.debug("rescheduling TASK: "+task);
        }
        this.internalQueue.add(task);
        return taskId;
    }

    /*
     * Creates a new task id and sets it on the new task. This method
     * is synchronized because task id is critical for differentiating
     * between tasks.
     */
    private synchronized void assignTaskId(ScheduledTask task) {
        int taskId = this.nextTaskId % Integer.MAX_VALUE;
        this.nextTaskId = taskId + 1;
        task.setTaskId(taskId);
    }

    /**
     * Cancels the specified task and removes it from the Scheduler so
     * it cannot execute any more.
     * <p/>
     * This method does not need to be synchronized because all the structures
     * are Thread-safe.
     *
     * @param taskId task id assigned and returned when the task was scheduled
     */
    public final void cancelTask(int taskId) {
        Integer id = new Integer(taskId);
        ScheduledTask task = this.taskMap.remove(id);
        if (task != null) {
            // first pause the task so it does not execute
            task.pause();
            // remove item from the queue
            log.info("XX canceling task: "+task);
            boolean removed = this.internalQueue.remove(task);
            if (!removed) {
                log.debug("XX task not in queue - so it must be executing");
                // if not in queue then must be executing
                Future<?> taskHandle = this.executingTasksMap.remove(id);
                if (taskHandle != null) {
                    // cancel current and future
                    // executions of this task
                    log.info("XX cancelling task: "+id);
                    taskHandle.cancel(true);
                }
            }
        }
    }

    /**
     * Pauses the specified task so it does not execute on its scheduled
     * time(s), but does not interrupt the task's schedule. If the task is
     * no longer scheduled and/or does not exist, then this method has
     * no effect.
     *
     * @param taskId task id for the task to pause
     */
    public final void pauseTask(int taskId) {
        Integer id = new Integer(taskId);
        ScheduledTask task = this.taskMap.get(id);
        if (task != null) {
            // pause the task
            task.pause();
        }
    }

    /**
     * Resumes a paused task to restart execution. If the 'executeImmediately' param
     * is passed as true, then the task will execute immediately, then resume its
     * schedule. If the 'executeImmediately' param is false, then the task will just
     * continue its normal schedule. If the task is no longer scheduled and/or does
     * not exist, then this method has no effect.
     *
     * @param taskId task id for the task to resume
     * @param executeImmediately set to true to execute the task immediately, then
     *   continue with its schedule. Set to false to just continue the schedule.
     */
    public final void resumeTask(int taskId, boolean executeImmediately) {
        Integer id = new Integer(taskId);
        ScheduledTask task = this.taskMap.get(id);
        if (task != null) {
            // take the task out of paused mode so it
            // executes on its next scheduled time
            task.resume();
            if (executeImmediately) {
                // remove task from the queue (if applicable)
                this.internalQueue.remove(task);
                // re-schedule the task to run now
                this.scheduleTask(task, 0);
            }
        }
    }

    /**
     * Implements {@link iEventListener} to listen for task lifecycle events.
     * <p/>
     * This method does not need to be synchronized because all the structures
     * are Thread-safe.
     */
    public void handleEvent(TaskEvent event) {
        // listen for LifecycleEvent & remove from the task maps when needed
        if (event instanceof TaskLifecycleEvent) {
            TaskLifecycleEvent lifecycleEvent = (TaskLifecycleEvent) event;
            TaskInternalState nextState = lifecycleEvent.getNextState();
            TaskProxy taskProxy = lifecycleEvent.getTaskProxy();
            Integer id = new Integer(taskProxy.getTaskId());
            if (nextState == TaskInternalState.INACTIVE) {
                // task just finished executing - remove from collections
                this.taskMap.remove(id);
                this.executingTasksMap.remove(id);
                log.debug("LIFECYCLE - task ("+taskProxy+") ends execution");
            } else if (nextState == TaskInternalState.EXECUTING) {
                // task just started executing
                log.debug("LIFECYCLE - task ("+taskProxy+") begins execution");
            }
        }
    }

    /**
     * Returns the executing task corresponding to the taskId. Used by subclasses to
     * access tasks.
     *
     * @param taskId task id for the desired task
     * @return task corresponding to the id, or null if the task is not executing
     * @see #getScheduledTask(int)
     */
    protected final Future<?> getExecutingTask(int taskId) {
        return executingTasksMap.get(new Integer(taskId));
    }

    /**
     * Returns true if this Scheduler's processing Thread is running, false
     * if the processing Thread is not running and needs to be started.
     * @see #start()
     */
    public boolean isRunning() {
        return (this.taskProcessor != null &&
                this.taskProcessor.getState() != Thread.State.NEW &&
                this.taskProcessor.getState() != Thread.State.TERMINATED);
    }

    /**
     * Starts this TaskScheduler and returns a
     * reference to itself for chaining.
     */
    public TaskScheduler start() {
        // add the error handler
        // (delayed because custom handler can be added during setup)
        TaskUtils.addEventListener(this.errorHandler);
        // start the processing thread
        log.info("Starting TaskScheduler");
        this.taskProcessor.start();
        return this;
    }

    /**
     * Stops this TaskScheduler. Note that all executing and scheduled tasks
     * will be cancelled when this method is called.
     */
    public void stop() {
        log.info("Stopping TaskScheduler");
        this.taskProcessor.interrupt();
    }

    /**
     * Returns task report
     */
    public String getState() {
        StringBuilder sb = new StringBuilder("TaskScheduler state: {");
        int count = 0;
        synchronized (this.taskMap) {
            for (Integer id : taskMap.keySet()) {
                ScheduledTask task = taskMap.get(id);
                if (count++ > 0) sb.append(", ");
                sb.append(task);
            }
        }
        sb.append('}');
        return sb.toString();
    }

    /**
     * ------------------------------------------------
     * Processing Thread executes tasks in the
     * internalQueue.
     * ------------------------------------------------
     */
    private class ProcessingThread extends Thread {

        private DelayQueue<ScheduledTask> queue;
        private ExecutorService executor;

        public ProcessingThread(String threadName, DelayQueue<ScheduledTask> internalQueue) {
            super(threadName);
            this.queue = internalQueue;

            // start ExecutorService to execute the Tasks
            this.executor = Executors.newCachedThreadPool(); //unbounded, but efficient, Thread pool
        }

        @Override
        public void run() {
            while (!this.isInterrupted()) {
                try {
                    // get next task to schedule
                    // - this will wait if no tasks
                    ScheduledTask nextTask = queue.take(); //queue is thread-safe
                    // submitted tasks execute asynchronously
                    Future<?> taskHandle = this.executor.submit(nextTask);
                    // put handle in map in case it needs to be cancelled
                    executingTasksMap.put(nextTask.getTaskId(), taskHandle);
                } catch (InterruptedException ex) {
                    // must interrupt again to break loop, because the interrupt
                    // status was cleared when the exception was thrown
                    this.interrupt();
                }
            }
            // exit cleanly
            this.shutdown();
        }

        private void shutdown() {
            this.executor.shutdownNow();
            this.queue.clear();
        }

    }

}
