package eu.qualimaster.common.switching.synchronization;

import java.util.Queue;

import org.apache.log4j.Logger;

import eu.qualimaster.base.algorithm.ISwitchTuple;
import eu.qualimaster.base.pipeline.NodeHostStorm;
import eu.qualimaster.common.signal.AbstractSignalConnection;
import eu.qualimaster.common.signal.GoToPassiveSignal;
import eu.qualimaster.common.signal.SignalStates;
import eu.qualimaster.common.signal.TransferredSignal;
import eu.qualimaster.common.switching.QueueHolder;
import eu.qualimaster.common.switching.SwitchNodeNameInfo;
import eu.qualimaster.common.switching.TupleSender;

/**
 * Provide a synchronization strategy for the original intermediary node.
 * 
 * @author Cui Qin
 *
 */
public class SeparatedOrgINTSynchronizationStrategy extends AbstractSynchronizationStrategy {
    public static final String STRATEGYTYPE = AbstractSynchronizationStrategy.STRATEGYTYPE
            + SwitchNodeNameInfo.ORIGINALINTERMEDIARYNODE;
    private static final Logger LOGGER = Logger.getLogger(SeparatedOrgINTSynchronizationStrategy.class);
    private long lastProcessedId;
    private long headId;
    private Queue<ISwitchTuple> inQueue;
    private Queue<ISwitchTuple> outQueue;
    private String host;
    private TupleSender sender;

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
    public SeparatedOrgINTSynchronizationStrategy(QueueHolder queueHolder, AbstractSignalConnection signalConnection,
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
        // do nothing
    }

    @Override
    public void doDataTransfer() {
        lastProcessedId = SignalStates.getLastProcessedId();
        headId = SignalStates.getHeadId();
        host = getHost(getNameInfo().getTargetIntermediaryNodeName());
        sender = new TupleSender(host, SignalStates.getTargetPort());
        LOGGER.info("Transferring data to the host: " + host + ", the headId: " + headId + ", the lastProcessedId: "
                + lastProcessedId);
        if (lastProcessedId != 0) {
            if (SignalStates.isTransferAll()) {
                transferAllOrgINT();
            } else if (headId != 0) {
                transferMissingItemsOrgINT();
            }
            goToPassive(); // the original intermediary node goes to passive
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
                sendToTarget(item);
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
                    sendToTarget(item);
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
        LOGGER.info("Transfer all items to the target Spout with outQueue size:" + outQueue.size() + ", inQueue size:"
                + inQueue.size() + " Top id:" + topId);
        // transferring data from the outQueue
        while (!outQueue.isEmpty()) {
            ISwitchTuple item = outQueue.poll();
            tmpId = item.getId();
            if (tmpId > lastProcessedId) {
                LOGGER.info(System.currentTimeMillis() + " Transferring the out queue to the target Spout: "
                        + item.getId() + ", count: " + count);
                sendToTarget(item);
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
                sendToTarget(item);
                transferredId = tmpId;
                count++;
            }
        }
        if (count < SignalStates.getNumTransferredData()) {
            TransferredSignal.sendSignal(getNameInfo().getTopologyName(), getNameInfo().getTargetIntermediaryNodeName(),
                    count, getSignalConnection());
            LOGGER.info(System.currentTimeMillis() + ", transferAll --Sent transferred signal with the number of data: "
                    + count);
        }

        if (transferredId == 0) {
            LOGGER.info(System.currentTimeMillis()
                    + ", transferAll --Sending transferred signal with the last transferred Id: " + transferredId);
            TransferredSignal.sendSignal(getNameInfo().getTopologyName(), getNameInfo().getTargetIntermediaryNodeName(),
                    transferredId, getSignalConnection());
        }
        LOGGER.info("The end of transferring all items to the target Spout. with outQueue size:" + outQueue.size()
                + ", inQueue size:" + inQueue.size());
    }

    /**
     * Send the tuple to the target node.
     * 
     * @param item
     *            the tuple
     */
    private void sendToTarget(ISwitchTuple item) {
        sender.send(SignalStates.getKryoSerOrgINT().serialize(item));
    }

    /**
     * Goes to passive state, i.e., the original algorithm is disactivating.
     */
    public void goToPassive() {
        outQueue.clear();
        SignalStates.setPassivateOrgINT(true); // isPassivate = true;
        SignalStates.setTransferringOrgINT(false); // isTransferring = false;
        // isActiveSpout = false;
        // SignalStates.setFirstId(0); // firstId = 0;
        // omitOnce = true;
        // synOnce = true;
        LOGGER.info(System.currentTimeMillis() + ", Go to passive and inform the end bolt.");
        GoToPassiveSignal.sendSignal(getNameInfo().getTopologyName(), getNameInfo().getOriginalEndNodeName(), true,
                getSignalConnection());
    }

    /**
     * Return the name of the host to be connected to.
     * 
     * @param nodeName
     *            the name of the target node
     * @return the name of the host to be connected to.
     */
    private static String getHost(String nodeName) {
        LOGGER.info("Getting the host, " + getNameInfo().getTopologyName() + ", " + nodeName);
        String host = NodeHostStorm.getHost(getNameInfo().getTopologyName(), nodeName);
        return host;
    }

}
