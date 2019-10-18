package eu.qualimaster.common.switching.determination;

import eu.qualimaster.common.signal.SignalStates;

/**
 * The determination strategy applying the switch point directly after the warm-up period.
 * @author Cui Qin
 *
 */
public class WarmupDeterminationStrategy extends AbstractDeterminationStrategy {
    private WarmupSwitchPoint switchPoint;
    
    /**
     * Constructor.
     * @param warmupDuration the warm-up duration
     */
    public WarmupDeterminationStrategy(long warmupDuration) {
        switchPoint = new WarmupSwitchPoint(warmupDuration);
    }
    
    @Override
    public void determineSwitchPoint() {
        SignalStates.setEmitTrgPRE(true); //start warm-up phase
        setSwitchPoint(switchPoint.determineSwitchPoint());
    }

    @Override
    public void uponSwitchPoint() {
        SignalStates.setEmitOrgPRE(false);
    }

}
