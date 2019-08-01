package eu.qualimaster.common.switching.determination;
/**
 * Determine an end point of a sliding window.
 * @author Cui Qin
 *
 */
public class SlidingWindowEndPoint implements IWindowEndPoint {
    private long windowSize;
    /**
     * Constructor for the sliding window-based end point.
     * @param windowSize the window size
     */
    public SlidingWindowEndPoint(long windowSize) {
        this.windowSize = windowSize;
    }
    
    @Override
    public long determineWindowEndPoint(long start, long end) {
        long endPoint = 0;
        double m = Math.ceil((Double.valueOf(String.valueOf(end)) 
                - Double.valueOf(String.valueOf(start))) / windowSize);
        endPoint = (long) (start + m * windowSize);
        return endPoint;
    }

}
