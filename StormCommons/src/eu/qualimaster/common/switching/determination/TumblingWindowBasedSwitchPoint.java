package eu.qualimaster.common.switching.determination;
/**
 * Calculate the switch point for the tumbling-window-based algorithm.
 * @author Cui Qin
 *
 */
public class TumblingWindowBasedSwitchPoint extends WindowBasedSwitchPoint {
    private long algStart;
    private long determinationBegin;
    
    /**
     * Constructor for the switch point of the tumbling window-based case.
     * @param algStart the starting point of the original algorithm
     * @param determinationBegin the point in time when determination starts
     * @param windowSize the sliding window size
     */
    public TumblingWindowBasedSwitchPoint(long algStart, long determinationBegin, long windowSize) {
        super(windowSize);
        this.algStart = algStart;
        this.determinationBegin = determinationBegin;
    }

    @Override
    public long determineSwitchPoint() {
        //The safepoint for the tumbling window-based case is at the end of the window.
        return determineWindowEndPoint(algStart, determinationBegin);
    }

}
