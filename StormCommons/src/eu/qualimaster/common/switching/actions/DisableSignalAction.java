package eu.qualimaster.common.switching.actions;

import eu.qualimaster.common.signal.AbstractSignalConnection;
import eu.qualimaster.common.switching.SwitchNodeNameInfo;
/**
 * Actions for disabling data streams via signal.
 * @author Cui Qin
 *
 */
public class DisableSignalAction implements IAction {
    private AbstractSignalConnection signalCon;
    private StreamFlowSignal streamFlow;
    
    /**
     * Constructor.
     * @param signalCon the signal connection used to send signals
     * @param streamFlow the stream flow to be disabled via a signal.
     */
    public DisableSignalAction(AbstractSignalConnection signalCon, StreamFlowSignal streamFlow) {
        this.signalCon = signalCon;
        this.streamFlow = streamFlow;
    }
    
    @Override
    public void execute() {
        switch (streamFlow) {
        case PRE_v3:
            sendDisableSignal(getNameInfoInstance().getOriginalIntermediaryNodeName());
            break;
        case PRE_v7:
            sendDisableSignal(getNameInfoInstance().getOriginalEndNodeName());
            break;
        case PRE_v4:
            sendDisableSignal(getNameInfoInstance().getTargetIntermediaryNodeName());
            break;
        case ORGINT_v1:
            sendDisableSignal(getNameInfoInstance().getPrecedingNodeName());
            break;
        case ORGINT_v2:
            sendDisableSignal(getNameInfoInstance().getPrecedingNodeName());
            break; 
        case ORGINT_v7:
            sendDisableSignal(getNameInfoInstance().getOriginalEndNodeName());
            break;
        default:
            break;
        }
        
    }
    /**
     * Sends a disable signal.
     * @param nodeName the node to be sent to
     */
    private void sendDisableSignal(String nodeName) {
        new SendSignalAction(Signal.DISABLE, nodeName, false, signalCon).execute();
    }
    
    /**
     * Returns the instance of the globally used node names.
     * @return the instance of the globally used node names
     */
    private static SwitchNodeNameInfo getNameInfoInstance() {
        return SwitchNodeNameInfo.getInstance();
    }
}
