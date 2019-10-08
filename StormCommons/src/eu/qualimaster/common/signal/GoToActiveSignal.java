package eu.qualimaster.common.signal;

import java.io.Serializable;

import org.apache.log4j.Logger;

import eu.qualimaster.common.switching.SwitchNodeNameInfo;

/**
 * Provide a signal class for the "goToActive" signal.
 * @author Cui Qin
 *
 */
public class GoToActiveSignal extends AbstractSignal {
    private static final Logger LOGGER = Logger.getLogger(GoToActiveSignal.class);
    private static final String SIGNALNAME = "goToActive";
    
    /**
     * Sends a transfer signal.
     * @param topology the topology to sent to
     * @param nodeName the name of the node to sent to
     * @param value the value to be sent
     * @param con the connection used to send the signal
     */
    public static void sendSignal(String topology, String nodeName, Serializable value, AbstractSignalConnection con) {
        sendSignal(topology, nodeName, SIGNALNAME, value, con);
    }
    
    /**
     * Provide a signal handler for the signal "goToActive" in the target end node.
     * @author Cui Qin
     *
     */
    public static class GoToActiveTrgENDSignalHandler extends AbstractSignalHandler {

        /**
         * Constructor without parameters.
         */
        public GoToActiveTrgENDSignalHandler() {}
        
        /**
         * Constructor for the signal handler.
         * @param signal the switch-related signal
         * @param node the name of the node in which the signal shall be handled
         * @param value the value to be sent in next signals
         */
        public GoToActiveTrgENDSignalHandler(ParameterChangeSignal signal, String node, Serializable value) {
        }
        
        @Override
        public void doSignal() {
            SignalStates.setEmitTrgEND(true);
            SignalStates.setActiveTrgEND(true);
        }

        @Override
        public void nextSignals() {
            //do nothing
        }
        
        static {
            LOGGER.info("Registing the signal handler for the signal:" + SIGNALNAME 
                    + ", for the node type: " + SwitchNodeNameInfo.TARGETENDNODE);
            SignalHandlerRegistry.register(SIGNALNAME + SwitchNodeNameInfo.TARGETENDNODE, 
                    GoToActiveSignal.GoToActiveTrgENDSignalHandler.class);
        }
    }
}
