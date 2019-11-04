package eu.qualimaster.common.switching.actions;

import org.apache.log4j.Logger;

import eu.qualimaster.common.signal.AbstractSignalConnection;
import eu.qualimaster.common.switching.SwitchNodeNameInfo;
/**
 * Provide the action for completing the algorithm switch.
 * @author Cui Qin
 *
 */
public class CompleteSwitchAction implements IAction {
    private static final Logger LOGGER = Logger.getLogger(CompleteSwitchAction.class);
    private AbstractSignalConnection signalCon;
    
    /**
     * Create an action for completing the algorithm switch.
     * @param signalCon the signal connection used to send signals
     */
    public CompleteSwitchAction(AbstractSignalConnection signalCon) {
        this.signalCon = signalCon;
    }
    
    @Override
    public void execute() {
        completingSynchronization();
    }
    
    /**
     * Completing the synchronization.
     */
    private void completingSynchronization() {
        LOGGER.info(System.currentTimeMillis() + ", Sending the synchronized signal to the preceding node!");
        new SendSignalAction(Signal.COMPLETED, SwitchNodeNameInfo.getInstance().getPrecedingNodeName(), 
                true, signalCon).execute();
        if (!SwitchStates.isPassivateTrgINT()) {
            new GoToActiveINTAction(signalCon).execute();
        } else {
            SwitchStates.setTransferringTrgINT(false);
            SwitchStates.setActiveTrgINT(true); // isActiveSpout = true;
            SwitchStates.setFirstTupleId(0); // firstId = 0;
        }
    }
    
}
