package eu.qualimaster.common.signal;

import java.io.Serializable;

import org.apache.log4j.Logger;

import eu.qualimaster.common.switching.SwitchNodeNameInfo;
import eu.qualimaster.common.switching.SwitchStrategies;
import eu.qualimaster.common.switching.warmupDataSynchronizationVariant.WSDSSignalStrategy;

/**
 * Provide a signal handler for the "emit" signal.
 * @author Cui Qin
 *
 */
public class EmitSignal extends AbstractSignal {
    private static Logger logger = Logger.getLogger(EmitSignal.class);
    private static final String SIGNALNAME = "emit";
    
    /**
     * Sends a emit signal.
     * @param topology the topology to sent to
     * @param nodeName the name of the node to sent to
     * @param value the value to be sent
     * @param con the connection used to send the signal
     */
    public static void sendSignal(String topology, String nodeName, Serializable value, AbstractSignalConnection con) {
        sendSignal(topology, nodeName, SIGNALNAME, value, con);
    }
    
    /**
     * Provide an emit signal handler for the target end node.
     * @author Cui Qin
     *
     */
    public static class EmitTrgENDSignalHandler extends AbstractSignalHandler {
        private WSDSSignalStrategy signalStrategy;
        
        /**
         * Constructor without parameters.
         */
        public EmitTrgENDSignalHandler() {}
        
        /**
         * Constructor for the signal handler.
         * @param signal the switch-related signal
         * @param node the name of the node in which the signal shall be handled
         * @param value the value to be sent in next signals
         */
        public EmitTrgENDSignalHandler(ParameterChangeSignal signal, String node, Serializable value) {
            signalStrategy = (WSDSSignalStrategy) SwitchStrategies.getInstance()
                    .getStrategies().get("signal");
        }
        
        @Override
        public void doSignal() {
            SignalStates.setEmitTrgEND(true); //enable the target node to emit
        }

        @Override
        public void nextSignals() {
            //do nothing
        }
        
        static {
            logger.info("Registing the signal handler for the signal:" + SIGNALNAME 
                    + ", for the node type: " + SwitchNodeNameInfo.TARGETENDNODE);
            SignalHandlerRegistry.register(SIGNALNAME + SwitchNodeNameInfo.TARGETENDNODE, 
                    EmitSignal.EmitTrgENDSignalHandler.class);
        }
    }
}
