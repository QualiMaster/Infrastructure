package eu.qualimaster.common.switching.tupleEmit;

import org.apache.log4j.Logger;

import eu.qualimaster.base.algorithm.ISwitchTuple;
import eu.qualimaster.common.signal.SignalStates;
import eu.qualimaster.common.switching.QueueHolder;
import eu.qualimaster.common.switching.SwitchNodeNameInfo;
import eu.qualimaster.common.switching.SynchronizedQueue;

/**
 * Provide a tuple emit strategy for the case of using separated intermediary
 * nodes.
 * 
 * @author Cui Qin
 *
 */
public class SeparatedOrgINTTupleEmitStrategy extends AbstractTupleEmitStrategy {
    public static final String STRATEGYTYPE = AbstractTupleEmitStrategy.STRATEGYTYPE
            + SwitchNodeNameInfo.ORIGINALINTERMEDIARYNODE;
    private static final Logger LOGGER = Logger.getLogger(SeparatedOrgINTTupleEmitStrategy.class);
    private SynchronizedQueue<ISwitchTuple> synInQueue;
    private boolean flag = true;

    /**
     * Constructor for the tuple emit strategy in the original intermediary node.
     * 
     * @param queueHolder
     *            the queue holder.
     */
    public SeparatedOrgINTTupleEmitStrategy(QueueHolder queueHolder) {
        super(queueHolder);
        synInQueue = new SynchronizedQueue<ISwitchTuple>(getInQueue(), SignalStates.getSynQueueSizeOrgINT());
    }

    @Override
    public String getStrategyType() {
        return STRATEGYTYPE;
    }
    
    @Override
    public ISwitchTuple nextEmittedTuple() {
        ISwitchTuple result = null;
        if ((!getInQueue().isEmpty()) && (!SignalStates.isPassivateOrgINT())) {
            result = synInQueue.consume();
            if (result.getId() != 0L) { // queue only during the switch
                getOutQueue().offer(result);
                LOGGER.info("Store the tuple in the outQueue: " + getOutQueue().size());
            }
        }
        if (flag) {
            flag = false;
            LOGGER.info("Set the starting point when the original algorithm starts to process: "
                    + System.currentTimeMillis());
            SignalStates.setAlgStartPoint(System.currentTimeMillis());
        }
        return result;
    }
}
