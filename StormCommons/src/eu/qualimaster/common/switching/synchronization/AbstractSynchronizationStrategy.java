package eu.qualimaster.common.switching.synchronization;

import java.util.Queue;

import eu.qualimaster.base.algorithm.ISwitchTuple;
import eu.qualimaster.common.signal.AbstractSignalConnection;
import eu.qualimaster.common.switching.QueueHolder;
import eu.qualimaster.common.switching.SwitchNodeNameInfo;

/**
 * Provide an abstract synchronization strategy.
 * @author Cui Qin
 *
 */
public abstract class AbstractSynchronizationStrategy implements ISynchronizationStrategy {
    protected static final String STRATEGYTYPE = "synchronization";
    private Queue<ISwitchTuple> inQueue;
    private Queue<ISwitchTuple> outQueue;
    private int overloadSize;
    private AbstractSignalConnection signalConnection;
    
    /**
     * Constructor of the abstract synchronization strategy.
     * 
     * @param queueHolder
     *            the queue holder
     * @param signalConnection
     *            the signal connection
     * @param overloadSize
     *            the maximal size of tuples we allow to synchronize
     */
    public AbstractSynchronizationStrategy(QueueHolder queueHolder, AbstractSignalConnection signalConnection,
            int overloadSize) {
        this.inQueue = queueHolder.getInQueue();
        this.outQueue = queueHolder.getOutQueue();
        this.signalConnection = signalConnection;
        this.overloadSize = overloadSize;
    }
    
    @Override
    public String getStrategyType() {
        return STRATEGYTYPE;
    }
    
    /**
     * Gets the information of the names of the switch-related nodes.
     * 
     * @return the information of the names of the switch-related nodes
     */
    public static SwitchNodeNameInfo getNameInfo() {
        return SwitchNodeNameInfo.getInstance();
    }

    /**
     * Returns the signal connection to support for sending signals.
     * 
     * @return the signal connection
     */
    public AbstractSignalConnection getSignalConnection() {
        return signalConnection;
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
     * Return the overload size.
     * @return the overload size
     */
    public int getOverloadSize() {
        return overloadSize;
    }
    
}
