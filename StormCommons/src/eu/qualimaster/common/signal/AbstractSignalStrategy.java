package eu.qualimaster.common.signal;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import eu.qualimaster.common.switching.SwitchNodeNameInfo;

/**
 * An abstract signal strategy.
 * @author Cui Qin
 *
 */
public abstract class AbstractSignalStrategy implements ISignalStrategy {
    private static Logger logger = LogManager.getLogger(AbstractSignalStrategy.class);
    private static final String STRATEGYTYPE = "signal";
    private static Map<String, ISignalHandler> signalHandlers = new HashMap<String, ISignalHandler>();
    private AbstractSignalConnection signalConnection;
    
    /**
     * Constructor of the abstract signal strategy.
     * @param signalConnection the signal connection
     */
    public AbstractSignalStrategy(AbstractSignalConnection signalConnection) {
        this.signalConnection = signalConnection;
    }
    
    @Override
    public void handleSignal(ParameterChangeSignal signal, String node, Serializable value) {
        for (int i = 0; i < signal.getChangeCount(); i++) {
            ParameterChange para = signal.getChange(i);
            try {
                logger.info("Handling the signal: " + para.getName());
                ISignalHandler handler = SignalHandlerRegistry.getSignalHandler(para.getName() + node)
                        .getDeclaredConstructor(ParameterChangeSignal.class, String.class, Serializable.class)
                        .newInstance(signal, node, value);
                handler.doSignal();
                signalHandlers.put(para.getName() + node, handler);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            }
            
        }
    }
    
    @Override
    public String getStrategyType() {
        return STRATEGYTYPE;
    }
    
    /**
     * Synchronize the switch early.
     */
    public void synchronizeEarly() {
        logger.info(System.currentTimeMillis()
                + ", synchronizing the switch early and sending the synchronized signal to the preceding node.");
        SynchronizedSignal.sendSignal(SwitchNodeNameInfo.getInstance().getTopologyName(),
                SwitchNodeNameInfo.getInstance().getPrecedingNodeName(), true,
                signalConnection);
    }
    
    /**
     * Returns the signal connection to support for sending signals.
     * @return the signal connection
     */
    public AbstractSignalConnection getSignalConnection() {
        return signalConnection;
    }

    /**
     * Returns the signal handlers.
     * @return the signal handlers
     */
    public Map<String, ISignalHandler> getSignalHandlers() {
        return signalHandlers;
    }
    
    
}
