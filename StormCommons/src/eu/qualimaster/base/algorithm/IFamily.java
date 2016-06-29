package eu.qualimaster.base.algorithm;

import eu.qualimaster.observables.IMeasurable;

/**
 * Common base interface for all families (and the realizing algorithms).
 */
public interface IFamily extends IMeasurable {
    
    /**
     * The desired state.
     * 
     * @author Holger Eichelberger
     */
    public enum State {
        
        /**
         * Activate the algorithm, i.e., process data.
         */
        ACTIVATE,
        
        /**
         * Passivate the algorithm, i.e., stop processing data. Another algorithm 
         * will take over.
         */
        PASSIVATE,
        
        /**
         * The owning pipeline is about to terminate. The algorithm shall close its referenced
         * resources and prepare for a potential shutdown of the executing virtual machine. Please
         * note the difference to {@link #PASSIVATE}.
         */
        TERMINATING
    }
    
    /**
     * Switch this algorithm into the given state.
     * The implementation shall be non-blocking!
     * 
     * @param state the target state
     */
    public void switchState(State state);
    
}
