package eu.qualimaster.common.switching.tupleEmit;

import eu.qualimaster.base.algorithm.ISwitchTuple;
import eu.qualimaster.common.switching.QueueHolder;
import eu.qualimaster.common.switching.SwitchNodeNameInfo;
import eu.qualimaster.common.switching.SynchronizedQueue;
import eu.qualimaster.common.switching.actions.SwitchActionMap;
import eu.qualimaster.common.switching.actions.SwitchStates;
import eu.qualimaster.common.switching.actions.ActionState;
import switching.logging.LogProtocol;

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
    private SynchronizedQueue<ISwitchTuple> synInQueue;
    private SwitchActionMap switchActionMap;
    private LogProtocol logProtocol = null;
    private boolean flag = true;
    
    /**
     * Constructor for the tuple emit strategy in the original intermediary node.
     * 
     * @param queueHolder
     *            the queue holder.
     * @param switchActionMap the map containing the switch actions.
     * @param logProtocol the log protocol used to write logs into corresponding files.
     */
    public SeparatedOrgINTTupleEmitStrategy(QueueHolder queueHolder, SwitchActionMap switchActionMap, 
            LogProtocol logProtocol) {
        this(queueHolder, switchActionMap);
        this.logProtocol = logProtocol;
    }
    
    /**
     * Constructor for the tuple emit strategy in the original intermediary node.
     * 
     * @param queueHolder
     *            the queue holder.
     * @param switchActionMap the map containing the switch actions.
     */
    public SeparatedOrgINTTupleEmitStrategy(QueueHolder queueHolder, SwitchActionMap switchActionMap) {
        super(queueHolder);
        synInQueue = new SynchronizedQueue<ISwitchTuple>(getInQueue(), SwitchStates.getSynQueueSizeOrgINT());
        this.switchActionMap = switchActionMap;
    }
    
    @Override
    public String getStrategyType() {
        return STRATEGYTYPE;
    }
    
    @Override
    public ISwitchTuple nextEmittedTuple() {
        ISwitchTuple result = null;
        
        if (!SwitchStates.isDetermined() && SwitchStates.getSwitchPoint() != 0L 
                && System.currentTimeMillis() >= SwitchStates.getSwitchPoint()) {
            if (null != logProtocol) { //write logs into a file.
                logProtocol.createSWDeterminedLog();
                logProtocol.createSAFEPOINTLog();
            }
            switchActionMap.executeActions(ActionState.SWITCH_POINT_REACHED, null, true, logProtocol);
            SwitchStates.setDetermined(true);
        }

        if ((!getInQueue().isEmpty()) && (!SwitchStates.isPassivateOrgINT())) {
            result = synInQueue.consume();
            //!!!this part moved to the pipeline to ensure the stored tuples are really emitted to the algorithm
//            if (result.getId() != 0L) { // queue only during the switch
//                getOutQueue().offer(result);
//                if (null != logProtocol) {
//                    logProtocol.createGENLog("Store the tuple in the outQueue and its size: " + getOutQueue().size());
//                }
//            }
        }
        if (flag) {
            flag = false;
            if (SwitchStates.getAlgStartPoint() == 0)  {
	            SwitchStates.setAlgStartPoint(System.currentTimeMillis());
	            if (null != logProtocol) {
	                logProtocol.createGENLog(System.currentTimeMillis() + ", Set the starting point when the original algorithm starts to process: "
	                        + SwitchStates.getAlgStartPoint());
	            }
            }
        }
        return result;
    }
    
}
