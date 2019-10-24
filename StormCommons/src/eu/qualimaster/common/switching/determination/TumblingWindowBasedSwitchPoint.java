package eu.qualimaster.common.switching.determination;

import org.apache.log4j.Logger;

import eu.qualimaster.common.switching.actions.SwitchStates;

/**
 * Calculate the switch point for the tumbling-window-based algorithm.
 * @author Cui Qin
 *
 */
public class TumblingWindowBasedSwitchPoint extends WindowBasedSwitchPoint {
    private static Logger logger = Logger.getLogger(TumblingWindowBasedSwitchPoint.class);
    private long algStart;
    private long determinationBegin;
    
    /**
     * Constructor for the switch point of the tumbling window-based case.
     * @param windowSize the sliding window size
     */
    public TumblingWindowBasedSwitchPoint(long windowSize) {
        super(windowSize);
    }
    
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
        long switchPoint = 0;
        if (0 == algStart) {
            algStart = SwitchStates.getAlgStartPoint();
        }
        if (0 == determinationBegin) {
            determinationBegin = SwitchStates.getDeterminationBegin();
        }
        if (0 != algStart && 0 != determinationBegin) {
            //The safepoint for the tumbling window-based case is at the end of the window.
            switchPoint = determineWindowEndPoint(algStart, determinationBegin);
        } else {
            logger.error("The algorithm start point or the determination begin point is not initialized!");
        }
        return switchPoint;
    }

}
