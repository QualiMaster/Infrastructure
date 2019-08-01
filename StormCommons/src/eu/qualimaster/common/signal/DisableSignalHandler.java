package eu.qualimaster.common.signal;

import org.apache.log4j.Logger;

import eu.qualimaster.common.switching.SwitchNodeNameInfo;
import eu.qualimaster.common.switching.SwitchStrategies;

/**
 * Provide a signal handler for the "disable".
 * @author Cui Qin
 *
 */
public class DisableSignalHandler {
    private static Logger logger = Logger.getLogger(DisableSignalHandler.class);
    private static final String SIGNALNAME = "disable";
    
    /**
     * Provide a signal handler for the signal "enact" in the original end node.
     * @author Cui Qin
     *
     */
    public static class DisableOrgENDSignalHandler extends AbstractSignalHandler {
        private AbstractSignalStrategy signalStrategy;
        
        /**
         * Constructor without parameters.
         */
        public DisableOrgENDSignalHandler() {}
        
        /**
         * Constructor for the signal handler.
         * @param signal the switch-related signal
         * @param node the name of the node in which the signal shall be handled
         */
        public DisableOrgENDSignalHandler(ParameterChangeSignal signal, String node) {
            signalStrategy = (AbstractSignalStrategy) SwitchStrategies.getInstance()
                    .getStrategies().get("signal");
        }
        
        @Override
        public void doSignal() {
            //TODO: change the emit status
            // do nothing for now
            
        }

        @Override
        public void nextSignals() {
            //create a signal to inform the original intermediary node that it is disabled
            ParameterChangeSignal stoppedSignal = new ParameterChangeSignal(getNameInfo().getTopologyName(), 
                    getNameInfo().getOriginalIntermediaryNodeName(), "stopped", true, null); 
            AbstractSignalConnection con = signalStrategy.getSignalConnection();
            try {
                logger.info(System.currentTimeMillis() 
                        + ", Sending the stopped signal to the original intermediar node!");
                con.sendSignal(stoppedSignal);
            } catch (SignalException e) {
                e.printStackTrace();
            }
            
        }
        
        static {
            logger.info("Registing the signal handler for the signal:" + SIGNALNAME);
            SignalHandlerRegistry.register(SIGNALNAME + SwitchNodeNameInfo.ORIGINALENDNODE, 
                    DisableSignalHandler.DisableOrgENDSignalHandler.class);
        }
        
    }

}
