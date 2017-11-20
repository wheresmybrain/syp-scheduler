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
 * This test task performs a database query then logs the results. But the
 * most important thing about this task is you can force it to fail by passing
 * the flag alwaysFail=true into the task's constructor. This task is programmed
 * to automatically remove itself if it fails 7 times in a row, which makes it
 * good for testing the error handling mechanism.
 *
 * @author Chris McFarland
 */
public class DatabaseTask implements Task {

    private static Logger log = LoggerFactory.getLogger(DatabaseTask.class);

    private DataSource dataSource;
    private int consecutiveFailures;
    private boolean alwaysFail;
    private String stateInfo;

    /**
     * Use this constructor when schedule from xml config file
     */
    public DatabaseTask() {
    }

    /**
     * Use this constructor to pass in a DataSource containing the connection pool.
     */
    public DatabaseTask(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Use this constructor when schedule in code
     */
    public DatabaseTask(boolean alwaysFail) {
        this.alwaysFail = alwaysFail;
    }

    /**
     * Called by config parser
     */
    public void setAlwaysFail(boolean alwaysFail) {
        this.alwaysFail = alwaysFail;
    }

    /**
     * @see Task#executeTask(SchedulerContext)
     */
    public void executeTask(SchedulerContext schedulerContext) throws Throwable {
        try {
            if (!this.alwaysFail) {
                Connection con = null;
                stateInfo = "database retrieval failed";

                StringBuilder sb = new StringBuilder("DatabaseTask result - MONTHS OF THE YEAR: ");
                PreparedStatement pstmt = null;
                try {
                    // execute the query
                    con = dataSource.getConnection();
                    String sql = "select attr1_tx from BABP.vcodetbl_data " +
                            "where codetbl_nm='MONTH' and bsns_sys_cd='GL' order by encode1_cd";
                    pstmt = con.prepareStatement(sql);
                    ResultSet rs = pstmt.executeQuery();
                    int count = 0;
                    while (rs.next()) {
                        String month = rs.getString(1);
                        if (count++ > 0) sb.append(", ");
                        sb.append(month);
                    }

                    this.stateInfo = "database retrieval successful";
                    this.consecutiveFailures = 0;
                } finally {
                    if (con != null) {
                        con.close();
                    }
                    if (pstmt != null) {
                        pstmt.close();
                    }
                }

                // display the results
                log.info(this + " - " + this.stateInfo);
                log.debug(sb.toString());

            } else {
                throw new RuntimeException("XX FORCING ERROR #"+(++consecutiveFailures)+" FOR TESTING PURPOSES!");
            }
        } catch (Exception ex) {
            if (this.consecutiveFailures < 7) {
                this.stateInfo = "THIS IS (EXPECTED) TASK FAILURE "+consecutiveFailures+"/7";
                throw ex;
            } else if (this.consecutiveFailures == 7) {
                // quit on 7th consecutive failure
                CancelEvent event = new CancelEvent();
                TaskUtils.fireEvent(event);
            } else {
                this.stateInfo = "THIS IS (UNEXPECTED) TASK FAILURE "+consecutiveFailures+"/7";
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
