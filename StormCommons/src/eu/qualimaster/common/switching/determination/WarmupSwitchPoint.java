package eu.qualimaster.common.switching.determination;
/**
 * The switch point determined by the warm-up duration.
 * @author Cui Qin
 *
 */
public class WarmupSwitchPoint implements ISwitchPoint {
    private long warmupDuration;
    
    /**
     * Constructor of the warm-up-based switch point.
     * @param warmupDuration the warm-up duration
     */
    public WarmupSwitchPoint(long warmupDuration) {
        this.warmupDuration = warmupDuration;
    }
    
    @Override
    public long determineSwitchPoint() {
        return this.warmupDuration;
    }

}
