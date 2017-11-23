package com.wheresmybrain.syp.scheduler.events.errorhandler;

import com.wheresmybrain.syp.scheduler.TaskEvent;
import com.wheresmybrain.syp.scheduler.SypScheduler;
import com.wheresmybrain.syp.scheduler.TaskUtils;
import com.wheresmybrain.syp.scheduler.events.EventListener;
import com.wheresmybrain.syp.scheduler.utils.JavaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This error handler logs task errors as warning messages. It will also
 * send email error notifications if you injected an "error emailer" using the
 * {@link SypScheduler#injectErrorReporter(ErrorReporter, String, String, String...)} method.
 * <p/>
 * If you need additional behavior when tasks fail, then create a custom event listener,
 * as described in the {@link EventListener} javadoc, and implement it to listen
 * specifically for {@link TaskErrorEvent} events, which are the same events that this
 * TaskErrorHandler listens for.
 *
 * @author @author <a href="mailto:chris.mcfarland@gmail.com">Chris McFarland</a>
 */
public class TaskErrorHandler implements EventListener {

    private static Logger log = LoggerFactory.getLogger(TaskErrorHandler.class);
    private static final SimpleDateFormat timeOnlyFormatter = new SimpleDateFormat("HH:mm");

    private static final String EOL = System.getProperty("line.separator");

    private String appName;
    private String environmentName;
    private ErrorReporter errorEmailer;
    private EmailThrottle emailThrottler = new EmailThrottle();
    private String[] defaultSupportAddresses = new String[0];

    /**
     * Implements <i>EventListener</i> method to handle error events.
     * @see EventListener#handleEvent(TaskEvent)
     */
    public final void handleEvent(TaskEvent event) {
        if (event instanceof TaskErrorEvent) {
            TaskErrorEvent taskError = (TaskErrorEvent) event;
            this.handleError(taskError);
        }
    }

    /**
     * The default behavior is to log information about the task error and
     * send email notification if an errorEmailer is set. The information
     * is logged as an error message. Subclasses can override this method
     * to replace or add to the default behavior.
     */
    protected void handleError(TaskErrorEvent errorEvent) {
        String errorMessage = this.extractErrorInfo(errorEvent);
        log.error(errorMessage);
        // email error notification
        if (this.errorEmailer != null) {
            // build subject line
            String timestamp = timeOnlyFormatter.format(new Date());
            String taskClassname = errorEvent.getTaskProxy().getTaskClass().getName();
            StringBuilder sb = new StringBuilder("[").append(timestamp).append("]")
                    .append(" SCHEDULED TASK ERROR {").append(taskClassname)
                    .append(" - task #").append(TaskUtils.getTaskId()).append("}");
            if (this.appName != null) sb.append(" FOR APP: ").append(appName);
            if (this.environmentName != null) {
                sb.append(" (").append(environmentName).append("-").append(errorEvent.getTaskThreadName()).append(")");
            }
            String subject = sb.toString();
            // email error notification
            this.emailTheError(subject, errorMessage, errorEvent);
        }
    }

    /**
     * Extracts the information from the errorEvent, then formats and
     * returns it as a multi-line String suitable for logging or inclusion
     * in an email. The info included are appName and environmentName (if set
     * on this handler during initialization), information on the task
     * that errored, debug details (if set on the event), and stacktrace
     * (if exception set on the event).
     */
    protected final String extractErrorInfo(TaskErrorEvent errorEvent) {
        // extract info from the error
        String taskInfo = errorEvent.getTaskProxy().getTaskInfo();
        Exception taskException = errorEvent.getTaskException();
        String[] debugInfo = errorEvent.getTaskProxy().getDebugState();
        // log task error as a warning
        StringBuilder sb = new StringBuilder("TASK ERROR");
        if (this.appName !=  null) sb.append(" FOR APP: ").append(this.appName);
        if (this.environmentName != null) sb.append(" (").append(environmentName).append(")");
        sb.append(EOL).append(taskInfo);
        if (debugInfo != null && debugInfo.length > 0) {
            sb.append(EOL).append("DEBUG INFO:");
            for (String info : debugInfo) {
                sb.append(EOL).append("    ").append(info);
            }
        }
        if (taskException != null) {
            sb.append(EOL).append("STACKTRACE:");
            sb.append(EOL).append(JavaUtils.getStacktrace(taskException));
        }
        return sb.toString();
    }

    /**
     * This method is can be called by the parent SypScheduler if the error
     * emailer is injected at that level, which is the only way to set the
     * error emailer into the default TaskErrorHandler.
     * Sets a class that can be used to send error notifications via email. If
     * no "error emailer" is injected, then the errors will only be logged.
     *
     * @param errorEmailer implementation of the ErrorReporter interface
     * @param supportAddresses the set of global support email addresses to receive
     *   notification of errors for every task. Additional addresses might be added
     *   on a task-by-task basis.
     */
    public void setErrorEmailer(ErrorReporter errorEmailer, String... supportAddresses) {
        this.errorEmailer = errorEmailer;
        if (supportAddresses != null) {
            this.defaultSupportAddresses = supportAddresses;
        }
    }

    /**
     * Makes the error emailer available to subclasses if needed.
     * @return the error emailer set on this handler, or null if
     * no emailer was set.
     */
    protected final ErrorReporter getErrorEmailer() {
        return this.errorEmailer;
    }

    /**
     * Sets the name of the application running this scheduler. Setting this
     * attribute is optional, but troubleshooting errors will be much easier
     * if this is set.
     */
    public void setAppName(String appName) {
        this.appName = appName;
    }

    /**
     * Returns the name of the application running this scheduler, or
     * null if no appName was set.
     */
    protected String getAppName() {
        return this.appName;
    }

    /**
     * Sets the name of the environment or server that the application is running on.
     * Setting this attribute is optional, but troubleshooting errors will be much
     * easier if this is set.
     */
    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
    }

    /**
     * Returns the name of the environment or server that the application is
     * running on, or null if no environmentName was set.
     */
    protected String getEnvironmentName() {
        return this.environmentName;
    }

    /*
     * Sends error notifications when tasks fail.
     */
    private void emailTheError(String subject, String errorMessage, TaskErrorEvent taskError) {
        log.debug("    >> emailing the error");
        String[] emailAddresses = this.defaultSupportAddresses;
        try {
            // extract info from the error
            int taskId = taskError.getTaskProxy().getTaskId();
            if (taskError.getEmails() != null) {
                emailAddresses = (String[]) this.combineArrays(emailAddresses, taskError.getEmails());
            }
            if (emailAddresses.length > 0) {
                // we are limiting message frequency
                if (this.emailThrottler.okayToSendError(taskId, emailAddresses[0])) {
                    // use the error emailer to send the message
                    this.errorEmailer.sendErrorNotification(subject, errorMessage, emailAddresses);
                }
            }
        } catch (Exception e) {
            // log & swallow failed notification attempt
            String err = "ERROR sending email (subject: "+subject+") to recipients: "+ Arrays.toString(emailAddresses)+" - see stacktrace:";
            log.error(err, e);
        }
    }

    /*
     * Combines 2 arrays into 1. Both arrays must be non-null and declared to hold the same
     * element type. The combined array is returned as Object, so it needs to be cast to the
     * same array type as what is passed in.
     *
     * @throws NullPointerException if either array is null
     * @throws IllegalArgumentException if either object is not array or same array type
     * @return If both array objects are null, then returns null. Else, if either array object
     *   is null, then returns the other. If both array objects are non-null, then returns the
     *   combined array.
     */
    private Object combineArrays(Object array1, Object array2) {
        if (array1 == null) {
            if (array2 == null) {
                return null;
            } else {
                return array2;
            }
        } else if (array2 == null) {
            return array1;
        }
        Class<?> array1Class = array1.getClass();
        Class<?> array2Class = array2.getClass();
        if (array1Class.isArray() && array2Class.isArray() && array1Class==array2Class) {
            int array1Length = Array.getLength(array1);
            int array2Length = Array.getLength(array2);
            int newLength = array1Length + array2Length;
            Object newArray = Array.newInstance(array1Class.getComponentType(), newLength);
            System.arraycopy(array1, 0, newArray, 0, array1Length);
            System.arraycopy(array2, 0, newArray, array1Length, array2Length);
            return newArray;
        } else {
            String err = "Both "+array1Class.getSimpleName()+" and "+
                    array2Class.getSimpleName()+" must be arrays of the same type!";
            throw new IllegalArgumentException(err);
        }
    }

    /*
     * Used by emailTheError method to "throttle" the frequency of error messages.
     */
    private static class EmailThrottle {

        // defines the minimum interval between
        // notifications for the same task error
        private static final int MIN_INTERVAL_IN_MINS = 60;

        // screenerMap:key=taskId, value=innerMap
        // timestampsMap=emailAddress, value=last notification time
        private Map<Integer,Map<String,Date>> screenerMap = new HashMap<Integer,Map<String,Date>>();

        // timestamp used to clean out old errors
        private Date lastCleanup = new Date();

        EmailThrottle() {
            //defining constructor avoids compiler warnings
        }

        /**
         * Returns true if it is okay to send error email to this address.
         * Email with same taskId cannot go to the same address more than once per hour.
         */
        public boolean okayToSendError(int taskId, String emailAddress) {
            boolean returnValue = false;
            Date currentTime = new Date();
            Integer task = new Integer(taskId);
            Map<String,Date> timestampsMap = this.screenerMap.get(task);
            if (timestampsMap == null) {
                // first email for this subject - create one with current notification time
                timestampsMap = new HashMap<String,Date>();
                timestampsMap.put(emailAddress, currentTime);
                this.screenerMap.put(task, timestampsMap);
                returnValue = true;
                log.debug("XX SENDING email for task (#"+taskId+") - first occurrence for this error");
            } else {
                Date lastNotification = timestampsMap.get(emailAddress);
                if (lastNotification == null) {
                    // first email for this recipient
                    timestampsMap.put(emailAddress, currentTime);
                    returnValue = true;
                } else {
                    // check if last notification was 60 or more minutes ago
                    if (this.getIntervalInMins(lastNotification, currentTime) >= MIN_INTERVAL_IN_MINS) {
                        timestampsMap.put(emailAddress, currentTime); //update notification time
                        returnValue = true;
                    }
                }
                log.debug("XX "+((returnValue)?"SENDING":"NOT SENDING")+
                        " email for task (#"+taskId+") - last sent: "+
                        timeOnlyFormatter.format(lastNotification));

                // clean up old errors (if needed)
                this.cleanup(currentTime);
            }

            return returnValue;
        }

        /*
         * Returns the number of minutes between two times, rounded down
         * to the nearest minute.
         * @throws IllegalArgumentException if either time is null, or if startTime
         *   is after endTime.
         */
        private int getIntervalInMins(Date startTime, Date endTime) {

            if (startTime == null) {
                throw new IllegalArgumentException("startTime is null.");
            } else if (endTime == null) {
                throw new IllegalArgumentException("endTime is null.");
            } else if (startTime.after(endTime)) {
                String start = timeOnlyFormatter.format(startTime);
                String end = timeOnlyFormatter.format(endTime);
                throw new IllegalArgumentException("startTime("+start+") is after endTime ("+end+".");
            }

            return (int) Math.floor((endTime.getTime() - startTime.getTime()) / 60000);
        }

        /*
         * cleans up screenerMap by removing entries older than 6 hrs
         */
        private void cleanup(Date currentTime) {
            int lastRun = this.getIntervalInMins(this.lastCleanup, currentTime);
            // run cleanup max once per hour
            if (lastRun >= 60) {
                Iterator<Integer> taskIter = this.screenerMap.keySet().iterator();
                while (taskIter.hasNext()) {
                    Integer task = taskIter.next();
                    Map<String,Date> addressMap = this.screenerMap.get(task);
                    Iterator<String> addressIter = addressMap.keySet().iterator();
                    while (addressIter.hasNext()) {
                        // remove entries > 6 hrs
                        String address = addressIter.next();
                        Date lastNotification = addressMap.get(address);
                        int minsSinceLastNotification = this.getIntervalInMins(lastNotification, currentTime);
                        if (minsSinceLastNotification > 360) {
                            addressIter.remove();
                            log.debug("XX cleaned up screenerMap for email ("+address+") and task: "+task);
                        }
                    }
                    // remove empty address Maps
                    if (addressMap.size() == 0) {
                        taskIter.remove();
                    }
                }
                // update timestamp
                this.lastCleanup = new Date();
            }
        }

    }

}
