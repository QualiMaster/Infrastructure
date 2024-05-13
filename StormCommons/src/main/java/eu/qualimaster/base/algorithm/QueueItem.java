package eu.qualimaster.base.algorithm;

/**
 * Provides the item for the queue used in the pipeline.
 * @author qin
 *
 * @param <T> the item type
 */
public class QueueItem<T> {
    private int id;
    private T tuple;
    
    /**
     * Creates a queue item with an integer id and the tuple.
     * @param id the sequential identification of the item
     * @param tuple the data item processing in the pipeline 
     */
    public QueueItem(int id, T tuple) {
        this.id = id;
        this.tuple = tuple;
    }
    
    /**
     * Returns the sequential id.
     * @return the sequential id
     */
    public int getId() {
        return id;
    }
    
    /**
     * Sets the sequential id.
     * @param id the sequential id
     */
    public void setId(int id) {
        this.id = id;
    }
    
    /**
     * Returns the data item.
     * @return the data item
     */
    public T getTuple() {
        return tuple;
    }
}
