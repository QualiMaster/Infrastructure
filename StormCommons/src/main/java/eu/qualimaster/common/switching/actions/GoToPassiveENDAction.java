package eu.qualimaster.common.switching.actions;
/**
 * Provides the action for letting the original end node to go to passive mode.
 * @author Cui Qin
 *
 */
public class GoToPassiveENDAction implements IAction {

    @Override
    public void execute() {
        SwitchStates.setEmitOrgEND(false);
        SwitchStates.setActiveOrgEND(false);
    }
}
