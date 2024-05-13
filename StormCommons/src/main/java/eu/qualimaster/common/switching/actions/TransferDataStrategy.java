package eu.qualimaster.common.switching.actions;

import java.util.List;
import java.util.Queue;

import eu.qualimaster.base.algorithm.ISwitchTuple;
import eu.qualimaster.base.pipeline.NodeHostStorm;
import eu.qualimaster.base.serializer.KryoSwitchTupleSerializer;
import eu.qualimaster.common.signal.AbstractSignalConnection;
import eu.qualimaster.common.switching.QueueHolder;
import eu.qualimaster.common.switching.SwitchNodeNameInfo;
import eu.qualimaster.common.switching.TupleSender;
import switching.logging.LogProtocol;
import switching.logging.QueueStatus;
import switching.logging.SignalName;
/**
 * Provides a concrete strategy to transfer data.
 * @author Cui Qin
 *
 */
public class TransferDataStrategy implements ITransferDataStrategy {
    private Queue<ISwitchTuple> inQueue;
    private Queue<ISwitchTuple> outQueue;
    private AbstractSignalConnection signalCon;
    private KryoSwitchTupleSerializer serializer;
    private String host;
    private TupleSender sender;
    private List<TupleSender> senders = null;
    private long lastProcessedId;
    private long headId;
    private LogProtocol logProtocol;
    private static boolean sendOnce = false;
    
    /**
     * Creates a strategy for transferring data.
     * @param queueHolder the queue holder carrying the input and output queues
     * @param signalCon the signal connection
     * @param serializer the kryro serializer to serialize the transferred data
     * @param logProtocol the log protocol used to write logs into corresponding files
     */
    public TransferDataStrategy(QueueHolder queueHolder, AbstractSignalConnection signalCon, KryoSwitchTupleSerializer serializer, LogProtocol logProtocol) {
        this(queueHolder, signalCon, serializer);
        this.logProtocol = logProtocol;
    }
    
    /**
     * Creates a strategy for transferring data.
     * @param queueHolder the queue holder carrying the input and output queues
     * @param signalCon the signal connection
     * @param serializer the kryro serializer to serialize the transferred data
     */
    public TransferDataStrategy(QueueHolder queueHolder, AbstractSignalConnection signalCon, KryoSwitchTupleSerializer serializer) {
        this.inQueue = queueHolder.getInQueue();
        this.outQueue = queueHolder.getOutQueue();
        this.signalCon = signalCon;
        this.serializer = serializer;
    }
    
    @Override
    public void transferData() {
        lastProcessedId = SwitchStates.getLastProcessedId();
        headId = SwitchStates.getHeadId();
        //connectTargetNode();
        connectMultiTargetNodes();
        if (null != logProtocol) {
            logProtocol.createGENLog("Transferring data to the host: " + host + ", the headId: " + headId 
                    + ", the lastProcessedId: " + lastProcessedId);
        }
        if (lastProcessedId != 0) {
        	logProtocol.createGENLog("isTransferAll:" + SwitchStates.isTransferAll());
            if (headId == 0 || SwitchStates.isTransferAll()) {
                transferAllOrgINT();
            } else if (headId != 0) {
                transferMissingItemsOrgINT();
            }
            goToPassive(); // the original intermediary node goes to passive
        } else {
        	logProtocol.createGENLog("Error: the lastProcessedId is ZERO!!!");
        }
    }
    
    /**
     * Connect the target node.
     */
    private void connectTargetNode() {
    	host = getHost(getNameInfo().getTargetIntermediaryNodeName());
        sender = new TupleSender(host, SwitchStates.getTargetPort());
    }
    
    /**
     * Connect a list of target nodes in case of parallelism.
     */
    private void connectMultiTargetNodes() {
    	senders = NodeHostStorm.createTupleSenders(getNameInfo().getTopologyName(), 
    			getNameInfo().getTargetIntermediaryNodeName(), SwitchStates.getTargetPort());
    }
    
    /**
     * Transfers the missing data items in the original intermediary node.
     */
    public void transferMissingItemsOrgINT() {
        long id;
        if (null != logProtocol) {
            logProtocol.createGENLog("Transferring missing items with outQueue: " + outQueue.size() 
                + ", inQueue:" + inQueue.size() + ", lastProcessedId: " + lastProcessedId + ", headId: " + headId);
        }
        while (!outQueue.isEmpty()) {
            ISwitchTuple item = outQueue.poll();
            id = item.getId();
            if (id > lastProcessedId && id < headId) {
                if (null != logProtocol) {
                    logProtocol.createTRANSFERLog(QueueStatus.OUTPUT, id);
                }
                sendToTargetShuffle(item);
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
                    if (null != logProtocol) {
                        logProtocol.createTRANSFERLog(QueueStatus.INPUT, id);
                    }
                    sendToTargetShuffle(item);
                }
                id = item.getId();
            }
        }
        if (null != logProtocol) {
            logProtocol.createGENLog("Reached the end of transferring missing items.");
            logProtocol.createQUEUELog(QueueStatus.INPUT, inQueue.size());
            logProtocol.createQUEUELog(QueueStatus.OUTPUT, outQueue.size());
        }
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
        if (null != logProtocol) {
            logProtocol.createGENLog("Transfer all items to the target Spout with the top id:" + topId);
            logProtocol.createQUEUELog(QueueStatus.INPUT, inQueue.size());
            logProtocol.createQUEUELog(QueueStatus.OUTPUT, outQueue.size());
        }
        while (!outQueue.isEmpty()) { // transferring data from the outQueue
            ISwitchTuple item = outQueue.poll();
            tmpId = item.getId();
            if (tmpId > lastProcessedId) {
                sendToTargetShuffle(item);
                transferredId = tmpId;
                count++;
                if (null != logProtocol) {
                    logProtocol.createTRANSFERLog(QueueStatus.OUTPUT, item.getId());
                    logProtocol.createGENLog("The count of the transferred data: " + count);
                }
            }
        }
        while (!inQueue.isEmpty()) { // transferring data from the inQueue
            ISwitchTuple item = inQueue.poll();
            tmpId = item.getId();
            if (tmpId > lastProcessedId) {
                sendToTargetShuffle(item);
                transferredId = tmpId;
                count++;
                if (null != logProtocol) {
                    logProtocol.createTRANSFERLog(QueueStatus.INPUT, item.getId());
                    logProtocol.createGENLog("The count of the transferred data: " + count);
                }
            }
        }
        
        if (!sendOnce && inQueue.isEmpty() && outQueue.isEmpty()) {
        	//send a TRANSFERRED signal to inform the actual number of items transferred.
        	sendOnce = true;
	        if (null != logProtocol) {
	            logProtocol.createSignalSENDLog(SignalName.TRANSFERRED, count
	                    , getNameInfo().getTargetIntermediaryNodeName());
	        }
	        //sending a "transferred" signal with the count of the sent items
	        new SendSignalAction(Signal.TRANSFERRED, getNameInfo().getTargetIntermediaryNodeName(),
	                count, signalCon).execute();
        }
//        if (count <= SwitchStates.getNumTransferredData()) {
//            if (null != logProtocol) {
//                logProtocol.createSignalSENDLog(SignalName.TRANSFERRED, count
//                        , getNameInfo().getTargetIntermediaryNodeName());
//            }
//            //sending a "transferred" signal with the count of the sent items
//            new SendSignalAction(Signal.TRANSFERRED, getNameInfo().getTargetIntermediaryNodeName(),
//                    count, signalCon).execute();
//        } else if (transferredId == 0) {
//            if (null != logProtocol) {
//                logProtocol.createSignalSENDLog(SignalName.TRANSFERRED, transferredId
//                        , getNameInfo().getTargetIntermediaryNodeName());
//            }
//            //sending a "transferred" signal with the id of the last transferred items
//            new SendSignalAction(Signal.TRANSFERRED, getNameInfo().getTargetIntermediaryNodeName(),
//                    transferredId, signalCon).execute();
//        } else {
//        	if (null != logProtocol) {
//                logProtocol.createSignalSENDLog(SignalName.COMPLETED, null
//                        , getNameInfo().getTargetIntermediaryNodeName());
//            }
//            new SendSignalAction(Signal.COMPLETED, SwitchNodeNameInfo.getInstance().getPrecedingNodeName(), 
//                    true, signalCon).execute();
//        }
        if (null != logProtocol) {
            logProtocol.createGENLog("The end of transferring all items to the target Spout.");
            logProtocol.createQUEUELog(QueueStatus.INPUT, inQueue.size());
            logProtocol.createQUEUELog(QueueStatus.OUTPUT, outQueue.size());
        }
    }

    /**
     * Send the tuple to the target node.
     * 
     * @param item
     *            the tuple
     */
    private void sendToTarget(ISwitchTuple item) {
        sender.send(serializer.serialize(item));
    }
    
    /**
     * Send the tuple to a target node randomly shuffled from a list of target nodes.
     * 
     * @param item
     *            the tuple
     */
    private void sendToTargetShuffle(ISwitchTuple item) {
        NodeHostStorm.shuffleSender(senders).send(serializer.serialize(item));
    }

    /**
     * Goes to passive state, i.e., the original algorithm is disactivating.
     */
    public void goToPassive() {
        outQueue.clear();
        SwitchStates.setPassivateOrgINT(true); // isPassivate = true;
        SwitchStates.setTransferringOrgINT(false); // isTransferring = false;
        if (null != logProtocol) {
            logProtocol.createGENLog("Go to passive and inform the end bolt.");
        }
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
