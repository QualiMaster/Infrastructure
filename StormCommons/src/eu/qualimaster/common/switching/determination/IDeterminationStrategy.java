package eu.qualimaster.common.switching.determination;

import eu.qualimaster.common.switching.IStrategy;

/**
 * Define an interface for the determination strategy.
 * @author Cui Qin
 *
 */
public interface IDeterminationStrategy extends IStrategy {
    /**
     * Determines the switch point in timestamp indicating the point in time to execute the switch.
     */
    public void determineSwitchPoint();
    
    /**
     * Waits for the switch point.
     */
    public void waitForSwitchPoint();
    
    /**
     * Reacts on the switch point.
     */
    public void uponSwitchPoint();
}
