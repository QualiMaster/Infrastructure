package eu.qualimaster.common.switching.determination;

/**
 * Define an abstract determination strategy.
 * @author Cui Qin
 *
 */
public abstract class AbstractDeterminationStrategy implements IDeterminationStrategy {
    private static final String STRATEGYTYPE = "determination";
    private static boolean isDetermined = false;
    private long switchPoint = 0L;
    
    @Override
    public String getStrategyType() {
        return STRATEGYTYPE;
    }
    
    /**
     * Check if the safe point is already determined.
     * @return <code>true</code> if determined, otherwise <code>false</code>
     */
    public static boolean isDetermined() {
        return isDetermined;
    }

    /**
     * Sets the state if the safe point is determined.
     * @param isDetermined <code>true</code> if determined, otherwise <code>false</code>
     */
    public static void setDetermined(boolean isDetermined) {
        AbstractDeterminationStrategy.isDetermined = isDetermined;
    }
    
    @Override
    public void waitForSwitchPoint() {
        long current = System.currentTimeMillis();
        if (switchPoint != 0L && current >= switchPoint) {
            uponSwitchPoint();
            isDetermined = true;
        }
    }
    
    /**
     * Returns the switch point.
     * @return the switch point
     */
    public long getSwitchPoint() {
        return switchPoint;
    }
    
    /**
     * Sets the switch point.
     * @param switchPoint the switch point
     */
    public void setSwitchPoint(long switchPoint) {
        this.switchPoint = switchPoint;
    }
}
