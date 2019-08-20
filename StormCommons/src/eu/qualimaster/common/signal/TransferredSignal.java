package eu.qualimaster.common.signal;

import java.io.Serializable;

import org.apache.log4j.Logger;

import eu.qualimaster.common.switching.SwitchNodeNameInfo;
import eu.qualimaster.common.switching.SwitchStrategies;
import eu.qualimaster.common.switching.synchronization.SeparatedTrgINTSynchronizationStrategy;

/**
 * Provide a signal handler for the "transferred" signal.
 * @author Cui Qin
 *
 */
public class TransferredSignal extends AbstractSignal {
    private static final Logger LOGGER = Logger.getLogger(TransferredSignal.class);
    private static final String SIGNALNAME = "transferred";
    
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
     * Provide a signal handler for the signal "transferred" in the target intermediary node.
     * @author Cui Qin
     *
     */
    public static class TransferredTrgINTSignalHandler extends AbstractSignalHandler {
        private SeparatedTrgINTSynchronizationStrategy synchronizationStrategy;
        
        /**
         * Constructor without parameters.
         */
        public TransferredTrgINTSignalHandler() {}
        
        /**
         * Constructor for the signal handler.
         * @param signal the switch-related signal
         * @param node the name of the node in which the signal shall be handled
         * @param value the value to be sent in next signals
         */
        public TransferredTrgINTSignalHandler(ParameterChangeSignal signal, String node, Serializable value) {
            synchronizationStrategy = (SeparatedTrgINTSynchronizationStrategy) SwitchStrategies.getInstance()
                    .getStrategies().get(SeparatedTrgINTSynchronizationStrategy.STRATEGYTYPE);
        }
        
        @Override
        public void doSignal() {
            synchronizationStrategy.completingSynchronization();
        }

        @Override
        public void nextSignals() {
            //do nothing
        }
        
        static {
            LOGGER.info("Registing the signal handler for the signal:" + SIGNALNAME 
                    + ", for the node type: " + SwitchNodeNameInfo.TARGETINTERMEDIARYNODE);
            SignalHandlerRegistry.register(SIGNALNAME + SwitchNodeNameInfo.TARGETINTERMEDIARYNODE, 
                    TransferredSignal.TransferredTrgINTSignalHandler.class);
        }
    }
}
