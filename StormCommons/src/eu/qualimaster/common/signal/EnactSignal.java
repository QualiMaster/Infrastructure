package eu.qualimaster.common.signal;

import java.io.Serializable;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import eu.qualimaster.common.switching.SwitchNodeNameInfo;
import eu.qualimaster.common.switching.SwitchStrategies;
import eu.qualimaster.common.switching.warmupDataSynchronizationVariant.WSDSDeterminationStrategy;
import eu.qualimaster.common.switching.warmupDataSynchronizationVariant.WSDSSignalStrategy;

/**
 * Provide a signal handler for the signal "enact".
 * @author Cui Qin
 *
 */
public class EnactSignal extends AbstractSignal {
    private static Logger logger = LogManager.getLogger(EnactSignal.class);
    private static final String SIGNALNAME = "enact";
    
    /**
     * Sends a disable signal.
     * @param topology the topology to sent to
     * @param nodeName the name of the node to sent to
     * @param value the value to be sent
     * @param con the connection used to send the signal
     */
    public static void sendSignal(String topology, String nodeName, Serializable value, AbstractSignalConnection con) {
        sendSignal(topology, nodeName, SIGNALNAME, value, con);
    }
    
    /**
     * Provide a signal handler for the signal "enact" in the original intermediary node.
     * The value of this signal is the switch arrival point.
     * @author Cui Qin
     *
     */
    public static class EnactOrgINTSignalHandler extends AbstractSignalHandler {
        private WSDSDeterminationStrategy determinationStrategy;
        private WSDSSignalStrategy signalStrategy;
        private long switchArrivalPoint;
        
        /**
         * Constructor without parameters.
         */
        public EnactOrgINTSignalHandler() {}
        
        /**
         * Constructor for the signal handler.
         * @param signal the switch-related signal
         * @param node the name of the node in which the signal shall be handled
         * @param value the value to be sent in next signals
         */
        public EnactOrgINTSignalHandler(ParameterChangeSignal signal, String node, Serializable value) {
//            super(signal, node);
            switchArrivalPoint = getSwitchArrivalPoint(signal);
            signalStrategy = (WSDSSignalStrategy) SwitchStrategies.getInstance()
                    .getStrategies().get("signal");
        }
        
        /**
         * Returns the switch arrival point extracted from the received signal.
         * @param signal the received signal
         * @return the switch arrival point
         */
        private long getSwitchArrivalPoint(ParameterChangeSignal signal) {
            long result = 0L;
            for (int i = 0; i < signal.getChangeCount(); i++) {
                ParameterChange para = signal.getChange(i);
                result = Long.valueOf(para.getStringValue());
            } 
            return result;
        }
        
        @Override
        public void doSignal() {
            logger.info(System.currentTimeMillis() + ", Handling the signal : " + SIGNALNAME);
            //determine the safepoint as the switch point
            determinationStrategy = (WSDSDeterminationStrategy) SwitchStrategies.getInstance()
                    .getStrategies().get("determination");
            determinationStrategy.setSwitchArrivalPoint(switchArrivalPoint);
            determinationStrategy.determineSwitchPoint();
            logger.info("The switch point is at " + determinationStrategy.getSwitchPoint());
        }
        
        @Override
        public void nextSignals() {
            SignalStates.setPassivateOrgINT(true); //passivate the org INT node
            logger.info(System.currentTimeMillis() 
                    + ", Sending the diable signal to the original end node!");
            DisableSignal.sendSignal(getNameInfo().getTopologyName()
                    , getNameInfo().getOriginalEndNodeName(), true, signalStrategy.getSignalConnection());
            
            logger.info(System.currentTimeMillis() 
                    + ", Sending the diable signal to the preceding node!");
            DisableSignal.sendSignal(getNameInfo().getTopologyName()
                    , getNameInfo().getPrecedingNodeName(), true, signalStrategy.getSignalConnection());
        }
        
        static {
            logger.info("Registing the signal handler for the signal:" + SIGNALNAME
                    + ", for the node type: " + SwitchNodeNameInfo.ORIGINALINTERMEDIARYNODE);
            SignalHandlerRegistry.register(SIGNALNAME + SwitchNodeNameInfo.ORIGINALINTERMEDIARYNODE, 
                    EnactSignal.EnactOrgINTSignalHandler.class);
        }
    }
}
