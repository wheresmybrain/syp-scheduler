package com.wheresmybrain.syp.scheduler.config;

import org.apache.commons.digester.AbstractObjectCreationFactory;
import org.apache.commons.digester.Digester;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration object used for storing the data parsed from the XML configuration file.
 * This class represents the top-level &lt;scheduler&gt; tag in the file. The Apache Commons
 * Digester is used under the hood to parse the configuration file.
 * <p/>
 * Although the application developer should not need to use (or even know about) this class,
 * or the other config-specific classes in this package, here are the instructions for
 * parsing and using a scheduler configuration file:<br>
 * To parse the configuration info in the scheduler-config.xml file, instantiate this class
 * with the InputStream from the config file, and the file info will be automatically populated
 * inside this object. From that point you can fetch the {@link TaskConfig} objects, which
 * provide the information for scheduling each task. If they were set in the config file, the
 * email addresses for application support are also available.
 * <p/>
 * This is an example format for the XML config file:
 * <pre>
 * &lt;!-- Emails (optionally) defined at this level are set as support addresses on all the tasks in this file --&gt;
 * &lt;scheduler emails="john.doe@email.com, msmith@hotmail.com"&gt;
 *
 *     &lt;!-- One task class can be instantiated and scheduled many different ways --&gt;
 *     &lt;task-config className="my.pkg.DoSomethingTask" interval="HOURS" hours="2" initialDelayInMillis="10"/&gt;
 *     &lt;task-config className="my.pkg.DoSomethingTask" interval="MINUTES" minutes="30" initialDelayInMillis="10"/&gt;
 *     &lt;task-config className="my.pkg.DoSomethingTask" interval="SECONDS" seconds="120" initialDelayInMillis="0"/&gt;
 *     &lt;task-config className="my.pkg.DoSomethingTask" interval="MILLISECONDS" milliseconds="999" initialDelayInMillis="0"/&gt;
 *     &lt;task-config className="my.pkg.DoSomethingTask" interval="DAILY" hours="23" minutes="59" seconds="59"/&gt;
 *     &lt;task-config className="my.pkg.DoSomethingTask" interval="WEEKLY" dayOfWeek="MONDAY" hours="14" minutes="00"/&gt;
 *
 *     &lt;!-- Custom attributes get set automatically - DateTimeTask has custom attribute 'name' --&gt;
 *     &lt;task-config className="my.pkg.DateTimeTask" interval="ONE_TIME" initialDelayInMillis="60000"&gt;
 *         &lt;custom-attributes name="ONE_TIME #1"/&gt;
 *     &lt;/task-config&gt;
 *     &lt;task-config className="my.pkg.DateTimeTask" interval="ONE_TIME" year="2000" month="DECEMBER" dayOfMonth="25" hours="06" minutes="00"&gt;
 *         &lt;custom-attributes name="ONE_TIME #2"/&gt;
 *     &lt;/task-config&gt;
 *
 *     &lt;!-- Notice the use of keyword LAST_DAY_OF_MONTH used for 'dayOfMonth' settings --&gt;
 *     &lt;task-config className="my.pkg.DateTimeTask" interval="YEARLY" monthOfYear="FEBRUARY" dayOfMonth="LAST_DAY_OF_MONTH" hours="02" minutes="00"&gt;
 *         &lt;custom-attributes name="YEARLY"/&gt;
 *     &lt;/task-config&gt;
 *
 *     &lt;!-- The MONTHLY interval has multiple ways to schedule (also notice 'dayOfMonth' setting relative to LAST_DAY_OF_MONTH) --&gt;
 *     &lt;task-config className="my.pkg.DateTimeTask" interval="MONTHLY" dayOfMonth="15" hours="00" minutes="00"&gt;
 *         &lt;custom-attributes name="MONTHLY #1"/&gt;
 *     &lt;/task-config&gt;
 *     &lt;task-config className="my.pkg.DateTimeTask" interval="MONTHLY" dayOfMonth="LAST_DAY_OF_MONTH-1" hours="00" minutes="00"&gt;
 *         &lt;custom-attributes name="MONTHLY #2"/&gt;
 *     &lt;/task-config&gt;
 *     &lt;task-config className="my.pkg.DateTimeTask" interval="MONTHLY" dayOfWeek="TUESDAY" dayOccurrence="SECOND" hours="01" minutes="00"&gt;
 *         &lt;custom-attributes name="MONTHLY #3"/&gt;
 *     &lt;/task-config&gt;
 *     &lt;task-config className="my.pkg.DateTimeTask" interval="MONTHLY" dayOfWeek="TUESDAY" dayOccurrence="LAST" hours="01" minutes="00"&gt;
 *         &lt;custom-attributes name="MONTHLY #4"/&gt;
 *     &lt;/task-config&gt;
 *
 * &lt;/scheduler&gt;
 * </pre>
 *
 * @see TaskConfig
 */
public class SchedulerConfig {

    private static final String EOL = System.getProperty("line.separator");

    private String emails;
    private ClassLoader classLoader = ClassLoader.getSystemClassLoader();
    private ArrayList<TaskConfig> tasks = new ArrayList<>();

    /**
     * Constructor to parse the xml configuration file using
     * the default system ClassLoader.
     *
     * @param inputStream representing the configuration file to parse
     */
    public SchedulerConfig(InputStream inputStream) {
        this(null, inputStream);
    }

    /**
     * Constructor to parse the xml configuration file using
     * a custom ClassLoader.
     *
     * @param customClassLoader the ClassLoader to use to instantiate task objects. If this
     *   is null, then the default Java system ClassLoader is used.
     * @param inputStream representing the configuration file to parse
     */
    public SchedulerConfig(ClassLoader customClassLoader, InputStream inputStream) {
        if (customClassLoader != null) {
            this.classLoader = customClassLoader;
        }
        Digester parser = new Digester();
        parser.setValidating(false);
        parser.push(this);
        this.addRules(parser);

        try {
            parser.parse(inputStream);
        } catch (SAXException ex) {
            throw new RuntimeException("SchedulerConfig parsing error", ex);
        }  catch (IOException ex) {
            throw new RuntimeException("SchedulerConfig file read error", ex);
        }
    }

    /**
     * Define Digester rules for parsing scheduler-config.xml data.
     */
    private void addRules(Digester d) {

        // define digester rules
        String pattern = null;

        // SchedulerConfig (top-level) object
        pattern = "scheduler";
        d.addSetProperties(pattern);

        // TaskConfig objects
        pattern = "scheduler/task-config";
        d.addObjectCreate(pattern, TaskConfig.class);
        d.addSetProperties(pattern); //attribute names match property names
        d.addSetNext(pattern, "addTaskConfig" );

        // Task objects are instantiated if they have custom attributes to set
        pattern = "scheduler/task-config/custom-attributes";
        d.addFactoryCreate(pattern, new TaskObjectFactory(pattern, d));
        d.addSetProperties(pattern);
        d.addSetNext(pattern, "setTask" );
    }

    /**
     * Returns the emails comma-delimited String declared in the
     * scheduler-config.xml. If any emails were declared, then they
     * will be applied to all the declared tasks.
     */
    public String getEmails() {
        return emails;
    }

    /**
     * @param emails the emails to set. This can be a comma-delimited
     *   String, which is parsed out by the class that calls this method.
     */
    public void setEmails(String emails) {
        this.emails = emails; //delete!
    }

    /**
     * Add a TaskConfig object parsed from the xml file.
     * Corresponds to the XML <i>task</i> element.
     * @param taskConfig
     * @throws SchedulerConfigException if the TaskConfig contains a config error/omission
     */
    public void addTaskConfig(TaskConfig taskConfig) {
        taskConfig.validate(this.classLoader); //throws unchecked exception on error
        tasks.add(taskConfig);
    }

    public List<TaskConfig> getTaskConfigs() {
        return tasks;
    }

    /**
     * Returns the possibly-custom classLoader for this config.
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * lists the TaskConfig objects inside this SchedulerConfig
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TASK CONFIGS: ");
        int size = this.tasks.size();
        if (size > 0) {
            for (TaskConfig taskConfig : this.tasks) {
                sb.append(EOL).append("    > ").append(taskConfig.toString());
            }
        } else {
            sb.append("(none)");
        }
        return sb.toString();
    }

    /*
     * Creates Task objects using classname (from Digester stack) and app's ClassLoader.
     */
    private class TaskObjectFactory extends AbstractObjectCreationFactory {

        private Digester d;
        private String invokingPattern;

        public TaskObjectFactory(String invokingPattern, Digester d) {
            this.invokingPattern = invokingPattern;
            this.d = d;
        }

        @Override
        public Object createObject(Attributes attrs) throws Exception {
            Object returnObject = null;
            Class<?> classToInstantiate = null;
            String classname = null;
            try {
                // get classname from digester stack and instantiate
                // task using (optionally a custom) ClassLoader
                classname = ((TaskConfig)d.peek()).getClassName(); //classname in TaskConfig
                classToInstantiate = classLoader.loadClass(classname);
                returnObject = classToInstantiate.newInstance();
            } catch (ClassNotFoundException ex) {
                String msg = "class not found: "+classname;
                String currState = "{classname to instantiate="+classname+"}";
                throw new SchedulerConfigException(msg, ex).setCurrentState(currState);
            } catch (Exception ex) {
                String msg = "cannot instantiate class "+classname+" (invoked by pattern: "+invokingPattern+")";
                String currState = "{classname to instantiate="+classname+"}";
                throw new SchedulerConfigException(msg, ex).setCurrentState(currState);
            }

            return returnObject;
        }

    }

}
