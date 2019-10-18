package eu.qualimaster.common.switching.actions;

/**
 * The action for disabling the data stream via a flag.
 * @author Cui Qin
 *
 */
public class DisableFlagAction implements IAction {
    private ControlStreamFlag streamFlow;
    
    /**
     * Constructor.
     * @param streamFlow the stream flow to be disabled via a flag 
     */
    public DisableFlagAction(ControlStreamFlag streamFlow) {
        this.streamFlow = streamFlow;
    }
    
    @Override
    public void execute() {
        streamFlow.disableStreamFlow();
    }

}
