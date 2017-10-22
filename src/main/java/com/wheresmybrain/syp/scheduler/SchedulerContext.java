package com.wheresmybrain.syp.scheduler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores data shared between different tasks running under the TaskScheduler.
 * The SchedulerContext object exists inside the TaskScheduler instance, and the SchedulerContext
 * reference is passed into every task execution through its <code>executeTask(SchedulerContext)</code>
 * method. The SchedulerContext is a way to share data between different tasks, and with
 * a parent application (via the TaskScheduler <code>getSchedulerContext()</code> method).
 * <p/>
 * A custom context class can be created by extending this class and setting its classname
 * on TaskScheduler using the {@link TaskScheduler#setSchedulerContext(SchedulerContext)} method
 * before starting the Scheduler.
 *
 * @author @author <a href="mailto:chris.mcfarland@gmail.com">Chris McFarland</a>
 */
public class SchedulerContext {

    private Map<Object,Object> dataMap = new ConcurrentHashMap<>();

    /**
     * Stores data with the specified key in context. If context previously contained
     * a mapping for this key, the old data is replaced. The internal Map is synchronized
     * because it can be accessed concurrently by multiple task Threads.
     *
     * @param key key with which the specified data is to be associated.
     * @param data the value to store for sharing with other task executions.
     * @return the old data if already existed for the specified key, or null if no
     *   data existed for this key.
     */
    public Object storeData(Object key, Object data) {
        return dataMap.put(key, data);
    }

    /**
     * Returns data with the specied key if data is present. The internal Map is
     * synchronized because it can be accessed concurrently by multiple task Threads.
     *
     * @param key key whose mapping is to be returned from the map.
     * @return the data to which the map associates the key, or null if the map
     *   contained no data for this key.
     */
    public Object getData(Object key) {
        return dataMap.get(key);
    }

    /**
     * Removes and returns data with the specied key if data is present. Tasks that store
     * data should remove the data when no longer needed. The internal Map is synchronized
     * because it can be accessed concurrently by multiple task Threads.
     *
     * @param key key whose mapping is to be removed from the map.
     * @return the data to which the map previously associated the key, or null if the map
     *   contained no data for this key.
     */
    public Object removeData(Object key) {
        return dataMap.remove(key);
    }

    /**
     * Clears all stored data from the context.
     */
    public synchronized void clearContext() {
        this.dataMap.clear();
    }

}
