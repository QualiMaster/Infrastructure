package eu.qualimaster.common.switching;

import java.util.Queue;

import backtype.storm.tuple.Tuple;
/**
 * A synchronized queue adopting the producer-consumer pattern.
 * @author Cui Qin
 *
 */
public class SynchronizedQueue {
    private static Queue<Tuple> queue;
    private static int size;
    
    /**
     * Creates a synchronized queue.
     * @param queue the queue to store data
     * @param size the size indicating the full queue 
     */
    public SynchronizedQueue(Queue<Tuple> queue, int size) {
        this.queue = queue;
        this.size = size;
    }
    /**
     * Consumes tuple data from the queue.
     * @return a tuple
     */
    public static Tuple consume() {
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
    public static void produce(Tuple data) {
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
}
