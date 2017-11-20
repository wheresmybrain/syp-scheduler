package com.wheresmybrain.syp.scheduler.testtasks;

import com.wheresmybrain.syp.scheduler.SchedulerContext;
import com.wheresmybrain.syp.scheduler.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * Task for testing only.
 * This task prints the date, time and task name to the log.
 */
public class DateTimeTask implements Task {

    private static Logger log = LoggerFactory.getLogger(DateTimeTask.class);

    private static final String TEST_MESSAGE = "[DATE-TIME TASK] (%1$tb %1$td [%1$ta] at %1$tH:%1$tM:%1$tS.%1$tL) Task [%2$s]%n";

    private boolean executed;
    private String name;

    /**
     * @see Task#executeTask(SchedulerContext)
     */
    public void executeTask(SchedulerContext schedulerContext) throws Throwable {
        log.info(String.format(TEST_MESSAGE, new Date(), this.name));
        this.executed = true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getDebugState() {
        return executed ? new String[] {"Already Executed!"} : new String[] {"Did not Execute Yet!"};
    }

}
