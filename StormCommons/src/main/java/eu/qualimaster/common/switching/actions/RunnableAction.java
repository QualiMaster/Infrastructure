package eu.qualimaster.common.switching.actions;
/**
 * Create a runnable action.
 * @author Cui Qin
 *
 */
public class RunnableAction implements Runnable {
    private IAction action;
    /**
     * Constructor for a runnable action.
     * @param action the action to fulfill the switch activities
     */
    public RunnableAction(IAction action) {
        this.action = action;
    }
    
    @Override
    public void run() {
        action.execute();
    }

}
