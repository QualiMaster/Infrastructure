package eu.qualimaster.common.signal;

import java.io.Serializable;

import org.apache.log4j.Logger;

import eu.qualimaster.common.switching.SwitchNodeNameInfo;
import eu.qualimaster.common.switching.SwitchStrategies;
import eu.qualimaster.common.switching.warmupDataSynchronizationVariant.WSDSSignalStrategy;

/**
 * Provide a signal handler for the "stopped" signal.
 * @author Cui Qin
 *
 */
public class StoppedSignal extends AbstractSignal {
    private static Logger logger = Logger.getLogger(StoppedSignal.class);
    private static final String SIGNALNAME = "stopped";
    
    /**
     * Sends a stopped signal.
     * @param topology the topology to sent to
     * @param nodeName the name of the node to sent to
     * @param value the value to be sent
     * @param con the connection used to send the signal
     */
    public static void sendSignal(String topology, String nodeName, Serializable value, AbstractSignalConnection con) {
        sendSignal(topology, nodeName, SIGNALNAME, value, con);
    }
    
    /**
     * Provide a signal handler for the signal "stopped" in the original intermediary node.
     * @author Cui Qin
     *
     */
    public static class StoppedOrgINTSignalHandler extends AbstractSignalHandler {
        private WSDSSignalStrategy signalStrategy;
        private Serializable value;
        
        /**
         * Constructor without parameters.
         */
        public StoppedOrgINTSignalHandler() {}
        
        /**
         * Constructor for the signal handler.
         * @param signal the switch-related signal
         * @param node the name of the node in which the signal shall be handled
         * @param value the value to be sent in next signals
         */
        public StoppedOrgINTSignalHandler(ParameterChangeSignal signal, String node, Serializable value) {
            this.value = value;
            signalStrategy = (WSDSSignalStrategy) SwitchStrategies.getInstance()
                    .getStrategies().get("signal");
        }
        
        @Override
        public void doSignal() {
            // TODO Auto-generated method stub
            nextSignals();
        }

        @Override
        public void nextSignals() {
            logger.info(System.currentTimeMillis() 
                    + ", Sending the lastProcessedId signal to the target intermediary node!");
            LastProcessedIdSignal.sendSignal(getNameInfo().getTopologyName()
                    , getNameInfo().getTargetIntermediaryNodeName(), value, signalStrategy.getSignalConnection());
        }
        
        static {
            logger.info("Registing the signal handler for the signal:" + SIGNALNAME 
                    + ", for the node type: " + SwitchNodeNameInfo.ORIGINALINTERMEDIARYNODE);
            SignalHandlerRegistry.register(SIGNALNAME + SwitchNodeNameInfo.ORIGINALINTERMEDIARYNODE, 
                    StoppedSignal.StoppedOrgINTSignalHandler.class);
        }
        
    }

}
