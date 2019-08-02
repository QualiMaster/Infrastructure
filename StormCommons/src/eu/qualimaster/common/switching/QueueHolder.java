package eu.qualimaster.common.switching;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

import eu.qualimaster.base.algorithm.ISwitchTuple;

/**
 * Holding the queues needed during the synchronization of the algorithm switching.
 * @author Cui Qin
 *
 */
public class QueueHolder {
    private transient ConcurrentLinkedDeque<ISwitchTuple> outQueue; //output queue for checking the acknowledged items
    private transient ConcurrentLinkedQueue<ISwitchTuple> inQueue; //input queue
    private transient ConcurrentLinkedQueue<ISwitchTuple> tmpQueue; //temporary queue
    
    /**
     * Constructor.
     * @param outQueue the output queue.
     * @param inQueue the input queue.
     * @param tmpQueue the temporary queue.
     */
    public QueueHolder(ConcurrentLinkedQueue<ISwitchTuple> inQueue, ConcurrentLinkedDeque<ISwitchTuple> outQueue
            , ConcurrentLinkedQueue<ISwitchTuple> tmpQueue) {
        this.outQueue = outQueue;
        this.inQueue = inQueue;
        this.tmpQueue = tmpQueue;
    }
    
    /**
     * Return the output queue.
     * @return the output queue
     */
    public ConcurrentLinkedDeque<ISwitchTuple> getOutQueue() {
        return outQueue;
    }

    /**
     * Set the output queue.
     * @param outQueue the output queue
     */
    public void setOutQueue(ConcurrentLinkedDeque<ISwitchTuple> outQueue) {
        this.outQueue = outQueue;
    }

    /**
     * Return the input queue.
     * @return the input queue
     */
    public ConcurrentLinkedQueue<ISwitchTuple> getInQueue() {
        return inQueue;
    }

    /**
     * Set the input queue.
     * @param inQueue the input queue
     */
    public void setInQueue(ConcurrentLinkedQueue<ISwitchTuple> inQueue) {
        this.inQueue = inQueue;
    }

    /**
     * Return the temporary queue.
     * @return the temporary queue
     */
    public ConcurrentLinkedQueue<ISwitchTuple> getTmpQueue() {
        return tmpQueue;
    }

    /**
     * Set the temporary queue.
     * @param tmpQueue the temporary queue
     */
    public void setTmpQueue(ConcurrentLinkedQueue<ISwitchTuple> tmpQueue) {
        this.tmpQueue = tmpQueue;
    }
}
