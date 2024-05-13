package eu.qualimaster.common.switching.actions;

import eu.qualimaster.common.signal.AbstractSignalConnection;
import eu.qualimaster.common.switching.SwitchNodeNameInfo;
/**
 * Actions for enabling data streams via signal.
 * @author Cui Qin
 *
 */
public class EnableSignalAction implements IAction {
    private AbstractSignalConnection signalCon;
    private StreamFlowSignal streamFlow;

    /**
     * Constructor.
     * @param signalCon the signal connection used to send signals
     * @param streamFlow the stream flow to be enabled via a signal.
     */
    public EnableSignalAction(AbstractSignalConnection signalCon, StreamFlowSignal streamFlow) {
        this.signalCon = signalCon;
        this.streamFlow = streamFlow;
    }
    
    @Override
    public void execute() {
        switch (streamFlow) {
        case PRE_v8:
            sendEnableSignal(getNameInfoInstance().getTargetEndNodeName());
            break;
        case ORGINT_v2:
            sendEnableSignal(getNameInfoInstance().getPrecedingNodeName());
            break;
        case ORGINT_v8:
            sendEnableSignal(getNameInfoInstance().getTargetEndNodeName());
            break;
        case TGTINT_v2:
            sendEnableSignal(getNameInfoInstance().getPrecedingNodeName());
            break;
        case TGTINT_v8:
            sendEnableSignal(getNameInfoInstance().getTargetEndNodeName());
            break;
        default:
            break;
        }
    }
    
    /**
     * Sends a disable signal.
     * @param nodeName the node to be sent to
     */
    private void sendEnableSignal(String nodeName) {
        new SendSignalAction(Signal.ENABLE, nodeName, true, signalCon).execute();
    }
    
    /**
     * Returns the instance of the globally used node names.
     * @return the instance of the globally used node names
     */
    private static SwitchNodeNameInfo getNameInfoInstance() {
        return SwitchNodeNameInfo.getInstance();
    }

}
