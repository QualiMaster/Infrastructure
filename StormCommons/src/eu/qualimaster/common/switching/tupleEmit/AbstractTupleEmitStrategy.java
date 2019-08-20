package eu.qualimaster.common.switching.tupleEmit;

import java.util.Queue;

import eu.qualimaster.base.algorithm.ISwitchTuple;
import eu.qualimaster.common.switching.QueueHolder;

/**
 * Provide an abstract class for tuple emit strategy.
 * @author Cui Qin
 *
 */
public abstract class AbstractTupleEmitStrategy implements ITupleEmitStrategy {
    protected static final String STRATEGYTYPE = "tupleEmit";
    private Queue<ISwitchTuple> inQueue;
    private Queue<ISwitchTuple> outQueue;
    private Queue<ISwitchTuple> tmpQueue;
    
    /**
     * Constructor for the tuple emit strategy.
     * 
     * @param queueHolder
     *            the queue holder.
     */
    public AbstractTupleEmitStrategy(QueueHolder queueHolder) {
        this.inQueue = queueHolder.getInQueue();
        this.outQueue = queueHolder.getOutQueue();
        this.tmpQueue = queueHolder.getTmpQueue();
    }
    
    @Override
    public String getStrategyType() {
        return STRATEGYTYPE;
    }

    /**
     * Return the input queue.
     * @return the input queue
     */
    public Queue<ISwitchTuple> getInQueue() {
        return inQueue;
    }

    /**
     * Return the output queue.
     * @return the output queue
     */
    public Queue<ISwitchTuple> getOutQueue() {
        return outQueue;
    }
    
    /**
     * Return the temporary queue.
     * @return the temporary queue
     */
    public Queue<ISwitchTuple> getTmpQueue() {
        return tmpQueue;
    }
    
}
