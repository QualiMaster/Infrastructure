package eu.qualimaster.common.switching.determination;

import org.apache.log4j.Logger;

import eu.qualimaster.common.switching.actions.SwitchStates;

/**
 * Calculate the switch point for the tumbling-window-based algorithm.
 * @author Cui Qin
 *
 */
public class TumblingWindowBasedSwitchPoint extends AbstractWindowBasedSwitchPoint {
    private static Logger logger = Logger.getLogger(TumblingWindowBasedSwitchPoint.class);
    private long windowSize;
    private long algStart;
    private long determinationBegin;
    
    /**
     * Constructor for the switch point of the tumbling window-based case.
     * @param windowSize the sliding window size
     */
    public TumblingWindowBasedSwitchPoint(long windowSize) {
    	this.windowSize = windowSize;
    }
    
    /**
     * Constructor for the switch point of the tumbling window-based case.
     * @param algStart the starting point of the original algorithm
     * @param determinationBegin the point in time when determination starts
     * @param windowSize the sliding window size
     */
    public TumblingWindowBasedSwitchPoint(long algStart, long determinationBegin, long windowSize) {
    	this.windowSize = windowSize;
        this.algStart = algStart;
        this.determinationBegin = determinationBegin;
    }

    
    @Override
    public long determineSwitchPoint() {
        return determineWindowEnd();
    }

	@Override
	protected long determineWindowEnd() {
		long endPoint = 0;
		if (0 == algStart) {
            algStart = SwitchStates.getAlgStartPoint();
        }
        if (0 == determinationBegin) {
            determinationBegin = SwitchStates.getDeterminationBegin();
        }
        if (0 != algStart && 0 != determinationBegin) {
        	double m = Math.ceil((Double.valueOf(String.valueOf(determinationBegin)) 
                    - Double.valueOf(String.valueOf(algStart))) / windowSize);
            endPoint = (long) (algStart + m * windowSize);
        } else {
            logger.error("The algorithm start point or the determination begin point is not initialized!");
        }
		return endPoint;
	}

}
