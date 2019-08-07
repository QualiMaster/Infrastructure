package eu.qualimaster.common.switching.synchronization;

import java.util.Queue;

import org.apache.log4j.Logger;

import eu.qualimaster.base.algorithm.ISwitchTuple;
import eu.qualimaster.common.signal.AbstractSignalConnection;
import eu.qualimaster.common.signal.EmitSignal;
import eu.qualimaster.common.signal.GoToActiveSignal;
import eu.qualimaster.common.signal.GoToPassiveSignal;
import eu.qualimaster.common.signal.HeadIdSignal;
import eu.qualimaster.common.signal.SignalStates;
import eu.qualimaster.common.signal.SynchronizedSignal;
import eu.qualimaster.common.signal.TransferSignal;
import eu.qualimaster.common.signal.TransferredSignal;
import eu.qualimaster.common.switching.QueueHolder;
import eu.qualimaster.common.switching.SwitchNodeNameInfo;

/**
 * Provide a synchronization strategy for the case of separated intermediary
 * nodes.
 * 
 * @author Cui Qin
 *
 */
public class SeparatedINTSynchronizationStrategy extends AbstractSynchronizationStrategy {
    private static final Logger LOGGER = Logger.getLogger(SeparatedINTSynchronizationStrategy.class);
    private int overloadSize;
    private AbstractSignalConnection signalConnection;
    private long lastProcessedId;
    private long lastEmittedId;
    private long headId;
    private Queue<ISwitchTuple> inQueue;
    private Queue<ISwitchTuple> outQueue;
    private String node;

    /**
     * Constructor of the abstract synchronization strategy.
     * 
     * @param node
     *            the node type
     * @param queueHolder
     *            the queue holder
     * @param signalConnection
     *            the signal connection
     * @param overloadSize
     *            the maximal size of tuples we allow to synchronize
     */
    public SeparatedINTSynchronizationStrategy(String node, QueueHolder queueHolder,
            AbstractSignalConnection signalConnection, int overloadSize) {
        this.node = node;
        this.inQueue = queueHolder.getInQueue();
        this.outQueue = queueHolder.getOutQueue();
        this.signalConnection = signalConnection;
        this.overloadSize = overloadSize;
    }

    @Override
    public void doSynchronization() {
        lastEmittedId = SignalStates.getLastEmittedId();
        lastProcessedId = SignalStates.getLastProcessedId();
        headId = SignalStates.getHeadId();

        switch (node) {
        case SwitchNodeNameInfo.TARGETINTERMEDIARYNODE:
            doSynchronizationTrgINT();
            break;
        case SwitchNodeNameInfo.ORIGINALENDNODE:
            doSynchronizationOrgINT();
            break;
        default:
            break;
        }
    }

    /**
     * The synchronization in the target intermediary node.
     */
    private void doSynchronizationTrgINT() {
        LOGGER.info("The lastEmittedId: " + lastEmittedId + ", the lastProcessedId: " + lastProcessedId);
        if (lastEmittedId != 0 && lastProcessedId != 0) {
            if (lastProcessedId == lastEmittedId || outQueue.size() > overloadSize
                    || (lastEmittedId - lastProcessedId) > overloadSize) {
                outQueue.clear();
                LOGGER.info(System.currentTimeMillis() + ", Sending the emit signal to the target end node!");
                EmitSignal.sendSignal(getNameInfo().getTopologyName(), getNameInfo().getTargetEndNodeName(), true,
                        signalConnection);
                completingSynchronization();
            } else {
                SignalStates.setTransferringTrgINT(true); // it is in the
                                                          // transferring phase
                synchronizeItemsTrgINT();
            }
        }
    }

    /**
     * The synchronization in the original intermediary node.
     */
    private void doSynchronizationOrgINT() {
        LOGGER.info("The headId: " + headId + ", the lastProcessedId: " + lastProcessedId);
        if (lastProcessedId != 0) {
            if (SignalStates.isTransferAll()) {
                transferAllOrgINT();
            } else if (headId != 0) {
                transferMissingItemsOrgINT();
            }
            goToPassive(); //the original intermediary node goes to passive
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
            TransferSignal.sendSignal(getNameInfo().getTopologyName(), getNameInfo().getOriginalIntermediaryNodeName(),
                    numTransferredData, signalConnection);
            SignalStates.setTransferAll(true); // record that it is set to send
                                               // all tuples

        } else {
            long id = lastProcessedId;
            if (!inQueue.isEmpty()) {
                id = inQueue.peek().getId();
            }
            LOGGER.info(System.currentTimeMillis() + "Synchronizing the last id of the current alg: " + id
                    + " with the last processed id of the previous alg:" + lastProcessedId);
            if (id > lastProcessedId) { // the current alg is faster than the
                                        // previous alg
                numTransferredData = (int) (id - lastProcessedId) - 1;
                LOGGER.info(System.currentTimeMillis() + ", Sending the headId signal to the original intermediary "
                        + "node with id:" + id + ", numTransferredData: " + numTransferredData);
                firstId = id - 1;
                String headIdValue = String.valueOf(id) + "," + String.valueOf(lastProcessedId);

                LOGGER.info(
                        System.currentTimeMillis() + ", Sending the headId signal to the original intermediary node!");
                HeadIdSignal.sendSignal(getNameInfo().getTopologyName(),
                        getNameInfo().getOriginalIntermediaryNodeName(), headIdValue, signalConnection);
            } else {
                while (id < lastProcessedId && !inQueue.isEmpty()) {
                    id = inQueue.poll().getId();
                }
                LOGGER.info(System.currentTimeMillis() + ", Skipped tuples until the id:" + id);
                LOGGER.info(System.currentTimeMillis() + ", Completing the synchronization.");

                completingSynchronization();
            }
        }

        LOGGER.info(System.currentTimeMillis() + ", Sending the emit signal to the target end node!");
        EmitSignal.sendSignal(getNameInfo().getTopologyName(), getNameInfo().getTargetEndNodeName(), true,
                signalConnection);

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
                signalConnection);
        if (!SignalStates.isPassivateTrgINT()) {
            goToActive();
        } else {
            SignalStates.setTransferringTrgINT(false);
            //isActiveSpout = true;
            SignalStates.setFirstId(0); //firstId = 0;
//            omitOnce = true;
//            synOnce = true;
        }
    }

    /**
     * Transfers the missing data items in the original intermediary node.
     */
    public void transferMissingItemsOrgINT() {
        long id;
        LOGGER.info("Transferring missing items with outQueue: " + outQueue.size() + ", inQueue:" + inQueue.size()
                + ", lastProcessedId: " + lastProcessedId + ", headId: " + headId);
        while (!outQueue.isEmpty()) {
            ISwitchTuple item = outQueue.poll();
            id = item.getId();
            if (id > lastProcessedId && id < headId) {
                LOGGER.info(System.currentTimeMillis() + "outQueue--Transferring the missing items " + id);
                // sendToTarget(item);
            }
            if (id == headId) {
                break;
            }
        }
        if (!inQueue.isEmpty()) {
            id = inQueue.peek().getId();
            while (id < headId) {
                ISwitchTuple item = inQueue.poll();
                if (id > lastProcessedId) {
                    LOGGER.info(System.currentTimeMillis() + "inQueue--Transferring the missing items " + id);
                    // sendToTarget(item);
                }
                id = item.getId();
            }
        }
        LOGGER.info("The end of transferring missing items with outQueue: " + outQueue.size() + ", inQueue:"
                + inQueue.size());
    }

    /**
     * Transfers all data items in the original intermediary node.
     */
    public void transferAllOrgINT() { // TODO: consider the case that there are
                                      // some un-acked items
        long topId = 0;
        long tmpId = 0;
        long transferredId = 0;
        int count = 0;
        if (!outQueue.isEmpty()) {
            topId = outQueue.peek().getId();
        } else if (!inQueue.isEmpty()) {
            topId = inQueue.peek().getId();
        }
        LOGGER.info("Transfer all items to the target Spout. with outQueue size:" + outQueue.size() + ", inQueue size:"
                + inQueue.size() + " Top id:" + topId);
        // transferring data from the outQueue
        while (!outQueue.isEmpty()) {
            ISwitchTuple item = outQueue.poll();
            tmpId = item.getId();
            if (tmpId > lastProcessedId) {
                LOGGER.info(
                        System.currentTimeMillis() + " Transferring the out queue to the target Spout." + item.getId());
                // sendToTarget(item);
                transferredId = tmpId;
                count++;
            }
        }
        // transferring data from the inQueue
        while (!inQueue.isEmpty()) {
            ISwitchTuple item = inQueue.poll();
            tmpId = item.getId();
            if (tmpId > lastProcessedId) {
                LOGGER.info(
                        System.currentTimeMillis() + " Transferring the in queue to the target Spout." + item.getId());
                // sendToTarget(item);
                transferredId = tmpId;
                count++;
            }
        }
        if (count < SignalStates.getNumTransferredData()) {
            TransferredSignal.sendSignal(getNameInfo().getTopologyName(), getNameInfo().getTargetIntermediaryNodeName(),
                    count, signalConnection);
            LOGGER.info(System.currentTimeMillis() + ", transferAll --Sent transferred signal with the number of data: "
                    + count);
        }

        if (transferredId == 0) {
            LOGGER.info(System.currentTimeMillis()
                    + ", transferAll --Sending transferred signal with the last transferred Id: " + transferredId);
            TransferredSignal.sendSignal(getNameInfo().getTopologyName(), getNameInfo().getTargetIntermediaryNodeName(),
                    transferredId, signalConnection);
        }
        LOGGER.info("The end of transferring all items to the target Spout. with outQueue size:" + outQueue.size()
                + ", inQueue size:" + inQueue.size());
    }

    /**
     * Goes to passive state, i.e., the original algorithm is disactivating.
     */
    public void goToPassive() {
        outQueue.clear();
        SignalStates.setPassivateOrgINT(true); // isPassivate = true;
        SignalStates.setTransferringOrgINT(false); // isTransferring = false;
        // isActiveSpout = false;
        SignalStates.setFirstId(0); // firstId = 0;
        // omitOnce = true;
        // synOnce = true;
        LOGGER.info(System.currentTimeMillis() + ", Go to passive and inform the end bolt.");
        GoToPassiveSignal.sendSignal(getNameInfo().getTopologyName(), getNameInfo().getOriginalEndNodeName(), true,
                signalConnection);
    }

    /**
     * Goes to active state, i.e., the target algorithm is activating.
     */
    public void goToActive() {
        SignalStates.setPassivateTrgINT(false); //isPassivate = false;
        SignalStates.setTransferringTrgINT(false);
        //isActiveSpout = true;
        SignalStates.setFirstId(0); //firstId = 0;
//        omitOnce = true;
//        synOnce = true;
        LOGGER.info(System.currentTimeMillis() + ", Go to active and inform the end bolt.");
        GoToActiveSignal.sendSignal(getNameInfo().getTopologyName(), getNameInfo().getTargetEndNodeName(), true,
                signalConnection);
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

}
