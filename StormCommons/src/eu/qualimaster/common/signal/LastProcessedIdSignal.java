package eu.qualimaster.common.signal;

import java.io.Serializable;

import org.apache.log4j.Logger;

import eu.qualimaster.common.switching.SwitchNodeNameInfo;
import eu.qualimaster.common.switching.SwitchStrategies;
import eu.qualimaster.common.switching.synchronization.SeparatedINTSynchronizationStrategy;
import eu.qualimaster.common.switching.warmupDataSynchronizationVariant.WSDSSignalStrategy;

/**
 * Provide a signal handler for the "lastProcessedId".
 * @author Cui Qin
 *
 */
public class LastProcessedIdSignal extends AbstractSignal {
    private static final Logger LOGGER = Logger.getLogger(LastProcessedIdSignal.class);
    private static final String SIGNALNAME = "lastProcessedId";
    
    /**
     * Sends a lastProcessedId signal.
     * @param topology the topology to sent to
     * @param nodeName the name of the node to sent to
     * @param value the value to be sent
     * @param con the connection used to send the signal
     */
    public static void sendSignal(String topology, String nodeName, Serializable value, AbstractSignalConnection con) {
        sendSignal(topology, nodeName, SIGNALNAME, value, con);
    }
    
    /**
     * Provide a signal handler for the signal "lastProcessedId" in the target intermediary node.
     * @author Cui Qin
     *
     */
    public static class LastProcessedIdTrgINTSignalHandler extends AbstractSignalHandler {
        private WSDSSignalStrategy signalStrategy;
        private SeparatedINTSynchronizationStrategy synchronizationStrategy;
        
        /**
         * Constructor without parameters.
         */
        public LastProcessedIdTrgINTSignalHandler() {}
        
        /**
         * Constructor for the signal handler.
         * @param signal the switch-related signal
         * @param node the name of the node in which the signal shall be handled
         * @param value the value to be sent in next signals
         */
        public LastProcessedIdTrgINTSignalHandler(ParameterChangeSignal signal, String node, Serializable value) {
            signalStrategy = (WSDSSignalStrategy) SwitchStrategies.getInstance()
                    .getStrategies().get("signal");
            synchronizationStrategy = (SeparatedINTSynchronizationStrategy) SwitchStrategies.getInstance()
                    .getStrategies().get("synchronization");
        }
        
        @Override
        public void doSignal() {
            synchronizationStrategy.doSynchronization();
        }

        @Override
        public void nextSignals() {
            // do nothing, no signals to be sent next
        }
        
        static {
            LOGGER.info("Registing the signal handler for the signal:" + SIGNALNAME 
                    + ", for the node type: " + SwitchNodeNameInfo.TARGETINTERMEDIARYNODE);
            SignalHandlerRegistry.register(SIGNALNAME + SwitchNodeNameInfo.TARGETINTERMEDIARYNODE, 
                    LastProcessedIdSignal.LastProcessedIdTrgINTSignalHandler.class);
        }
    }
}
