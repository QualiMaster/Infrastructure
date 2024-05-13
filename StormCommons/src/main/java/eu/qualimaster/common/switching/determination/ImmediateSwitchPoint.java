package eu.qualimaster.common.switching.determination;
/**
 * Provide a switch point determined immediately.
 * @author Cui Qin
 *
 */
public class ImmediateSwitchPoint implements ISwitchPoint {

    @Override
    public long determineSwitchPoint() {
        return System.currentTimeMillis();
    }

}
