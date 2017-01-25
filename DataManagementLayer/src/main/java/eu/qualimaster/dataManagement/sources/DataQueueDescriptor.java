package eu.qualimaster.dataManagement.sources;

import java.util.Queue;

import eu.qualimaster.dataManagement.sources.ReplayMechanism.ProfilingQueueItem;
/**
 * Data queue descriptor carrying the tuple id, the respective queue and its class type.
 * @author Cui Qin
 *
 * @param <T> the target object type
 */
public class DataQueueDescriptor<T> {
    private String id;
    private transient Queue<ProfilingQueueItem<T>> queue;
    private Class<T> cls;
    /**
     * Creates a data queue descriptor.
     * @param id the tuple id
     * @param queue the queue
     * @param cls the class type
     */
    public DataQueueDescriptor(String id, Queue<ProfilingQueueItem<T>> queue, Class<T> cls) {
        this.id = id;
        this.queue = queue;
        this.cls = cls;
    }
    /**
     * Returns the tuple id.
     * @return the tuple id
     */
    public String getId() {
        return id;
    }
    /**
     * Returns the queue.
     * @return the queue
     */
    public Queue<ProfilingQueueItem<T>> getQueue() {
        return queue;
    }
    /**
     * Returns the class.
     * @return the class
     */
    public Class<T> getCls() {
        return cls;
    }
    /**
     * Adds the queue item to queue.
     * @param timestamp the timestamp
     * @param obj the item to be added
     */
    public void add(long timestamp, Object obj) {
        T item = cls.cast(obj);
        queue.add(new ProfilingQueueItem<T>(timestamp, item));
    }
    
}
