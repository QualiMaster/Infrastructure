package eu.qualimaster.common.switching.determination;

import org.apache.log4j.Logger;

import eu.qualimaster.common.switching.actions.SwitchStates;
/**
 * Calculate the switch point for the sliding-window-based algorithm.
 * @author Cui Qin
 *
 */
public class SlidingWindowBasedSwitchPoint extends AbstractWindowBasedSwitchPoint {
    private static Logger logger = Logger.getLogger(SlidingWindowBasedSwitchPoint.class);
    private long algStart;
    private long windowSize;
    private long slidingStep;
    private long determinationBegin;
   
    /**
     * Constructor for the switch point of the sliding window-based case.
     * @param windowSize the sliding window size
     * @param slidingStep the sliding window step
     */
    public SlidingWindowBasedSwitchPoint(long windowSize, long slidingStep) {
        this.windowSize = windowSize;
        this.slidingStep = slidingStep;
    }
    
    /**
     * Constructor for the switch point of the sliding window-based case.
     * @param algStart the starting point of the original algorithm
     * @param determinationBegin the point in time when determination starts (including warm-up time)
     * @param windowSize the sliding window size
     * @param slidingStep the sliding window step
     */
    public SlidingWindowBasedSwitchPoint(long algStart, long determinationBegin, long windowSize, long slidingStep) {
        this.algStart = algStart;
        this.determinationBegin = determinationBegin;
        this.windowSize = windowSize;
        this.slidingStep = slidingStep;
    }

    @Override
    public long determineSwitchPoint() {
        long switchPoint = 0;
        if (0 == determinationBegin) {
            determinationBegin = SwitchStates.getDeterminationBegin();
        }
        if (0 == algStart) {
        	algStart = SwitchStates.getAlgStartPoint();
        }
        logger.info("The algorithm start:" + algStart + ", the determination begin:" + determinationBegin + ", the window size:" + windowSize + ", the sliding step:" + slidingStep);
        if (0 != algStart && 0 != determinationBegin) {
            //determine the end point in the current window
            long windowEnd = determineWindowEnd();
            logger.info("The window end: " + windowEnd);
            if ((windowEnd - determinationBegin) <= windowSize - slidingStep) { //overlapping part in window
                double k = Math.floor((determinationBegin - (windowEnd - windowSize)) / slidingStep);
                switchPoint = (long) (windowEnd + k * slidingStep); 
                logger.info(System.currentTimeMillis() + ", is the switch point -- overlapping part: " + switchPoint);
            } else { //non-overlapping part in window
                switchPoint = windowEnd;
                logger.info(System.currentTimeMillis() 
                        + ", is the switch point -- non-overlapping part: " + switchPoint);
            }
        } else {
            logger.error("The algorithm start point or the determination begin point is not initialized!");
            logger.info("The algStart:" + algStart + ", the determinationBegin: " + determinationBegin);
        }
        return switchPoint;
    }

	@Override
	protected long determineWindowEnd() {
		long endPoint = 0;
        if (0 == determinationBegin) {
            determinationBegin = SwitchStates.getDeterminationBegin();
        }
        if (0 != algStart && 0 != determinationBegin) {
            double m = Math.floor((Double.valueOf(String.valueOf(determinationBegin))
            		-Double.valueOf(String.valueOf(algStart)))/slidingStep);
            if (m >= 1) {
            	endPoint = (long) (algStart + windowSize + (m-1) * slidingStep);
            } else {
            	endPoint = (long) (algStart + windowSize);
            }
        } else {
            logger.error("The determination begin point is not initialized!");
        }
		return endPoint;
	}

}
