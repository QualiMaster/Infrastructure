package eu.qualimaster.common.switching.determination;
/**
 * An interface for determining the safe point.
 * @author Cui Qin
 *
 */
public interface ISafePoint {
    /**
     * Determine the safe point.
     * @return the safe point
     */
    public long determineSafePoint();

}
