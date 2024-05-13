package eu.qualimaster.common.switching.determination;

import eu.qualimaster.common.signal.SignalStates;

/**
 * The determination strategy applying the switch point directly after the warm-up period 
 * with data synchronization (DSyn) existed.
 * @author Cui Qin
 *
 */
public class WarmupDeterminationSynStrategy extends WarmupDeterminationStrategy {

    /**
     * Constructor.
     * @param warmupDuration the warm-up duration
     */
    public WarmupDeterminationSynStrategy(long warmupDuration) {
        super(warmupDuration);
    }
    
    @Override
    public void uponSwitchPoint() {
        super.uponSwitchPoint();
        SignalStates.setEmitTrgPRE(false); //also disable the target one for data synchronization
    }
}
