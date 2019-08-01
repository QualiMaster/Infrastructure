package eu.qualimaster.common.signal;
/**
 * An interface for switch signals.
 * @author Cui Qin
 *
 */
public interface ISignalHandler {
    /**
     * Reacts on the signal.
     */
    public void doSignal();
    
    /**
     * Sends the next signals.
     */
    public void nextSignals();
}
