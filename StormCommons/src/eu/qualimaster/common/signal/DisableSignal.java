package eu.qualimaster.common.signal;

import java.io.Serializable;

import org.apache.log4j.Logger;

import eu.qualimaster.common.switching.SwitchNodeNameInfo;
import eu.qualimaster.common.switching.SwitchStrategies;
import eu.qualimaster.common.switching.synchronization.SeparatedTrgINTSynchronizationStrategy;
import eu.qualimaster.common.switching.warmupDataSynchronizationVariant.WSDSSignalStrategy;

/**
 * Provide a signal handler for the "disable".
 * @author Cui Qin
 *
 */
public class DisableSignal extends AbstractSignal {
    private static Logger logger = Logger.getLogger(DisableSignal.class);
    private static final String SIGNALNAME = "disable";
    
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
     * Provide a signal handler for the signal "disable" in the original end node.
     * @author Cui Qin
     *
     */
    public static class DisableOrgENDSignalHandler extends AbstractSignalHandler {
        private WSDSSignalStrategy signalStrategy;
        
        /**
         * Constructor without parameters.
         */
        public DisableOrgENDSignalHandler() {}
        
        /**
         * Constructor for the signal handler.
         * @param signal the switch-related signal
         * @param node the name of the node in which the signal shall be handled
         * @param value the value to be sent in next signals
         */
        public DisableOrgENDSignalHandler(ParameterChangeSignal signal, String node, Serializable value) {
            signalStrategy = (WSDSSignalStrategy) SwitchStrategies.getInstance()
                    .getStrategies().get("signal");
        }
        
        @Override
        public void doSignal() {
            SignalStates.setEmittingOrgEND(false); //stop emitting data
            nextSignals();
        }

        @Override
        public void nextSignals() {
            //create a signal to inform the original intermediary node that it is disabled
            logger.info(System.currentTimeMillis() 
                    + ", Sending the stopped signal to the original intermediary node!");
            StoppedSignal.sendSignal(getNameInfo().getTopologyName()
                    , getNameInfo().getOriginalIntermediaryNodeName(), true, signalStrategy.getSignalConnection());
        }
        
        static {
            logger.info("Registing the signal handler for the signal:" + SIGNALNAME 
                    + ", for the node type: " + SwitchNodeNameInfo.ORIGINALENDNODE);
            SignalHandlerRegistry.register(SIGNALNAME + SwitchNodeNameInfo.ORIGINALENDNODE, 
                    DisableSignal.DisableOrgENDSignalHandler.class);
        }
    }
    
    /**
     * Provide a signal handler for the signal "disable" in the preceding node.
     * @author Cui Qin
     *
     */
    public static class DisablePRESignalHandler extends AbstractSignalHandler {
        private WSDSSignalStrategy signalStrategy;
        private Serializable value;
        
        /**
         * Constructor without parameters.
         */
        public DisablePRESignalHandler() {}
        
        /**
         * Constructor for the signal handler.
         * @param signal the switch-related signal
         * @param node the name of the node in which the signal shall be handled
         * @param value the value to be sent in next signals
         */
        public DisablePRESignalHandler(ParameterChangeSignal signal, String node, Serializable value) {
            this.value = value;
            signalStrategy = (WSDSSignalStrategy) SwitchStrategies.getInstance()
                    .getStrategies().get("signal");
        }
        
        @Override
        public void doSignal() {
            // change the both and emit state to false
            logger.info("Changing the states of the emit and both.");
            SignalStates.setBothPRE(false);
            SignalStates.setEmitPRE(false);
            logger.info("Sending next signals...");
            nextSignals();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void nextSignals() {
            logger.info(System.currentTimeMillis() 
                    + ", Sending the disable signal to the target intermediary node!");
            sendSignal(getNameInfo().getTopologyName(), getNameInfo().getTargetIntermediaryNodeName(), this.value
                    , signalStrategy.getSignalConnection());
        }
        
        static {
            logger.info("Registing the signal handler for the signal:" + SIGNALNAME 
                    + ", for the node type: " + SwitchNodeNameInfo.PRECEDINGNODE);
            SignalHandlerRegistry.register(SIGNALNAME + SwitchNodeNameInfo.PRECEDINGNODE, 
                    DisableSignal.DisablePRESignalHandler.class);
        }
    }
    
    /**
     * Provide a signal handler for the signal "disable" in the target intermediary node.
     * @author Cui Qin
     *
     */
    public static class DisableTrgINTSignalHandler extends AbstractSignalHandler {
        private SeparatedTrgINTSynchronizationStrategy synchronizationStrategy;
        
        /**
         * Constructor without parameters.
         */
        public DisableTrgINTSignalHandler() {}
        
        /**
         * Constructor for the signal handler.
         * @param signal the switch-related signal
         * @param node the name of the node in which the signal shall be handled
         * @param value the value to be sent in next signals
         */
        public DisableTrgINTSignalHandler(ParameterChangeSignal signal, String node, Serializable value) {
            synchronizationStrategy = (SeparatedTrgINTSynchronizationStrategy) SwitchStrategies.getInstance()
                    .getStrategies().get(SeparatedTrgINTSynchronizationStrategy.STRATEGYTYPE);
        }
        
        @Override
        public void doSignal() {
            synchronizationStrategy.doSynchronization();
        }

        @Override
        public void nextSignals() {
            // do nothing
        }
        
        static {
            logger.info("Registing the signal handler for the signal:" + SIGNALNAME 
                    + ", for the node type: " + SwitchNodeNameInfo.TARGETINTERMEDIARYNODE);
            SignalHandlerRegistry.register(SIGNALNAME + SwitchNodeNameInfo.TARGETINTERMEDIARYNODE, 
                    DisableSignal.DisableTrgINTSignalHandler.class);
        }
    }
}
