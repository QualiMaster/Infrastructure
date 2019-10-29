package eu.qualimaster.common.switching.tupleReceiving;

import java.io.IOException;
import java.io.PrintWriter;
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

/**
 * Provide a tuple receiving strategy for the target intermediary node in the
 * separated case.
 * 
 * @author Cui Qin
 *
 */
public class SeparatedTrgINTTupleReceiveStrategy extends AbstractTupleReceiveStrategy {
    public static final String STRATEGYTYPE = AbstractTupleReceiveStrategy.STRATEGYTYPE
            + SwitchNodeNameInfo.TARGETINTERMEDIARYNODE;
    private static final Logger LOGGER = Logger.getLogger(SeparatedTrgINTTupleReceiveStrategy.class);
    private SynchronizedQueue<ISwitchTuple> synInQueue;
    private SynchronizedQueue<ISwitchTuple> synTmpQueue;
    private PrintWriter out;

    /**
     * Constructor for the tuple receiving strategy in the target intermediary node.
     * @param queueHolder the queue holder
     * @param serializer the serializer for deserializing received data
     * @param signalCon the signal connection used to send signals
     * @param actionMap the map containing the switch actions
     * @param out the log writer used to write logs into files
     */
    public SeparatedTrgINTTupleReceiveStrategy(QueueHolder queueHolder, ISwitchTupleSerializer serializer,
            AbstractSignalConnection signalCon, Map<ActionState, List<IAction>> actionMap, PrintWriter out) {
        this(queueHolder, serializer, signalCon, actionMap);
        this.out = out;
    }
    
    /**
     * Constructor for the tuple receiving strategy in the target intermediary node.
     * @param queueHolder the queue holder
     * @param serializer the serializer for deserializing received data
     * @param signalCon the signal connection used to send signals
     * @param actionMap the map containing the switch actions
     */
    public SeparatedTrgINTTupleReceiveStrategy(QueueHolder queueHolder, ISwitchTupleSerializer serializer,
            AbstractSignalConnection signalCon, Map<ActionState, List<IAction>> actionMap) {
        super(serializer, signalCon, actionMap);
        this.synInQueue = new SynchronizedQueue<ISwitchTuple>(queueHolder.getInQueue(),
                SwitchStates.getSynQueueSizeTrgINT());
        this.synTmpQueue = new SynchronizedQueue<ISwitchTuple>(queueHolder.getTmpQueue(),
                SwitchStates.getSynQueueSizeTrgINT());
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
            result =  new SeparatedTupleReceiverHandler(synInQueue, synTmpQueue, getSerializer(),
                    getSignalCon(), getActionMap(), out);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

}
