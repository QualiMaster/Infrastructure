package eu.qualimaster.common.switching.actions;

import java.util.Queue;

import org.apache.log4j.Logger;

import eu.qualimaster.base.algorithm.ISwitchTuple;
import eu.qualimaster.common.signal.AbstractSignalConnection;
import eu.qualimaster.common.switching.QueueHolder;
import eu.qualimaster.common.switching.SwitchNodeNameInfo;

/**
 * Provides a synchronization strategy.
 * 
 * @author Cui Qin
 *
 */
public class SynchronizationStrategy implements ISynchronizationStrategy {
    private static final Logger LOGGER = Logger.getLogger(SynchronizationStrategy.class);
    private Queue<ISwitchTuple> inQueue;
    private Queue<ISwitchTuple> outQueue;
    private AbstractSignalConnection signalCon;
    private int overloadSize;
    private long lastProcessedId;
    private long lastEmittedId;

    /**
     * Creates a synchronization strategy.
     * 
     * @param queueHolder
     *            the queue holder carrying the input and output queues
     * @param signalCon
     *            the signal connection
     * @param overloadSize the size indicating queues are overloaded
     */
    public SynchronizationStrategy(QueueHolder queueHolder, AbstractSignalConnection signalCon, int overloadSize) {
        this.inQueue = queueHolder.getInQueue();
        this.outQueue = queueHolder.getOutQueue();
        this.signalCon = signalCon;
        this.overloadSize = overloadSize;
    }

    @Override
    public void synchronizeData() {
        lastEmittedId = SwitchStates.getLastEmittedId();
        lastProcessedId = SwitchStates.getLastProcessedId();
        LOGGER.info("The lastEmittedId: " + lastEmittedId + ", the lastProcessedId: " + lastProcessedId);
        if (lastEmittedId != 0 && lastProcessedId != 0) {
            if (lastProcessedId == lastEmittedId || outQueue.size() > overloadSize
                    || (lastEmittedId - lastProcessedId) > overloadSize) {
                outQueue.clear();
                LOGGER.info(System.currentTimeMillis() + ", Sending the emit signal to the target end node!");
                new SendSignalAction(Signal.EMIT, getNameInfo().getTargetEndNodeName(), true, signalCon).execute();
                new CompleteSwitchAction(signalCon).execute();
            } else {
                SwitchStates.setTransferringTrgINT(true); // it is in the
                                                          // transferring phase
                synchronizeItemsTrgINT();
            }
        }

    }

    /**
     * Synchronize data items in both queues for the target intermediary node.
     */
    private void synchronizeItemsTrgINT() {
        long firstId = 0;
        int numTransferredData = 0;
        if (inQueue.isEmpty()) { // request the original intermediary node to
                                 // transfer all tuples
            LOGGER.info(System.currentTimeMillis() + ", Request to send all tuples.");
            numTransferredData = (int) (lastEmittedId - lastProcessedId);
            firstId = lastEmittedId;

            LOGGER.info(
                    System.currentTimeMillis() + ", Sending the transfer signal to the original intermediary node!");
            new SendSignalAction(Signal.TRANSFER, getNameInfo().getOriginalIntermediaryNodeName(), numTransferredData,
                    signalCon).execute();
            SwitchStates.setTransferAll(true); // record that it is set to send
                                               // all tuples

        } else {
            long id = lastProcessedId;
            if (!inQueue.isEmpty()) {
                id = inQueue.peek().getId();
            }
            LOGGER.info(System.currentTimeMillis() + ", Synchronizing the last id of the current alg: " + id
                    + " with the last processed id of the previous alg:" + lastProcessedId);
            if (id > lastProcessedId) { // the current alg is faster than the
                                        // previous alg
                numTransferredData = (int) (id - lastProcessedId) - 1;
                LOGGER.info(System.currentTimeMillis() + ", Sending the headId signal to the original intermediary "
                        + "node with id:" + id + ", lastProcessedId: " + lastProcessedId);
                firstId = id - 1;
                String headIdValue = String.valueOf(id) + "," + String.valueOf(lastProcessedId);
                new SendSignalAction(Signal.HEADID, getNameInfo().getOriginalIntermediaryNodeName(), headIdValue,
                        signalCon).execute();
            } else {
                while (id < lastProcessedId && !inQueue.isEmpty()) {
                    id = inQueue.poll().getId();
                }
                LOGGER.info(System.currentTimeMillis() + ", Skipped tuples until the id:" + id
                        + ", with input queue size:" + inQueue.size());
                LOGGER.info(System.currentTimeMillis() + ", Completing the synchronization.");

                new CompleteSwitchAction(signalCon).execute();
            }
        }

        LOGGER.info(System.currentTimeMillis() + ", Sending the emit signal to the target end node!");
        new SendSignalAction(Signal.EMIT, getNameInfo().getTargetEndNodeName(), true, signalCon).execute();

        // record the number of data items to be transferred
        SwitchStates.setNumTransferredData(numTransferredData);
        // record the id of the first tuple to be transferred
        SwitchStates.setFirstTupleId(firstId);
    }

    

    /**
     * Gets the information of the names of the switch-related nodes.
     * 
     * @return the information of the names of the switch-related nodes
     */
    private static SwitchNodeNameInfo getNameInfo() {
        return SwitchNodeNameInfo.getInstance();
    }
}
