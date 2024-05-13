package eu.qualimaster.common.signal;

import java.io.Serializable;

import org.apache.log4j.Logger;

import eu.qualimaster.common.switching.SwitchNodeNameInfo;
import eu.qualimaster.common.switching.SwitchStrategies;
import eu.qualimaster.common.switching.synchronization.SeparatedOrgINTSynchronizationStrategy;

/**
 * Provide a signal handler for the "transfer" signal.
 * @author Cui Qin
 *
 */
public class TransferSignal extends AbstractSignal {
    private static Logger logger = Logger.getLogger(TransferSignal.class);
    private static final String SIGNALNAME = "transfer";
    
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
     * Provide a signal handler for the signal "transfer" in the original intermediary node.
     * @author Cui Qin
     *
     */
    public static class TransferOrgINTSignalHandler extends AbstractSignalHandler {
        private SeparatedOrgINTSynchronizationStrategy synchronizationStrategy;
        
        /**
         * Constructor without parameters.
         */
        public TransferOrgINTSignalHandler() {}
        
        /**
         * Constructor for the signal handler.
         * @param signal the switch-related signal
         * @param node the name of the node in which the signal shall be handled
         * @param value the value to be sent in next signals
         */
        public TransferOrgINTSignalHandler(ParameterChangeSignal signal, String node, Serializable value) {
            synchronizationStrategy = (SeparatedOrgINTSynchronizationStrategy) SwitchStrategies.getInstance()
                    .getStrategies().get(SeparatedOrgINTSynchronizationStrategy.STRATEGYTYPE);
        }
       
        @Override
        public void doSignal() {
            synchronizationStrategy.doDataTransfer();
        }

        @Override
        public void nextSignals() {
            // do nothing
            
        }
        
        static {
            logger.info("Registing the signal handler for the signal:" + SIGNALNAME 
                    + ", for the node type: " + SwitchNodeNameInfo.ORIGINALINTERMEDIARYNODE);
            SignalHandlerRegistry.register(SIGNALNAME + SwitchNodeNameInfo.ORIGINALINTERMEDIARYNODE, 
                    TransferSignal.TransferOrgINTSignalHandler.class);
        }
    }
    
}
