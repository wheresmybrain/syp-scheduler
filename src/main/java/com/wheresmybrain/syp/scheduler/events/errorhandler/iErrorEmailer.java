package com.wheresmybrain.syp.scheduler.events.errorhandler;

/**
 * Defines an interface for injecting a class that will be used to send email
 * notifications when a task fails. The object that gets injected should be fully
 * initialized, connected to the mail server and ready to send email!
 * <p/>
 * The Scheduler's framework automatically limits the frequency of emails sent
 * as error notifications to one notification per hour per recipient for the same
 * failing task. This is necessary because, for example, if something is broken
 * in a task that executes once per minute, then you don't want the notification
 * sent to the same recipient every minute!
 */
public interface iErrorEmailer {

    /**
     * This method will be called by the Scheduler's {@link TaskErrorHandler} to
     * send email notifications when a task fails. Note that the TaskErrorHandler
     * will only call this method a maximum of once per hour for the same failing
     * task to make sure recipients aren't inundated with error emails!
     *
     * @param subject The text your implementation should use as the email subject line.
     *   The subject will include a timestamp and the task name. Also, the app and platform
     *   name will be included if they are available.
     * @param messageText The text of the error notification to send. This will include
     *   the stacktrace too if an Exception was thrown.
     * @param emailAddresses One or more String email addresses of error message recipients.
     * @throws Exception if there is an error sending the message. This information will
     *   be logged.
     */
    void sendErrorNotification(
            String subject,
            String messageText,
            String... emailAddresses) throws Exception;

}
