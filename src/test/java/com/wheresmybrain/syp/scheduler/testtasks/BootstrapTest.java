package com.wheresmybrain.syp.scheduler.testtasks;

import com.wheresmybrain.syp.scheduler.SypScheduler;
import com.wheresmybrain.syp.scheduler.testevents.CancelEventHandler;
import com.wheresmybrain.syp.scheduler.utils.ResourceReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.StringTokenizer;

/**
 * BootstrapTest is a simple Java application that tests the syp-scheduler.
 * <p/>
 *
 * @author Chris McFarland
 */
public class BootstrapTest {

    //-- static

    private static final String EOL = System.getProperty("line.separator");

    public static void main(String[] args) {
        try {
            BootstrapTest bootstrapTest = new BootstrapTest();
            bootstrapTest.start();
        } catch (Exception e) {
            System.out.println("BootstrapTest failed to start - see stacktrace");
            e.printStackTrace();
        }
    }

    //-- instance

    private final SypScheduler scheduler = new SypScheduler();

    /*
     * Example of configuring and starting syp-scheduler
     */
    private void start() throws IOException {
        // read config file as input stream
        // (it's in the classpath)
        ResourceReader reader = new ResourceReader();
        InputStream is = reader.readResource("scheduler-config.xml");
        // register event listeners
        this.scheduler.addEventListener(new CancelEventHandler(scheduler));
        // start scheduler and schedule
        // tasks (fr config file)
        this.printClasspath();
        this.scheduler.start();
        this.scheduler.scheduleTasks(is);
    }

    /**
     * display the classpath
     */
    private void printClasspath() {
        System.out.println("Classpath: ");
        System.out.println(EOL);
        String classpath = System.getProperty("java.class.path");
        StringTokenizer tokens = new StringTokenizer(classpath, ";");
        while (tokens.hasMoreTokens()) {
            String token = tokens.nextToken();
            System.out.println(token);
        }
        System.out.println(EOL);
    }
}
