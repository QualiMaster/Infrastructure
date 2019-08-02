package eu.qualimaster.common.signal;

/**
 * Record the signal states.
 * @author Cui Qin
 *
 */
public class SignalStates {
    private static boolean isTransferring;
    private static SignalStates signalStatesInstance;
    
    /**
     * Constructor for the class.
     */
    private SignalStates() {}
    
    /**
     * Returns the instance of the singleton class.
     * @return the instance of the singleton class
     */
    public static SignalStates getInstance() {
        if (null == signalStatesInstance) {
            signalStatesInstance = new SignalStates();
        } 
        return signalStatesInstance;
    }
    
    /**
     * Initialize the signal states.
     */
    public static void init() {
        isTransferring = false;
    }

    /**
     * Return whether it is transferring.
     * @return <code>true</code> it is transferring; otherwise false.
     */
    public static boolean isTransferring() {
        return isTransferring;
    }
    
    /**
     * Set the state of transferring.
     * @param isTransferring <code>true</code> it is transferring; otherwise false.
     */
    public static void setTransferring(boolean isTransferring) {
        SignalStates.isTransferring = isTransferring;
    }
    
    
}
