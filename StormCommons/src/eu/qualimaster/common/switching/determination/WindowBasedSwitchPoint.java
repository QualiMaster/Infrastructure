package eu.qualimaster.common.switching.determination;
/**
 * Provide a common class for calculating the switch point for window-based algorithms.
 * @author Cui Qin
 *
 */
public abstract class WindowBasedSwitchPoint implements ISwitchPoint {
    private long windowSize;
    /**
     * Constructor for the sliding window-based end point.
     * @param windowSize the window size
     */
    public WindowBasedSwitchPoint(long windowSize) {
        this.windowSize = windowSize;
    }
    
    /**
     * Determine a point where the first window after the arrival of the switch ends.
     * @param start the start point in time when the window-based processing starts 
     * @param end the end point in time when the determination is starting   
     * @return the point of the window end
     */
    public long determineWindowEndPoint(long start, long end) {
        long endPoint = 0;
        double m = Math.ceil((Double.valueOf(String.valueOf(end)) 
                - Double.valueOf(String.valueOf(start))) / windowSize);
        endPoint = (long) (start + m * windowSize);
        return endPoint;
    }

}
