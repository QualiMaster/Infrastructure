package eu.qualimaster.common.signal;

import eu.qualimaster.common.switching.IStrategy;

/**
 * Defines the interface of the signal strategy.
 * @author Cui Qin
 *
 */
public interface ISignalStrategy extends IStrategy {
    /**
     * Handles the signal.
     * @param signal the signal
     * @param node the node that receives the signal
     */
    public void handleSignal(ParameterChangeSignal signal, String node);
    
    /**
     * Initializes the signal handlers.
     */
    public void init();
}
