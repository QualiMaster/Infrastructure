package eu.qualimaster.common.signal;

import java.io.Serializable;

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
     * @param value the value to be sent in next signals
     */
    public void handleSignal(ParameterChangeSignal signal, String node, Serializable value);
    
//    /**
//     * Sets the value for the next signal to be sent.
//     * @param value the value to be sent in next signals
//     */
//    public void setNextSignalValue(Serializable value);
    
    /**
     * Initializes the signal handlers.
     */
    public void init();
}
