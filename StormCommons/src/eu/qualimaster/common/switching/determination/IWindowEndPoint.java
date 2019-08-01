package eu.qualimaster.common.switching.determination;
/**
 * An interface for determining the end point of the window.
 * @author Cui Qin
 *
 */
public interface IWindowEndPoint {
    /**
     * Determine a point where the first window after the arrival of the switch ends.
     * @param start the start point in time when the window-based processing starts 
     * @param end the end point in time when the determination is starting   
     * @return the point of the window end
     */
    public long determineWindowEndPoint(long start, long end);
}
