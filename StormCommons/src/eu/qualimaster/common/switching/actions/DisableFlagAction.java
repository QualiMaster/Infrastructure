package eu.qualimaster.common.switching.actions;

/**
 * The action for disabling the data stream via a flag.
 * @author Cui Qin
 *
 */
public class DisableFlagAction implements IAction {
    private StreamFlowFlag streamFlow;
    
    /**
     * Constructor.
     * @param streamFlow the stream flow to be disabled via a flag 
     */
    public DisableFlagAction(StreamFlowFlag streamFlow) {
        this.streamFlow = streamFlow;
    }
    
    @Override
    public void execute() {
        streamFlow.disableStreamFlow();
    }

}
