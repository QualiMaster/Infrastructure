package eu.qualimaster.common.signal;

/**
 * Record the signal states.
 * @author Cui Qin
 *
 */
public class SignalStates {
    private static SignalStates signalStatesInstance;
    private static boolean isTransferring;
    private static boolean isEmittingOrgEND;
    private static boolean isEmittingTrgEND;
    private static long firstId;
    private static int numTransferredDataTrgINT;
    
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
     * Initialize the initial values of the signal states.
     */
    public static void init() {
        isTransferring = false;
        isEmittingOrgEND = true; //initially the original end node emits
        isEmittingTrgEND = false; //initially the target end node is disabled to emit
        firstId = 0;
        numTransferredDataTrgINT = 0; 
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
    
    /**
     * Return the id of the first tuple to be transferred from the original intermediary node.
     * @return the id of the first tuple to be transferred from the original intermediary node
     */
    public static long getFirstId() {
        return firstId;
    }
    
    /**
     * Record the id of the first tuple to be transferred from the original intermediary node.
     * @param firstId the id of the first tuple to be transferred from the original intermediary node
     */
    public static void setFirstId(long firstId) {
        SignalStates.firstId = firstId;
    }
    
    /**
     * Return whether it is emitting in the original end node.
     * @return <code>true</code> it is emitting; otherwise false.
     */
    public static boolean isEmittingOrgEND() {
        return isEmittingOrgEND;
    }

    /**
     * Sets the state of whether it is emitting in the original end node.
     * @param isEmittingOrgEND <code>true</code> it is emitting; otherwise false.
     */
    public static void setEmittingOrgEND(boolean isEmittingOrgEND) {
        SignalStates.isEmittingOrgEND = isEmittingOrgEND;
    }
    
    /**
     * Return whether it is emitting in the target end node.
     * @return <code>true</code> it is emitting; otherwise false.
     */
    public static boolean isEmittingTrgEND() {
        return isEmittingTrgEND;
    }

    
    /**
     * Sets the state of whether it is emitting in the target end node.
     * @param isEmittingTrgEND <code>true</code> it is emitting; otherwise false.
     */
    public static void setEmittingTrgEND(boolean isEmittingTrgEND) {
        SignalStates.isEmittingTrgEND = isEmittingTrgEND;
    }
    
    /**
     * Return the number of data items to be transferred, used in the target intermediary node. 
     * @return the number of data items to be transferred
     */
    public static int getNumTransferredDataTrgINT() {
        return numTransferredDataTrgINT;
    }

    /**
     * Sets the number of data items to be transferred, used in the target intermediary node. 
     * @param numTransferredDataTrgINT the number of data items to be transferred
     */
    public static void setNumTransferredDataTrgINT(int numTransferredDataTrgINT) {
        SignalStates.numTransferredDataTrgINT = numTransferredDataTrgINT;
    }
    
    
}
