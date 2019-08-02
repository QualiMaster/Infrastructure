package eu.qualimaster.common.switching.synchronization;

import java.util.Queue;

import org.apache.log4j.Logger;

import eu.qualimaster.base.algorithm.ISwitchTuple;
import eu.qualimaster.common.signal.AbstractSignalConnection;
import eu.qualimaster.common.signal.EmitSignal;
import eu.qualimaster.common.signal.HeadIdSignal;
import eu.qualimaster.common.signal.SignalStates;
import eu.qualimaster.common.signal.SynchronizedSignal;
import eu.qualimaster.common.signal.TransferSignal;
import eu.qualimaster.common.switching.QueueHolder;
import eu.qualimaster.common.switching.SwitchNodeNameInfo;

/**
 * Provide a synchronization strategy for the case of separated intermediary nodes.
 * @author Cui Qin
 *
 */
public class SeparatedINTSynchronizationStrategy extends AbstractSynchronizationStrategy {
    private static final Logger LOGGER = Logger.getLogger(SeparatedINTSynchronizationStrategy.class);
    private int overloadSize;
    private AbstractSignalConnection signalConnection;
    private long lastProcessedId;
    private long lastEmittedId;
    private Queue<ISwitchTuple> inQueue;
    private Queue<ISwitchTuple> outQueue;
    
    /**
     * Constructor of the abstract synchronization strategy.
     * @param queueHolder the queue holder
     * @param signalConnection the signal connection
     * @param overloadSize the maximal size of tuples we allow to synchronize
     */
    public SeparatedINTSynchronizationStrategy(QueueHolder queueHolder, AbstractSignalConnection signalConnection
            , int overloadSize) {
        this.inQueue = queueHolder.getInQueue();
        this.outQueue = queueHolder.getOutQueue();
        this.signalConnection = signalConnection;
        this.overloadSize = overloadSize;
    }
    
    /**
     * Constructor of the abstract synchronization strategy.
     * @param lastEmittedId the id of the last tuple emitted from the preceding node
     * @param lastProcessedId the id of the last processed id
     * @param queueHolder the queue holder
     * @param signalConnection the signal connection
     * @param overloadSize the maximal size of tuples we allow to synchronize
     */
    public SeparatedINTSynchronizationStrategy(long lastEmittedId, long lastProcessedId, QueueHolder queueHolder
            , AbstractSignalConnection signalConnection, int overloadSize) {
        this.lastEmittedId = lastEmittedId;
        this.lastProcessedId = lastProcessedId;
        this.inQueue = queueHolder.getInQueue();
        this.outQueue = queueHolder.getOutQueue();
        this.signalConnection = signalConnection;
        this.overloadSize = overloadSize;
    }
    
    @Override
    public void doSynchronization() {
        LOGGER.info("The lastEmittedId: " + lastEmittedId + ", the lastProcessedId: " + lastProcessedId);
        if (lastEmittedId != 0 && lastProcessedId != 0) {
            if (lastProcessedId == lastEmittedId || outQueue.size() > overloadSize 
                    || (lastEmittedId - lastProcessedId) > overloadSize) {
                outQueue.clear();
                LOGGER.info(System.currentTimeMillis() 
                        + ", Sending the emit signal to the target end node!");
                EmitSignal.sendSignal(getNameInfo().getTopologyName(), getNameInfo().getTargetEndNodeName(), true
                        , signalConnection);
                completingSynchronization();
            } else {
                SignalStates.setTransferring(true); //it is in the transferring phase
                synchronizeItems();
            }
        }
        
    }
    
    /**
     * Synchronize data items in both queues.
     * @return the id of the first tuple to be transferred if needed
     */
    private long synchronizeItems() {
        long firstId = 0;
        int numTransferredData = 0;
        if (inQueue.isEmpty()) { //request the original intermediary node to transfer all tuples
            LOGGER.info(System.currentTimeMillis() + ", Request to send all tuples.");
            numTransferredData = (int) (lastEmittedId - lastProcessedId);
            firstId = lastEmittedId;
            
            LOGGER.info(System.currentTimeMillis() 
                    + ", Sending the transfer signal to the original intermediary node!");
            TransferSignal.sendSignal(getNameInfo().getTopologyName(), getNameInfo().getOriginalIntermediaryNodeName()
                    , numTransferredData, signalConnection);
            
        } else {
            long id = lastProcessedId;
            if (!inQueue.isEmpty()) {
                id = inQueue.peek().getId();
            }
            LOGGER.info(System.currentTimeMillis() + "Synchronizing the last id of the current alg: " + id 
                    + " with the last processed id of the previous alg:" + lastProcessedId);
            if (id > lastProcessedId) { //the current alg is faster than the previous alg
                numTransferredData = (int) (id - lastProcessedId) - 1;
                LOGGER.info(System.currentTimeMillis() + ", Sending the headId signal to the original intermediary "
                        + "node with id:" + id + ", numTransferredData: " + numTransferredData);
                firstId = id - 1;
                String headIdValue = String.valueOf(id) + "," + String.valueOf(lastProcessedId);
                
                LOGGER.info(System.currentTimeMillis() 
                        + ", Sending the headId signal to the original intermediary node!");
                HeadIdSignal.sendSignal(getNameInfo().getTopologyName(), getNameInfo().getOriginalIntermediaryNodeName()
                        , headIdValue, signalConnection);
            } else {
                while (id < lastProcessedId && !inQueue.isEmpty()) {
                    id = inQueue.poll().getId();
                }
                LOGGER.info(System.currentTimeMillis() + ", Skipped tuples until the id:" + id);
                LOGGER.info(System.currentTimeMillis() + ", Completing the synchronization.");
                
                completingSynchronization();
            }
        }
        
        LOGGER.info(System.currentTimeMillis() 
                + ", Sending the emit signal to the target end node!");
        EmitSignal.sendSignal(getNameInfo().getTopologyName(), getNameInfo().getTargetEndNodeName()
                , true, signalConnection);
        return firstId;
    }
    
    /**
     * Completing the synchronization.
     */
    private void completingSynchronization() {
        LOGGER.info(System.currentTimeMillis() 
                + ", Sending the synchronized signal to the preceding node!");
        SynchronizedSignal.sendSignal(getNameInfo().getTopologyName(), getNameInfo().getPrecedingNodeName()
                , true, signalConnection); 
        //TODO other states
    }
    
    /**
     * Gets the information of the names of the switch-related nodes.
     * @return the information of the names of the switch-related nodes
     */
    public static SwitchNodeNameInfo getNameInfo() {
        return SwitchNodeNameInfo.getInstance();
    }
    
    /**
     * Returns the signal connection to support for sending signals.
     * @return the signal connection
     */
    public AbstractSignalConnection getSignalConnection() {
        return signalConnection;
    }
    
    /**
     * Return the id of the last processed tuple.
     * @return the last processed id
     */
    public long getLastProcessedId() {
        return lastProcessedId;
    }

    /**
     * Set the id of the last processed tuple.
     * @param lastProcessedId the last processed id
     */
    public void setLastProcessedId(long lastProcessedId) {
        this.lastProcessedId = lastProcessedId;
    }
    
    /**
     * Return the id of the last emitted tuple.
     * @return the id of the last emitted tuple
     */
    public long getLastEmittedId() {
        return lastEmittedId;
    }
    
    /**
     * Set the id of the last emitted tuple.
     * @param lastEmittedId the id of the last emitted tuple
     */
    public void setLastEmittedId(long lastEmittedId) {
        this.lastEmittedId = lastEmittedId;
    }

}
