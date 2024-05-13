package eu.qualimaster.common.signal;

/**
 * A listener for Curator signals.
 * 
 * @author Cui Qin
 */
public interface SignalListener {
    
    /**
     * Is called upon signal reception.
     *  
     * @param data the payload data
     */
    public void onSignal(byte[] data);
}
