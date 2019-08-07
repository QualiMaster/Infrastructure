package eu.qualimaster.common.signal;

/**
 * Record the signal states.
 * @author Cui Qin
 *
 */
public class SignalStates {
    private static SignalStates signalStatesInstance;
    private static boolean isPassivateTrgINT = false;
    private static boolean isPassivateOrgINT = false;
    private static boolean isTransferringTrgINT = false;
    private static boolean isTransferringOrgINT = false;
    private static boolean isEmittingOrgEND = true;
    private static boolean isEmittingTrgEND = false;
    private static boolean isTransferAll = false;
    private static long firstId = 0;
    private static int numTransferredData = 0;
    private static long lastProcessedId = 0;
    private static long lastEmittedId = 0;
    private static long headId = 0;
    
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
        isTransferringTrgINT = false;
        isEmittingOrgEND = true; //initially the original end node emits
        isEmittingTrgEND = false; //initially the target end node is disabled to emit
        isTransferAll = false;
        firstId = 0;
        numTransferredData = 0; 
        lastProcessedId = 0;
        lastEmittedId = 0;
        headId = 0;
    }

    /**
     * Return whether it is passivate in the target intermediary node.
     * @return <code>true</code> it is passivate; otherwise <code>false</code>
     */
    public static boolean isPassivateTrgINT() {
        return isPassivateTrgINT;
    }

    /**
     * Set the state of whether it is passivate in the target intermediary node.
     * @param isPassivateTrgINT <code>true</code> it is passivate; otherwise <code>false</code>
     */
    public static void setPassivateTrgINT(boolean isPassivateTrgINT) {
        SignalStates.isPassivateTrgINT = isPassivateTrgINT;
    }

    /**
     * Return whether it is passivate in the original intermediary node.
     * @return <code>true</code> it is passivate; otherwise <code>false</code>
     */
    public static boolean isPassivateOrgINT() {
        return isPassivateOrgINT;
    }

    /**
     * Set the state of whether it is passivate in the original intermediary node.
     * @param isPassivateOrgINT <code>true</code> it is passivate; otherwise <code>false</code>
     */
    public static void setPassivateOrgINT(boolean isPassivateOrgINT) {
        SignalStates.isPassivateOrgINT = isPassivateOrgINT;
    }

    /**
     * Return whether it is transferring, used in the target intermediary node.
     * @return <code>true</code> it is transferring; otherwise <code>false</code>
     */
    public static boolean isTransferringTrgINT() {
        return isTransferringTrgINT;
    }
    
    /**
     * Set the state of transferring in the target intermediary node.
     * @param isTransferringTrgINT <code>true</code> it is transferring; otherwise <code>false</code>
     */
    public static void setTransferringTrgINT(boolean isTransferringTrgINT) {
        SignalStates.isTransferringTrgINT = isTransferringTrgINT;
    }
    
    /**
     * Return whether it is transferring, used in the original intermediary node.
     * @return <code>true</code> it is transferring; otherwise <code>false</code>
     */
    public static boolean isTransferringOrgINT() {
        return isTransferringOrgINT;
    }
    
    /**
     * Set the state of transferring in the original intermediary node.
     * @param isTransferringOrgINT <code>true</code> it is transferring; otherwise <code>false</code>
     */
    public static void setTransferringOrgINT(boolean isTransferringOrgINT) {
        SignalStates.isTransferringOrgINT = isTransferringOrgINT;
    }
    
    /**
     * Return the id of the first tuple to be transferred, used in the target intermediary node.
     * @return the id of the first tuple to be transferred, used the target intermediary node
     */
    public static long getFirstId() {
        return firstId;
    }
    
    /**
     * Record the id of the first tuple to be transferred, used in the target intermediary node.
     * @param firstId the id of the first tuple to be transferred, used in the target intermediary node
     */
    public static void setFirstId(long firstId) {
        SignalStates.firstId = firstId;
    }
    
    /**
     * Return whether it is emitting in the original end node.
     * @return <code>true</code> it is emitting; otherwise <code>false</code>.
     */
    public static boolean isEmittingOrgEND() {
        return isEmittingOrgEND;
    }

    /**
     * Sets the state of whether it is emitting in the original end node.
     * @param isEmittingOrgEND <code>true</code> it is emitting; otherwise <code>false</code>.
     */
    public static void setEmittingOrgEND(boolean isEmittingOrgEND) {
        SignalStates.isEmittingOrgEND = isEmittingOrgEND;
    }
    
    /**
     * Return whether it is emitting in the target end node.
     * @return <code>true</code> it is emitting; otherwise <code>false</code>.
     */
    public static boolean isEmittingTrgEND() {
        return isEmittingTrgEND;
    }

    
    /**
     * Sets the state of whether it is emitting in the target end node.
     * @param isEmittingTrgEND <code>true</code> it is emitting; otherwise <code>false</code>.
     */
    public static void setEmittingTrgEND(boolean isEmittingTrgEND) {
        SignalStates.isEmittingTrgEND = isEmittingTrgEND;
    }
    
    /**
     * Return the number of data items to be transferred. 
     * @return the number of data items to be transferred
     */
    public static int getNumTransferredData() {
        return numTransferredData;
    }

    /**
     * Sets the number of data items to be transferred. 
     * @param numTransferredData the number of data items to be transferred
     */
    public static void setNumTransferredData(int numTransferredData) {
        SignalStates.numTransferredData = numTransferredData;
    }
    
    /**
     * Return the id of the last processed tuple.
     * 
     * @return the last processed id
     */
    public static long getLastProcessedId() {
        return lastProcessedId;
    }

    /**
     * Set the id of the last processed tuple.
     * 
     * @param lastProcessedId
     *            the last processed id
     */
    public static void setLastProcessedId(long lastProcessedId) {
        SignalStates.lastProcessedId = lastProcessedId;
    }

    /**
     * Return the id of the last emitted tuple.
     * 
     * @return the id of the last emitted tuple
     */
    public static long getLastEmittedId() {
        return lastEmittedId;
    }

    /**
     * Set the id of the last emitted tuple.
     * 
     * @param lastEmittedId
     *            the id of the last emitted tuple
     */
    public static void setLastEmittedId(long lastEmittedId) {
        SignalStates.lastEmittedId = lastEmittedId;
    }

    /**
     * Return the id of the first tuple to be transferred.
     * 
     * @return the id of the first tuple to be transferred
     */
    public static long getHeadId() {
        return headId;
    }

    /**
     * Set the id of the first tuple to be transferred.
     * 
     * @param headId
     *            the id of the first tuple to be transferred
     */
    public static void setHeadId(long headId) {
        SignalStates.headId = headId;
    }
    
    /**
     * Return whether it is transferring all tuples.
     * @return <code>true</code> it is transferring all tuples; otherwise <code>false</code>
     */
    public static boolean isTransferAll() {
        return isTransferAll;
    }
    
    /**
     * Set the state of whether it is transferring all tuples.
     * @param isTransferAll <code>true</code> it is transferring all tuples; otherwise <code>false</code>
     */
    public static void setTransferAll(boolean isTransferAll) {
        SignalStates.isTransferAll = isTransferAll;
    }
    
    
    
}
