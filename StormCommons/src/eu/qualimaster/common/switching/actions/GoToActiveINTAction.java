package eu.qualimaster.common.switching.actions;

import org.apache.log4j.Logger;

import eu.qualimaster.common.signal.AbstractSignalConnection;
import eu.qualimaster.common.switching.SwitchNodeNameInfo;
/**
 * Provides the action for letting the target intermediary node go to the active mode.
 * @author Cui Qin
 *
 */
public class GoToActiveINTAction implements IAction {
    private static final Logger LOGGER = Logger.getLogger(GoToActiveINTAction.class);
    private AbstractSignalConnection signalCon;
    /**
     * Create an active to let the target algorithm to go to the active mode.
     * @param signalCon the signal connection used to send signals
     */
    public GoToActiveINTAction(AbstractSignalConnection signalCon) {
        this.signalCon = signalCon;
    }
    
    @Override
    public void execute() {
        goToActive();
    }
    
    /**
     * Goes to active state, i.e., the target algorithm is activating.
     */
    private void goToActive() {
        SwitchStates.setPassivateTrgINT(false); // isPassivate = false;
        SwitchStates.setTransferringTrgINT(false);
        SwitchStates.setActiveTrgINT(true); // isActiveSpout = true;
        SwitchStates.setFirstTupleId(0); // firstId = 0;
        LOGGER.info(System.currentTimeMillis() + ", Go to active and inform the end bolt.");
        new SendSignalAction(Signal.GOTOACTIVE, SwitchNodeNameInfo.getInstance().getTargetEndNodeName(), 
                true, signalCon).execute();
    }

}
