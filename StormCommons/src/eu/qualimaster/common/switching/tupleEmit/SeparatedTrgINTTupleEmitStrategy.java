package eu.qualimaster.common.switching.tupleEmit;

import org.apache.log4j.Logger;

import eu.qualimaster.base.algorithm.ISwitchTuple;
import eu.qualimaster.common.switching.QueueHolder;
import eu.qualimaster.common.switching.SwitchNodeNameInfo;
import eu.qualimaster.common.switching.SynchronizedQueue;
import eu.qualimaster.common.switching.actions.SwitchStates;

/**
 * Provide a tuple emit strategy for the target intermediary node.
 * @author Cui Qin
 *
 */
public class SeparatedTrgINTTupleEmitStrategy extends AbstractTupleEmitStrategy {
    public static final String STRATEGYTYPE = AbstractTupleEmitStrategy.STRATEGYTYPE
            + SwitchNodeNameInfo.TARGETINTERMEDIARYNODE;
    private static final Logger LOGGER = Logger.getLogger(SeparatedTrgINTTupleEmitStrategy.class);
    private SynchronizedQueue<ISwitchTuple> synInQueue;
    private SynchronizedQueue<ISwitchTuple> synTmpQueue;
    
    /**
     * Constructor for the tuple emit strategy in the target intermediary node.
     * 
     * @param queueHolder
     *            the queue holder.
     */
    public SeparatedTrgINTTupleEmitStrategy(QueueHolder queueHolder) {
        super(queueHolder);
        synInQueue = new SynchronizedQueue<ISwitchTuple>(getInQueue(), SwitchStates.getSynQueueSizeTrgINT());
        synTmpQueue = new SynchronizedQueue<ISwitchTuple>(getTmpQueue(), SwitchStates.getSynQueueSizeTrgINT());
    }

    @Override
    public String getStrategyType() {
        return STRATEGYTYPE;
    }
    
    @Override
    public ISwitchTuple nextEmittedTuple() {
        ISwitchTuple result = null;
        if (SwitchStates.isTransferringTrgINT() || (!getTmpQueue().isEmpty()) && (!SwitchStates.isPassivateTrgINT())) {
            if (!getTmpQueue().isEmpty()) {
                result = synTmpQueue.consume();
            }
        } else if ((!getInQueue().isEmpty()) && (!SwitchStates.isPassivateTrgINT())) {
            result = synInQueue.consume();
            if (result.getId() != 0L) { // queue only during the switch
                getOutQueue().offer(result);
                LOGGER.info("Store the tuple in the outQueue: " + getOutQueue().size());
            }
        }
        return result;
    }
}
