package eu.qualimaster.common.switching.tupleReceiving;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import eu.qualimaster.base.algorithm.ISwitchTuple;
import eu.qualimaster.base.serializer.ISwitchTupleSerializer;
import eu.qualimaster.common.signal.AbstractSignalConnection;
import eu.qualimaster.common.switching.QueueHolder;
import eu.qualimaster.common.switching.SwitchNodeNameInfo;
import eu.qualimaster.common.switching.SynchronizedQueue;
import eu.qualimaster.common.switching.actions.IAction;
import eu.qualimaster.common.switching.actions.SwitchStates;
import eu.qualimaster.common.switching.actions.SwitchStates.ActionState;
import switching.logging.LogProtocol;

/**
 * Provide a tuple receiving strategy for the original intermediary node in the
 * separated case.
 * 
 * @author Cui Qin
 *
 */
public class SeparatedOrgINTTupleReceiveStrategy extends AbstractTupleReceiveStrategy {
    public static final String STRATEGYTYPE = AbstractTupleReceiveStrategy.STRATEGYTYPE
            + SwitchNodeNameInfo.ORIGINALINTERMEDIARYNODE;
    private static final Logger LOGGER = Logger.getLogger(SeparatedOrgINTTupleReceiveStrategy.class);
    private SynchronizedQueue<ISwitchTuple> synInQueue;
    private SynchronizedQueue<ISwitchTuple> synTmpQueue;
    private LogProtocol logProtocol;
    
    /**
     * Constructor for the tuple receiving strategy in the original intermediary
     * node.
     * 
     * @param queueHolder
     *            the queue holder
     * @param serializer
     *            the serializer for deserializing received data
     * @param signalCon the signal connection used to send signals
     * @param actionMap the map containing the switch actions
     * @param logProtocol the log protocol used to write logs into corresponding files
     */
    public SeparatedOrgINTTupleReceiveStrategy(QueueHolder queueHolder, ISwitchTupleSerializer serializer,
            AbstractSignalConnection signalCon, Map<ActionState, List<IAction>> actionMap, LogProtocol logProtocol) {
        this(queueHolder, serializer, signalCon, actionMap);
        this.logProtocol = logProtocol;
    }
    
    /**
     * Constructor for the tuple receiving strategy in the original intermediary
     * node.
     * 
     * @param queueHolder
     *            the queue holder
     * @param serializer
     *            the serializer for deserializing received data
     * @param signalCon the signal connection used to send signals
     * @param actionMap the map containing the switch actions
     */
    public SeparatedOrgINTTupleReceiveStrategy(QueueHolder queueHolder, ISwitchTupleSerializer serializer,
            AbstractSignalConnection signalCon, Map<ActionState, List<IAction>> actionMap) {
        super(serializer, signalCon, actionMap);
        this.synInQueue = new SynchronizedQueue<ISwitchTuple>(queueHolder.getInQueue(),
                SwitchStates.getSynQueueSizeOrgINT());
        this.synTmpQueue = new SynchronizedQueue<ISwitchTuple>(queueHolder.getTmpQueue(),
                SwitchStates.getSynQueueSizeOrgINT());
    }

    @Override
    public String getStrategyType() {
        return STRATEGYTYPE;
    }
    
    @Override
    public ITupleReceiverHandler createHandler() {
        ITupleReceiverHandler result = null;
        try {
            LOGGER.info("Creating a handler for tuple receive.");
            result = new SeparatedTupleReceiverHandler(synInQueue, synTmpQueue, getSerializer(),
                        getSignalCon(), getActionMap(), logProtocol); 
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

}
