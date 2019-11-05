package eu.qualimaster.common.switching.tupleEmit;

import java.util.List;
import java.util.Map;

import eu.qualimaster.base.algorithm.ISwitchTuple;
import eu.qualimaster.common.switching.QueueHolder;
import eu.qualimaster.common.switching.SwitchNodeNameInfo;
import eu.qualimaster.common.switching.SynchronizedQueue;
import eu.qualimaster.common.switching.actions.IAction;
import eu.qualimaster.common.switching.actions.SwitchStates;
import eu.qualimaster.common.switching.actions.SwitchStates.ActionState;
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
    private Map<ActionState, List<IAction>> actionMap;
    private LogProtocol logProtocol = null;
    private boolean flag = true;
    private boolean isDetermined = false;
    
    /**
     * Constructor for the tuple emit strategy in the original intermediary node.
     * 
     * @param queueHolder
     *            the queue holder.
     * @param actionMap the map containing the switch actions.
     * @param logProtocol the log protocol used to write logs into corresponding files.
     */
    public SeparatedOrgINTTupleEmitStrategy(QueueHolder queueHolder, Map<ActionState, List<IAction>> actionMap, 
            LogProtocol logProtocol) {
        this(queueHolder, actionMap);
        this.logProtocol = logProtocol;
    }
    
    /**
     * Constructor for the tuple emit strategy in the original intermediary node.
     * 
     * @param queueHolder
     *            the queue holder.
     * @param actionMap the map containing the switch actions.
     */
    public SeparatedOrgINTTupleEmitStrategy(QueueHolder queueHolder, Map<ActionState, List<IAction>> actionMap) {
        super(queueHolder);
        synInQueue = new SynchronizedQueue<ISwitchTuple>(getInQueue(), SwitchStates.getSynQueueSizeOrgINT());
        this.actionMap = actionMap;
    }
    
    @Override
    public String getStrategyType() {
        return STRATEGYTYPE;
    }
    
    @Override
    public ISwitchTuple nextEmittedTuple() {
        ISwitchTuple result = null;
        
        if (!isDetermined && SwitchStates.getSwitchPoint() != 0L 
                && System.currentTimeMillis() >= SwitchStates.getSwitchPoint()) {
            if (null != logProtocol) { //write logs into a file.
                logProtocol.createSAFEPOINTLog();
            }
            SwitchStates.executeActions(ActionState.SWITCH_POINT_REACHED, actionMap, null, logProtocol);
            isDetermined = true;
        }
        
        if ((!getInQueue().isEmpty()) && (!SwitchStates.isPassivateOrgINT())) {
            result = synInQueue.consume();
            if (result.getId() != 0L) { // queue only during the switch
                getOutQueue().offer(result);
                if (null != logProtocol) {
                    logProtocol.createGENLog("Store the tuple in the outQueue and its size: " + getOutQueue().size());
                }
            }
        }
        if (flag) {
            flag = false;
            if (null != logProtocol) {
                logProtocol.createGENLog("Set the starting point when the original algorithm starts to process: "
                        + System.currentTimeMillis());
            }
            SwitchStates.setAlgStartPoint(System.currentTimeMillis());
        }
        return result;
    }
    
}
