package eu.qualimaster.common.switching.synchronization;

import java.util.Queue;

import org.apache.log4j.Logger;

import eu.qualimaster.base.algorithm.ISwitchTuple;
import eu.qualimaster.common.signal.AbstractSignalConnection;
import eu.qualimaster.common.signal.EmitSignal;
import eu.qualimaster.common.signal.GoToActiveSignal;
import eu.qualimaster.common.signal.HeadIdSignal;
import eu.qualimaster.common.signal.SignalStates;
import eu.qualimaster.common.signal.SynchronizedSignal;
import eu.qualimaster.common.signal.TransferSignal;
import eu.qualimaster.common.switching.QueueHolder;
import eu.qualimaster.common.switching.SwitchNodeNameInfo;

/**
 * Provide a synchronization strategy for the target intermediary node.
 * 
 * @author Cui Qin
 *
 */
public class SeparatedTrgINTSynchronizationStrategy extends AbstractSynchronizationStrategy {
    public static final String STRATEGYTYPE = AbstractSynchronizationStrategy.STRATEGYTYPE
            + SwitchNodeNameInfo.TARGETINTERMEDIARYNODE;
    private static final Logger LOGGER = Logger.getLogger(SeparatedTrgINTSynchronizationStrategy.class);
    private long lastProcessedId;
    private long lastEmittedId;
    private Queue<ISwitchTuple> inQueue;
    private Queue<ISwitchTuple> outQueue;

    /**
     * Constructor.
     * 
     * @param queueHolder
     *            the queue holder
     * @param signalConnection
     *            the signal connection
     * @param overloadSize
     *            the maximal size of tuples we allow to synchronize
     */
    public SeparatedTrgINTSynchronizationStrategy(QueueHolder queueHolder, AbstractSignalConnection signalConnection,
            int overloadSize) {
        super(queueHolder, signalConnection, overloadSize);
        this.inQueue = getInQueue();
        this.outQueue = getOutQueue();
    }

    @Override
    public String getStrategyType() {
        return STRATEGYTYPE;
    }

    @Override
    public void doSynchronization() {
        lastEmittedId = SignalStates.getLastEmittedId();
        lastProcessedId = SignalStates.getLastProcessedId();
        LOGGER.info("The lastEmittedId: " + lastEmittedId + ", the lastProcessedId: " + lastProcessedId);
        if (lastEmittedId != 0 && lastProcessedId != 0) {
            if (lastProcessedId == lastEmittedId || outQueue.size() > getOverloadSize()
                    || (lastEmittedId - lastProcessedId) > getOverloadSize()) {
                outQueue.clear();
                LOGGER.info(System.currentTimeMillis() + ", Sending the emit signal to the target end node!");
                EmitSignal.sendSignal(getNameInfo().getTopologyName(), getNameInfo().getTargetEndNodeName(), true,
                        getSignalConnection());
                completingSynchronization();
            } else {
                SignalStates.setTransferringTrgINT(true); // it is in the
                                                          // transferring phase
                synchronizeItemsTrgINT();
            }
        }
    }

    @Override
    public void doDataTransfer() {
        // do nothing
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
            TransferSignal.sendSignal(getNameInfo().getTopologyName(), getNameInfo().getOriginalIntermediaryNodeName(),
                    numTransferredData, getSignalConnection());
            SignalStates.setTransferAll(true); // record that it is set to send
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

                HeadIdSignal.sendSignal(getNameInfo().getTopologyName(),
                        getNameInfo().getOriginalIntermediaryNodeName(), headIdValue, getSignalConnection());
            } else {
                while (id < lastProcessedId && !inQueue.isEmpty()) {
                    id = inQueue.poll().getId();
                }
                LOGGER.info(System.currentTimeMillis() + ", Skipped tuples until the id:" + id
                        + ", with input queue size:" + inQueue.size());
                LOGGER.info(System.currentTimeMillis() + ", Completing the synchronization.");

                completingSynchronization();
            }
        }

        LOGGER.info(System.currentTimeMillis() + ", Sending the emit signal to the target end node!");
        EmitSignal.sendSignal(getNameInfo().getTopologyName(), getNameInfo().getTargetEndNodeName(), true,
                getSignalConnection());

        // record the number of data items to be transferred
        SignalStates.setNumTransferredData(numTransferredData);
        // record the id of the first tuple to be transferred
        SignalStates.setFirstId(firstId);
    }

    /**
     * Completing the synchronization.
     */
    public void completingSynchronization() {
        LOGGER.info(System.currentTimeMillis() + ", Sending the synchronized signal to the preceding node!");
        SynchronizedSignal.sendSignal(getNameInfo().getTopologyName(), getNameInfo().getPrecedingNodeName(), true,
                getSignalConnection());
        if (!SignalStates.isPassivateTrgINT()) {
            goToActive();
        } else {
            SignalStates.setTransferringTrgINT(false);
            // isActiveSpout = true;
            SignalStates.setFirstId(0); // firstId = 0;
            // omitOnce = true;
            // synOnce = true;
        }
    }

    /**
     * Goes to active state, i.e., the target algorithm is activating.
     */
    public void goToActive() {
        SignalStates.setPassivateTrgINT(false); // isPassivate = false;
        SignalStates.setTransferringTrgINT(false);
        // isActiveSpout = true;
        SignalStates.setFirstId(0); // firstId = 0;
        // omitOnce = true;
        // synOnce = true;
        LOGGER.info(System.currentTimeMillis() + ", Go to active and inform the end bolt.");
        GoToActiveSignal.sendSignal(getNameInfo().getTopologyName(), getNameInfo().getTargetEndNodeName(), true,
                getSignalConnection());
    }

}
