package eu.qualimaster.common.switching;

import java.util.Queue;
/**
 * A synchronized queue adopting the producer-consumer pattern.
 * @param <T> the data type
 * @author Cui Qin
 *
 */
public class SynchronizedQueue<T> {
    private Queue<T> queue;
    private int size;
    
    /**
     * Creates a synchronized queue.
     * @param queue the queue to store data
     * @param size the size of a full queue 
     */
    public SynchronizedQueue(Queue<T> queue, int size) {
        this.queue = queue;
        this.size = size;
    }
    /**
     * Consumes tuple data from the queue.
     * @return a tuple
     */
    public T consume() {
      //wait if queue is empty
        while (queue.isEmpty()) {
            synchronized (queue) {
                try {
                    queue.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
      //Otherwise consume element and notify waiting producer
        synchronized (queue) {
            queue.notifyAll();
            return queue.poll();
        }
    }

    /**
     * Stores tuples into the queue.
     * @param data the tuple data to be stored
     */
    public void produce(T data) {
        // wait if queue is full
        while (queue.size() == size) {
            synchronized (queue) {
                try {
                    queue.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        // producing element and notify consumers
        synchronized (queue) {
            queue.offer(data);
            queue.notifyAll();
        }
    }
    
    /**
     * Returns the current size of the queue.
     * @return the current size
     */
    public int currentSize() {
        return queue.size();
    }
}
