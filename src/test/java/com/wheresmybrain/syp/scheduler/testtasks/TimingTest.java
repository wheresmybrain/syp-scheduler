package com.wheresmybrain.syp.scheduler.testtasks;

import com.wheresmybrain.syp.scheduler.SchedulerContext;
import com.wheresmybrain.syp.scheduler.Task;
import com.wheresmybrain.syp.scheduler.TaskUtils;
import com.wheresmybrain.syp.scheduler.utils.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * This task tests accuracy of the scheduling by printing out the execution time
 * and interval since the last execution (in hours, minutes, seconds and milliseconds).
 * Schedule this task across a variety of intervals and make sure to set the 'name' to
 * indicate what interval it was scheduled for so you know how accurate it is. For
 * example, set name = "45 SECOND INTERVAL" when you schedule the TimingTest task to
 * run every 45 seconds.
 *
 * @author Chris McFarland
 */
public class TimingTest implements Task {

    private static Logger log = LoggerFactory.getLogger(TimingTest.class);

    private static final String TEST_MESSAGE =
            "[TIMING TEST] (#%1$d) Task [%2$s] - time since last execution: %3$s%n";

    private String name;
    private Date lastExecution;
    private long executionNumber;

    /**
     * Writes the exact time since the last execution.
     */
    public void executeTask(SchedulerContext schedulerContext) throws Throwable {
        this.executionNumber += 1;
        Date thisExecution = new Date();
        int taskId = TaskUtils.getTaskId();
        String interval = (lastExecution != null) ?
                TimeUtils.getIntervalDescription(lastExecution, thisExecution) : "(first execution!)";
        log.info(String.format(TEST_MESSAGE, taskId, name, interval));
        this.lastExecution = thisExecution;
    }

    public String[] getDebugState() {
        String state = "%s failed on execution number: %d";
        return new String[] {String.format(state, name, executionNumber)};
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the 'name'.
     */
    @Override
    public String toString() {
        return this.name;
    }

}
