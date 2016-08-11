package eu.qualimaster.common.switching;
/**
 * The state of the intermediary node.
 * @author Cui Qin
 *
 */
public interface IState {
    /**
     * The state involved the switch mechanism in the intermediary node.
     * @author Cui Qin
     *
     */
    public enum SwitchState {
        ACTIVE_DEFAULT,
        PASSIVE_DEFAULT
    }
}
