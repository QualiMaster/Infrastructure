package eu.qualimaster.common.signal;

import java.io.Serializable;

import org.apache.log4j.Logger;

/**
 * Provide a signal handler for the "enable".
 * @author Cui Qin
 *
 */
public class EnableSignal extends AbstractSignal {
    
    private static Logger logger = Logger.getLogger(EnableSignal.class);
    private static final String SIGNALNAME = "enable";
    
    /**
     * Sends a enable signal.
     * @param topology the topology to sent to
     * @param nodeName the name of the node to sent to
     * @param value the value to be sent
     * @param con the connection used to send the signal
     */
    public static void sendSignal(String topology, String nodeName, Serializable value, AbstractSignalConnection con) {
        sendSignal(topology, nodeName, SIGNALNAME, value, con);
    }

}
