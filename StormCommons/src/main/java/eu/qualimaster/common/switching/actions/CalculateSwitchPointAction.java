package eu.qualimaster.common.switching.actions;

import eu.qualimaster.common.switching.determination.ISwitchPoint;
/**
 * Provides the action of calculating the switch point.
 * @author Cui Qin
 *
 */
public class CalculateSwitchPointAction implements IAction {
    private ISwitchPoint switchPoint;
    
    /**
     * Creates an action of calculating the switch point.
     * @param switchPoint the switch point
     */
    public CalculateSwitchPointAction(ISwitchPoint switchPoint) {
        this.switchPoint = switchPoint;
    }

    @Override
    public void execute() {
        try {
            SwitchStates.setSwitchPoint(switchPoint.determineSwitchPoint());
        } catch (NullPointerException e) {
            System.err.println("The switch point is null!" + e.getMessage());
        }
    }

}
