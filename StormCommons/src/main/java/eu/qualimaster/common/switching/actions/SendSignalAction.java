package eu.qualimaster.common.switching.actions;

import java.io.Serializable;

import eu.qualimaster.common.signal.AbstractSignalConnection;
import eu.qualimaster.common.signal.ParameterChangeSignal;
import eu.qualimaster.common.signal.SignalException;
import eu.qualimaster.common.switching.SwitchNodeNameInfo;
/**
 * Provides the action of sending a signal.
 * @author Cui Qin
 *
 */
public class SendSignalAction implements IAction {
    private Signal signal;
    private String nodeName;
    private Serializable value;
    private AbstractSignalConnection con;
    
    /**
     * Constructor.
     * @param signal the signal carrying the signal name
     * @param nodeName the name of the node to send to 
     * @param value the value to be sent
     * @param con the signal connection used to send the signal
     */
    public SendSignalAction(Signal signal, String nodeName, Serializable value
            , AbstractSignalConnection con) {
        this.signal = signal;
        this.nodeName = nodeName;
        this.value = value;
        this.con = con;
    }
    
    @Override
    public void execute() {
        sendSignal(SwitchNodeNameInfo.getInstance().getTopologyName(), nodeName, signal.getSignalName(), value, con);
    }
    
    /**
     * Updates the signal carried by the signal.
     * @param value the value carried by the signal
     */
    public void updateValue(Serializable value) {
        this.value = value;
    }
    
    /**
     * Sends a signal.
     * @param topology the topology to sent to
     * @param nodeName the name of the node to sent to
     * @param signalName the signal name
     * @param value the value to be sent
     * @param con the connection used to send the signal
     */
    private static void sendSignal(String topology, String nodeName, String signalName, Serializable value
            , AbstractSignalConnection con) {
        ParameterChangeSignal signal = new ParameterChangeSignal(topology, 
                nodeName, signalName, value, null); 
        try {
            con.sendSignal(signal);
        } catch (SignalException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the signal.
     * @return the signal
     */
    public Signal getSignal() {
        return signal;
    }
    
}
