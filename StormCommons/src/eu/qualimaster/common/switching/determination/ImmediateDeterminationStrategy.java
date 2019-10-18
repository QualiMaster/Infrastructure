package eu.qualimaster.common.switching.determination;

import eu.qualimaster.common.signal.SignalStates;

/**
 * The determination strategy applying immediate switch point.
 * @author Cui Qin
 *
 */
public class ImmediateDeterminationStrategy extends AbstractDeterminationStrategy {
    private ImmediateSwitchPoint switchPoint;
    
    /**
     * Constructor.
     */
    public ImmediateDeterminationStrategy() {
        switchPoint = new ImmediateSwitchPoint();
    }
    
    @Override
    public void determineSwitchPoint() {
        setSwitchPoint(switchPoint.determineSwitchPoint());
    }

    @Override
    public void uponSwitchPoint() {
        //redirect the data stream from the original algorithm to the target algorithm
        SignalStates.setEmitOrgPRE(false);
        SignalStates.setEmitTrgPRE(true);
    }

}
