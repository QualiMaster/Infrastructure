package eu.qualimaster.common.switching.tupleReceiving;

import java.io.IOException;

import org.apache.log4j.Logger;

import eu.qualimaster.base.algorithm.ISwitchTuple;
import eu.qualimaster.base.serializer.ISwitchTupleSerializer;
import eu.qualimaster.common.signal.SignalStates;
import eu.qualimaster.common.switching.QueueHolder;
import eu.qualimaster.common.switching.SwitchNodeNameInfo;
import eu.qualimaster.common.switching.SynchronizedQueue;

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

    /**
     * Constructor for the tuple receiving strategy in the target intermediary node.
     * @param queueHolder the queue holder
     * @param serializer the serializer for deserializing received data
     */
    public SeparatedTrgINTTupleReceiveStrategy(QueueHolder queueHolder, ISwitchTupleSerializer serializer) {
        super(serializer);
        this.synInQueue = new SynchronizedQueue<ISwitchTuple>(queueHolder.getInQueue(),
                SignalStates.getSynQueueSizeTrgINT());
        this.synTmpQueue = new SynchronizedQueue<ISwitchTuple>(queueHolder.getTmpQueue(),
                SignalStates.getSynQueueSizeTrgINT());
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
            result =  new SeparatedTupleReceiverHandler(synInQueue, synTmpQueue, getSerializer());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

}
