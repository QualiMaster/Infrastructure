package eu.qualimaster.common.switching.determination;

import org.apache.log4j.Logger;

/**
 * The safe point for the sliding window-based early warm-up case.
 * @author Cui Qin
 *
 */
public class SlidingWindowBasedEarlyWarmupSafePoint implements ISafePoint {
    private static Logger logger = Logger.getLogger(SlidingWindowBasedEarlyWarmupSafePoint.class);
    private long algStart;
    private long windowSize;
    private long slidingStep;
    private long warmupDuration;
    private long switchArrivalPoint;
    private SlidingWindowEndPoint endPoint;
    
    /**
     * Constructor for the safe point of the sliding window-based warly warm-up case.
     * @param algStart the starting point of the original algorithm
     * @param switchArrivalPoint the point in time when the switch is arrived
     * @param warmupDuration the warm-up duration
     * @param windowSize the sliding window size
     * @param slidingStep the sliding window step
     */
    public SlidingWindowBasedEarlyWarmupSafePoint(long algStart, long switchArrivalPoint
            , long warmupDuration, long windowSize, long slidingStep) {
        this.algStart = algStart;
        this.switchArrivalPoint = switchArrivalPoint;
        this.warmupDuration = warmupDuration;
        this.windowSize = windowSize;
        this.slidingStep = slidingStep;
        endPoint = new SlidingWindowEndPoint(windowSize);
    }
    
    @Override
    public long determineSafePoint() {
        long safepoint = 0;
        long end = switchArrivalPoint + warmupDuration;
        //determine the end point in the current window
        long windowEnd = endPoint.determineWindowEndPoint(algStart, end);
        if ((windowEnd - end) <= windowSize - slidingStep) { //overlapping part in window
            double k = Math.ceil((end - (windowEnd - windowSize)) / slidingStep);
            safepoint = (long) (end + k * windowSize); 
            logger.info(System.currentTimeMillis() + ", is the safepoint -- overlapping part: " + safepoint);
        } else { //non-overlapping part in window
            safepoint = windowEnd;
            logger.info(System.currentTimeMillis() + ", is the safepoint -- non-overlapping part: " + safepoint);
        }
        return safepoint;
    }

}
