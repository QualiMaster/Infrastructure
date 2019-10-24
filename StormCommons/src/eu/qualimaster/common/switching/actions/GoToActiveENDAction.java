package eu.qualimaster.common.switching.actions;
/**
 * Provides the action for letting the target end node go to the active mode.
 * @author Cui Qin
 *
 */
public class GoToActiveENDAction implements IAction {

    @Override
    public void execute() {
        SwitchStates.setEmitTrgEND(true);
        SwitchStates.setActiveTrgEND(true);
    }

}
