package eu.qualimaster.common.switching.actions;

import java.io.PrintWriter;
import java.util.Queue;

import org.apache.log4j.Logger;

import eu.qualimaster.base.algorithm.ISwitchTuple;
import eu.qualimaster.base.pipeline.NodeHostStorm;
import eu.qualimaster.common.signal.AbstractSignalConnection;
import eu.qualimaster.common.switching.QueueHolder;
import eu.qualimaster.common.switching.SwitchNodeNameInfo;
import eu.qualimaster.common.switching.TupleSender;
/**
 * Provides a concrete strategy to transfer data.
 * @author Cui Qin
 *
 */
public class TransferDataStrategy implements ITransferDataStrategy {
    private static final Logger LOGGER = Logger.getLogger(TransferDataStrategy.class);
    private Queue<ISwitchTuple> inQueue;
    private Queue<ISwitchTuple> outQueue;
    private AbstractSignalConnection signalCon;
    private String host;
    private TupleSender sender;
    private long lastProcessedId;
    private long headId;
    private PrintWriter out;
    
    /**
     * Creates a strategy for transferring data.
     * @param queueHolder the queue holder carrying the input and output queues
     * @param signalCon the signal connection
     * @param out the log writer used to write logs into corresponding files
     */
    public TransferDataStrategy(QueueHolder queueHolder, AbstractSignalConnection signalCon, PrintWriter out) {
        this(queueHolder, signalCon);
        this.out = out;
    }
    
    /**
     * Creates a strategy for transferring data.
     * @param queueHolder the queue holder carrying the input and output queues
     * @param signalCon the signal connection
     */
    public TransferDataStrategy(QueueHolder queueHolder, AbstractSignalConnection signalCon) {
        this.inQueue = queueHolder.getInQueue();
        this.outQueue = queueHolder.getOutQueue();
        this.signalCon = signalCon;
    }
    
    @Override
    public void transferData() {
        lastProcessedId = SwitchStates.getLastProcessedId();
        headId = SwitchStates.getHeadId();
        host = getHost(getNameInfo().getTargetIntermediaryNodeName());
        sender = new TupleSender(host, SwitchStates.getTargetPort());
        if (null != out) {
            out.println("Transferring data to the host: " + host + ", the headId: " + headId + ", the lastProcessedId: "
                    + lastProcessedId);
            out.flush();
        }
        LOGGER.info("Transferring data to the host: " + host + ", the headId: " + headId + ", the lastProcessedId: "
                + lastProcessedId);
        if (lastProcessedId != 0) {
            if (SwitchStates.isTransferAll()) {
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
        if (null != out) {
            out.println("Transferring missing items with outQueue: " + outQueue.size() + ", inQueue:" + inQueue.size()
                + ", lastProcessedId: " + lastProcessedId + ", headId: " + headId);
            out.flush();
        }
        LOGGER.info("Transferring missing items with outQueue: " + outQueue.size() + ", inQueue:" + inQueue.size()
                + ", lastProcessedId: " + lastProcessedId + ", headId: " + headId);
        while (!outQueue.isEmpty()) {
            ISwitchTuple item = outQueue.poll();
            id = item.getId();
            if (id > lastProcessedId && id < headId) {
                if (null != out) {
                    out.println(System.currentTimeMillis() + ", outQueue--Transferring the missing items " + id);
                    out.flush();
                }
                LOGGER.info(System.currentTimeMillis() + ", outQueue--Transferring the missing items " + id);
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
                    if (null != out) {
                        out.println(System.currentTimeMillis() + ", inQueue--Transferring the missing items " + id);
                        out.flush();
                    }
                    LOGGER.info(System.currentTimeMillis() + ", inQueue--Transferring the missing items " + id);
                    sendToTarget(item);
                }
                id = item.getId();
            }
        }
        if (null != out) {
            out.println("The end of transferring missing items with outQueue: " + outQueue.size() + ", inQueue:"
                    + inQueue.size());
            out.flush();
        }
        LOGGER.info("The end of transferring missing items with outQueue: " + outQueue.size() + ", inQueue:"
                + inQueue.size());
    }

    /**
     * Transfers all data items in the original intermediary node.
     */
    public void transferAllOrgINT() { // TODO: consider the case that there are some un-acked items
        long topId = 0;
        long tmpId = 0;
        long transferredId = 0;
        int count = 0;
        if (!outQueue.isEmpty()) {
            topId = outQueue.peek().getId();
        } else if (!inQueue.isEmpty()) {
            topId = inQueue.peek().getId();
        }
        if (null != out) {
            out.println("Transfer all items to the target Spout with outQueue size:" + outQueue.size() 
                + ", inQueue size:" + inQueue.size() + " Top id:" + topId);
        }
        LOGGER.info("Transfer all items to the target Spout with outQueue size:" + outQueue.size() + ", inQueue size:"
                + inQueue.size() + " Top id:" + topId);
        while (!outQueue.isEmpty()) { // transferring data from the outQueue
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
        while (!inQueue.isEmpty()) { // transferring data from the inQueue
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
        if (count <= SwitchStates.getNumTransferredData()) {
            if (null != out) {
                out.println(System.currentTimeMillis() + ", transferAll --Sent transferred signal with the number "
                        + "of data: " + count);
                out.flush();
            }
            //sending a "transferred" signal with the count of the sent items
            new SendSignalAction(Signal.TRANSFERRED, getNameInfo().getTargetIntermediaryNodeName(),
                    count, signalCon).execute();
            LOGGER.info(System.currentTimeMillis() + ", transferAll --Sent transferred signal with the number of data: "
                    + count);
        }
        if (transferredId == 0) {
            if (null != out) {
                out.println(System.currentTimeMillis()
                        + ", transferAll --Sending transferred signal with the last transferred Id: " + transferredId);
                out.flush();
            }
            LOGGER.info(System.currentTimeMillis()
                    + ", transferAll --Sending transferred signal with the last transferred Id: " + transferredId);
            //sending a "transferred" signal with the id of the last transferred items
            new SendSignalAction(Signal.TRANSFERRED, getNameInfo().getTargetIntermediaryNodeName(),
                    transferredId, signalCon).execute();
        }
        if (null != out) {
            out.println("The end of transferring all items to the target Spout. with outQueue size:" + outQueue.size()
                + ", inQueue size:" + inQueue.size());
            out.flush();
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
        sender.send(SwitchStates.getKryoSerOrgINT().serialize(item));
    }

    /**
     * Goes to passive state, i.e., the original algorithm is disactivating.
     */
    public void goToPassive() {
        outQueue.clear();
        SwitchStates.setPassivateOrgINT(true); // isPassivate = true;
        SwitchStates.setTransferringOrgINT(false); // isTransferring = false;
        if (null != out) {
            out.println(System.currentTimeMillis() + ", Go to passive and inform the end bolt.");
            out.flush();
        }
        LOGGER.info(System.currentTimeMillis() + ", Go to passive and inform the end bolt.");
        //sending a "goToPassive" signal with the value of "true"
        new SendSignalAction(Signal.GOTOPASSIVE, getNameInfo().getOriginalEndNodeName(),
                true, signalCon).execute();
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
    
    /**
     * Gets the information of the names of the switch-related nodes.
     * 
     * @return the information of the names of the switch-related nodes
     */
    private static SwitchNodeNameInfo getNameInfo() {
        return SwitchNodeNameInfo.getInstance();
    }
}
