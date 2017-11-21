package com.wheresmybrain.syp.scheduler.testtasks;

import com.wheresmybrain.syp.scheduler.SchedulerContext;
import com.wheresmybrain.syp.scheduler.TaskUtils;
import com.wheresmybrain.syp.scheduler.Task;
import com.wheresmybrain.syp.scheduler.testevents.CancelEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * This test task is programmed to automatically remove itself after it fails 7 times
 * in a row, which makes it good for testing (and demonstrating) the event handling
 * mechanism > on 7th failure the task will fire a custom <code>CancelEvent</code>,
 * which is handled by the {@link com.wheresmybrain.syp.scheduler.testevents.CancelEventHandler}.
 * <p/>
 * This task can be forced to fail by passing the flag alwaysFail=true in to the task's
 * constructor, or . If the flag alwaysFail=false, then this task just
 * writes a line to the log.
 *
 * @author Chris McFarland
 */
public class AlwaysFailTask implements Task {

    private static Logger log = LoggerFactory.getLogger(AlwaysFailTask.class);

    private int consecutiveFailures;
    private boolean alwaysFail;
    private String stateInfo;

    /**
     * Use this constructor when schedule this task in code
     */
    public AlwaysFailTask(boolean alwaysFail) {
        this.alwaysFail = alwaysFail;
    }

    /**
     * This constructor is called by the xml config parser
     */
    public AlwaysFailTask() {
    }

    /**
     * Called by the xml config parser
     */
    public void setAlwaysFail(boolean alwaysFail) {
        this.alwaysFail = alwaysFail;
    }

    /**
     * @see Task#executeTask(SchedulerContext)
     */
    public void executeTask(SchedulerContext schedulerContext) throws Throwable {
        try {
            if (alwaysFail) {
                throw new RuntimeException("XX FORCING ERROR #"+(++consecutiveFailures)+" FOR TESTING PURPOSES!");
            } else {
                log.info(String.format("This task (%s) is *not* set to fail", getClass().getSimpleName()));
            }
        } catch (Exception ex) {
            if (this.consecutiveFailures < 7) {
                this.stateInfo = "THIS IS (EXPECTED) TASK FAILURE #"+consecutiveFailures+"/7";
                throw ex;
            } else if (this.consecutiveFailures == 7) {
                // quit on 7th consecutive failure
                CancelEvent event = new CancelEvent();
                TaskUtils.fireEvent(event);
            } else {
                this.stateInfo = "THIS IS (UNEXPECTED) TASK FAILURE #"+consecutiveFailures+"/7";
                throw ex;
            }
        }
    }

    /**
     * @see Task#getDebugState()
     */
    public String[] getDebugState() {
        return new String[] {this.stateInfo};
    }

}
