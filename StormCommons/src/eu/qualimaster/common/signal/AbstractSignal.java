package eu.qualimaster.common.signal;

import java.io.Serializable;
/**
 * Provide an abstract signal.
 * @author Cui Qin
 *
 */
public abstract class AbstractSignal implements ISignal {
    
    /**
     * Sends a transfer signal.
     * @param topology the topology to sent to
     * @param nodeName the name of the node to sent to
     * @param signalName the signal name
     * @param value the value to be sent
     * @param con the connection used to send the signal
     */
    protected static void sendSignal(String topology, String nodeName, String signalName, Serializable value
            , AbstractSignalConnection con) {
        ParameterChangeSignal signal = new ParameterChangeSignal(topology, 
                nodeName, signalName, value, null); 
        try {
            con.sendSignal(signal);
        } catch (SignalException e) {
            e.printStackTrace();
        }
    }
}
