package eu.qualimaster.common.signal;

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
public class EnactSignalHandler {
    private static Logger logger = LogManager.getLogger(EnactSignalHandler.class);
    private static final String SIGNALNAME = "enact";
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
         */
        public EnactOrgINTSignalHandler(ParameterChangeSignal signal, String node) {
//            super(signal, node);
            switchArrivalPoint = getSwitchArrivalPoint(signal);
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
            //create a signal to disable the end node
            ParameterChangeSignal disableSignal = new ParameterChangeSignal(getNameInfo().getTopologyName(), 
                    getNameInfo().getOriginalEndNodeName(), "disable", true, null); 
            logger.info("The original intermediary node name: " + getNameInfo().getOriginalIntermediaryNodeName());
            logger.info("Created the disable signal sending to " + getNameInfo().getOriginalEndNodeName());
            //create a signal to passivate the preceding node 
            ParameterChangeSignal passivateSignal = new ParameterChangeSignal(getNameInfo().getTopologyName(), 
                    getNameInfo().getPrecedingNodeName(), "passivate", true, null);
            logger.info("Created the passivate signal sending to " + getNameInfo().getPrecedingNodeName());
            signalStrategy = (WSDSSignalStrategy) SwitchStrategies.getInstance()
                    .getStrategies().get("signal");
            AbstractSignalConnection con = signalStrategy.getSignalConnection();
            try {
                //send the defined signals
                logger.info("Sending the defined signals with the connection: " + con);
                con.sendSignal(disableSignal);
                con.sendSignal(passivateSignal);
            } catch (SignalException e) {
                e.printStackTrace();
            }
        }
        
        static {
            logger.info("Registing the signal handler for the signal:" + SIGNALNAME);
            SignalHandlerRegistry.register(SIGNALNAME + SwitchNodeNameInfo.ORIGINALINTERMEDIARYNODE, 
                    EnactSignalHandler.EnactOrgINTSignalHandler.class);
        }
    }
}
