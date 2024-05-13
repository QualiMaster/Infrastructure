package eu.qualimaster.common.switching.actions;

/**
 * The action for enabling the data stream via a flag.
 * @author Cui Qin
 *
 */
public class EnableFlagAction implements IAction {
    private StreamFlowFlag streamFlow;
    
    /**
     * Constructor.
     * @param streamFlow the stream flow to be enabled via a flag 
     */
    public EnableFlagAction(StreamFlowFlag streamFlow) {
        this.streamFlow = streamFlow;
    }
    
    @Override
    public void execute() {
        streamFlow.enableStreamFlow();
    }

}
