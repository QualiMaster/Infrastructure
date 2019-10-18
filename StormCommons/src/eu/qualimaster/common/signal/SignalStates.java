package eu.qualimaster.common.signal;

import eu.qualimaster.base.serializer.KryoSwitchTupleSerializer;

/**
 * Record the signal states.
 * @author Cui Qin
 *
 */

public class SignalStates {
    private static SignalStates signalStatesInstance;
    private static boolean isActiveOrgINT = true;
    private static boolean isActiveTrgINT = false;
    private static boolean isActiveOrgEND = true;
    private static boolean isActiveTrgEND = false;
    private static boolean isPassivateTrgINT = false;
    private static boolean isPassivateOrgINT = false;
    private static boolean isTransferringTrgINT = false;
    private static boolean isTransferringOrgINT = false;
    private static boolean isEmitOrgEND = true;
    private static boolean isEmitTrgEND = false;
    private static boolean isTransferAll = false;
    private static boolean isEmitOrgPRE = true;
    private static boolean isEmitTrgPRE = false;
    private static boolean isDataSynExisted = false;
    // ---to be changed
    private static boolean isBothPRE = false;
    private static boolean isEmitPRE = true;
    // ---to be changed
    private static long firstId = 0;
    private static int numTransferredData = 0;
    private static long lastProcessedId = 0;
    private static long lastEmittedId = 0;
    private static long headId = 0;
    private static long algStartPoint = 0;
    private static int targetPort = 6027;
    private static KryoSwitchTupleSerializer kryoSerOrgINT = null;
    private static int synQueueSizeOrgINT = 10;
    private static int synQueueSizeTrgINT = 50;

    /**
     * Constructor for the class.
     */
    private SignalStates() {
    }

    /**
     * Returns the instance of the singleton class.
     * 
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
        isActiveOrgINT = true;
        isActiveTrgINT = false;
        isActiveOrgEND = true;
        isActiveTrgEND = false;
        isPassivateTrgINT = false;
        isPassivateOrgINT = false;
        isTransferringTrgINT = false;
        isTransferringOrgINT = false;
        isEmitOrgEND = true; // initially the original end node emits
        isEmitTrgEND = false; // initially the target end node is disabled
                                  // to emit
        isDataSynExisted = false;
        isTransferAll = false;
        isEmitOrgPRE = true;
        isEmitTrgPRE = false;
        isBothPRE = false;
        isEmitPRE = true;
        firstId = 0;
        numTransferredData = 0;
        lastProcessedId = 0;
        lastEmittedId = 0;
        headId = 0;
        targetPort = 6027;
    }

    /**
     * Return whether it is passivate in the target intermediary node.
     * 
     * @return <code>true</code> it is passivate; otherwise <code>false</code>
     */
    public static boolean isPassivateTrgINT() {
        return isPassivateTrgINT;
    }

    /**
     * Set the state of whether it is passivate in the target intermediary node.
     * 
     * @param isPassivateTrgINT
     *            <code>true</code> it is passivate; otherwise
     *            <code>false</code>
     */
    public static void setPassivateTrgINT(boolean isPassivateTrgINT) {
        SignalStates.isPassivateTrgINT = isPassivateTrgINT;
    }

    /**
     * Return whether it is passivate in the original intermediary node.
     * 
     * @return <code>true</code> it is passivate; otherwise <code>false</code>
     */
    public static boolean isPassivateOrgINT() {
        return isPassivateOrgINT;
    }

    /**
     * Set the state of whether it is passivate in the original intermediary
     * node.
     * 
     * @param isPassivateOrgINT
     *            <code>true</code> it is passivate; otherwise
     *            <code>false</code>
     */
    public static void setPassivateOrgINT(boolean isPassivateOrgINT) {
        SignalStates.isPassivateOrgINT = isPassivateOrgINT;
    }

    /**
     * Return whether it is transferring, used in the target intermediary node.
     * 
     * @return <code>true</code> it is transferring; otherwise
     *         <code>false</code>
     */
    public static boolean isTransferringTrgINT() {
        return isTransferringTrgINT;
    }

    /**
     * Set the state of transferring in the target intermediary node.
     * 
     * @param isTransferringTrgINT
     *            <code>true</code> it is transferring; otherwise
     *            <code>false</code>
     */
    public static void setTransferringTrgINT(boolean isTransferringTrgINT) {
        SignalStates.isTransferringTrgINT = isTransferringTrgINT;
    }

    /**
     * Return whether it is transferring, used in the original intermediary
     * node.
     * 
     * @return <code>true</code> it is transferring; otherwise
     *         <code>false</code>
     */
    public static boolean isTransferringOrgINT() {
        return isTransferringOrgINT;
    }

    /**
     * Set the state of transferring in the original intermediary node.
     * 
     * @param isTransferringOrgINT
     *            <code>true</code> it is transferring; otherwise
     *            <code>false</code>
     */
    public static void setTransferringOrgINT(boolean isTransferringOrgINT) {
        SignalStates.isTransferringOrgINT = isTransferringOrgINT;
    }

    /**
     * Return the id of the first tuple to be transferred, used in the target
     * intermediary node.
     * 
     * @return the id of the first tuple to be transferred, used the target
     *         intermediary node
     */
    public static long getFirstId() {
        return firstId;
    }

    /**
     * Record the id of the first tuple to be transferred, used in the target
     * intermediary node.
     * 
     * @param firstId
     *            the id of the first tuple to be transferred, used in the
     *            target intermediary node
     */
    public static void setFirstId(long firstId) {
        SignalStates.firstId = firstId;
    }

    /**
     * Return whether it is emitting in the original end node.
     * 
     * @return <code>true</code> it is emitting; otherwise <code>false</code>.
     */
    public static boolean isEmitOrgEND() {
        return isEmitOrgEND;
    }

    /**
     * Sets the state of whether it is emitting in the original end node.
     * 
     * @param isEmittingOrgEND
     *            <code>true</code> it is emitting; otherwise <code>false</code>
     *            .
     */
    public static void setEmitOrgEND(boolean isEmittingOrgEND) {
        SignalStates.isEmitOrgEND = isEmittingOrgEND;
    }

    /**
     * Return whether it is emitting in the target end node.
     * 
     * @return <code>true</code> it is emitting; otherwise <code>false</code>.
     */
    public static boolean isEmitTrgEND() {
        return isEmitTrgEND;
    }

    /**
     * Sets the state of whether it is emitting in the target end node.
     * 
     * @param isEmittingTrgEND
     *            <code>true</code> it is emitting; otherwise <code>false</code>
     *            .
     */
    public static void setEmitTrgEND(boolean isEmittingTrgEND) {
        SignalStates.isEmitTrgEND = isEmittingTrgEND;
    }

    /**
     * Return the number of data items to be transferred.
     * 
     * @return the number of data items to be transferred
     */
    public static int getNumTransferredData() {
        return numTransferredData;
    }

    /**
     * Sets the number of data items to be transferred.
     * 
     * @param numTransferredData
     *            the number of data items to be transferred
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
     * 
     * @return <code>true</code> it is transferring all tuples; otherwise
     *         <code>false</code>
     */
    public static boolean isTransferAll() {
        return isTransferAll;
    }

    /**
     * Set the state of whether it is transferring all tuples.
     * 
     * @param isTransferAll
     *            <code>true</code> it is transferring all tuples; otherwise
     *            <code>false</code>
     */
    public static void setTransferAll(boolean isTransferAll) {
        SignalStates.isTransferAll = isTransferAll;
    }

    /**
     * Return the timestamp when the original algorithm is started to process
     * tuples.
     * 
     * @return the timestamp when the original algorithm is started to process
     *         tuples
     */
    public static long getAlgStartPoint() {
        return algStartPoint;
    }

    /**
     * Set the timestamp when the original algorithm is started to process
     * tuples.
     * 
     * @param algStartPoint
     *            the timestamp when the original algorithm is started to
     *            process tuples
     */
    public static void setAlgStartPoint(long algStartPoint) {
        SignalStates.algStartPoint = algStartPoint;
    }

    /**
     * Return the port of the target node.
     * 
     * @return the port of the target node
     */
    public static int getTargetPort() {
        return targetPort;
    }

    /**
     * Set the port of the target node.
     * 
     * @param targetPort
     *            the port of the target node
     */
    public static void setTargetPort(int targetPort) {
        SignalStates.targetPort = targetPort;
    }

    /**
     * Return the kryo serializer for the original intermediary node.
     * 
     * @return the kryo serializer
     */
    public static KryoSwitchTupleSerializer getKryoSerOrgINT() {
        return kryoSerOrgINT;
    }

    /**
     * Set the kryo serializer for the original intermediary node.
     * 
     * @param kryoSerOrgINT
     *            the kryo serializer
     */
    public static void setKryoSerOrgINT(KryoSwitchTupleSerializer kryoSerOrgINT) {
        SignalStates.kryoSerOrgINT = kryoSerOrgINT;
    }

    /**
     * Return the size of the synchronized queue in the original intermediary
     * node.
     * 
     * @return the size of the synchronized queue
     */
    public static int getSynQueueSizeOrgINT() {
        return synQueueSizeOrgINT;
    }

    /**
     * Set the size of the synchronized queue in the original intermediary node.
     * 
     * @param synQueueSizeOrgINT
     *            the size of the synchronized queue
     */
    public static void setSynQueueSizeOrgINT(int synQueueSizeOrgINT) {
        SignalStates.synQueueSizeOrgINT = synQueueSizeOrgINT;
    }

    /**
     * Return the size of the synchronized queue in the target intermediary
     * node.
     * 
     * @return the size of the synchronized queue
     */
    public static int getSynQueueSizeTrgINT() {
        return synQueueSizeTrgINT;
    }

    /**
     * Set the size of the synchronized queue in the target intermediary node.
     * 
     * @param synQueueSizeTrgINT
     *            the size of the synchronized queue
     */
    public static void setSynQueueSizeTrgINT(int synQueueSizeTrgINT) {
        SignalStates.synQueueSizeTrgINT = synQueueSizeTrgINT;
    }

    /**
     * Return whether the original intermediary node is active.
     * 
     * @return <code>true</code> it is active, otherwise <code>false</code>
     */
    public static boolean isActiveOrgINT() {
        return isActiveOrgINT;
    }

    /**
     * Set the state of whether the original intermediary node is active.
     * 
     * @param isActiveOrgINT
     *            <code>true</code> it is active, otherwise <code>false</code>
     */
    public static void setActiveOrgINT(boolean isActiveOrgINT) {
        SignalStates.isActiveOrgINT = isActiveOrgINT;
    }

    /**
     * Return whether the target intermediary node is active.
     * 
     * @return <code>true</code> it is active, otherwise <code>false</code>
     */
    public static boolean isActiveTrgINT() {
        return isActiveTrgINT;
    }

    /**
     * Set the state of whether the target intermediary node is active.
     * 
     * @param isActiveTrgINT
     *            <code>true</code> it is active, otherwise <code>false</code>
     */
    public static void setActiveTrgINT(boolean isActiveTrgINT) {
        SignalStates.isActiveTrgINT = isActiveTrgINT;
    }

    /**
     * Return whether the data stream to the original algorithm is enabled.
     * 
     * @return <code>true</code> the data stream to the original algorithm is
     *         enabled, otherwise <code>false</code>
     */
    public static boolean isEmitOrgPRE() {
        return isEmitOrgPRE;
    }

    /**
     * Set the state of whether the data stream to the original algorithm is
     * enabled.
     * 
     * @param isOrgEmitPRE
     *            <code>true</code> the data stream to the original algorithm is
     *            enabled, otherwise <code>false</code>
     */
    public static void setEmitOrgPRE(boolean isOrgEmitPRE) {
        SignalStates.isEmitOrgPRE = isOrgEmitPRE;
    }

    /**
     * Return whether the data stream to the target algorithm is enabled.
     * 
     * @return <code>true</code> the data stream to the target algorithm is
     *         enabled, otherwise <code>false</code>
     */
    public static boolean isEmitTrgPRE() {
        return isEmitTrgPRE;
    }

    /**
     * Set the state of whether the data stream to the target algorithm is
     * enabled.
     * 
     * @param isTrgEmitPRE
     *            <code>true</code> the data stream to the target algorithm is
     *            enabled, otherwise <code>false</code>
     */
    public static void setEmitTrgPRE(boolean isTrgEmitPRE) {
        SignalStates.isEmitTrgPRE = isTrgEmitPRE;
    }

    /**
     * Return whether both algorithms emit data in parallel.
     * 
     * @return <code>true</code> both algorithms emit data, otherwise
     *         <code>false</code>
     */
    public static boolean isBothPRE() {
        return isBothPRE;
    }

    /**
     * Set the state of whether both algorithms emit data in parallel.
     * 
     * @param isBothPRE
     *            <code>true</code> both algorithms emit data, otherwise
     *            <code>false</code>
     */
    public static void setBothPRE(boolean isBothPRE) {
        SignalStates.isBothPRE = isBothPRE;
    }

    /**
     * Return whether it shall emit data.
     * 
     * @return <code>true</code> it shall emit data, otherwise
     *         <code>false</code>
     */
    public static boolean isEmitPRE() {
        return isEmitPRE;
    }

    /**
     * Set the state of whether it shall emit data.
     * 
     * @param isEmitPRE
     *            <code>true</code> it shall emit data, otherwise
     *            <code>false</code>
     */
    public static void setEmitPRE(boolean isEmitPRE) {
        SignalStates.isEmitPRE = isEmitPRE;
    }

    /**
     * Return whether the original end node is active.
     * 
     * @return <code>true</code> it is active, otherwise <code>false</code>
     */
    public static boolean isActiveOrgEND() {
        return isActiveOrgEND;
    }

    /**
     * Set the state of whether the original end node is active.
     * 
     * @param isActiveOrgEND
     *            <code>true</code> it is active, otherwise <code>false</code>
     */
    public static void setActiveOrgEND(boolean isActiveOrgEND) {
        SignalStates.isActiveOrgEND = isActiveOrgEND;
    }

    /**
     * Return whether the target end node is active.
     * 
     * @return <code>true</code> it is active, otherwise <code>false</code>
     */
    public static boolean isActiveTrgEND() {
        return isActiveTrgEND;
    }

    /**
     * Set the state of whether the target end node is active.
     * 
     * @param isActiveTrgEND
     *            <code>true</code> it is active, otherwise <code>false</code>
     */
    public static void setActiveTrgEND(boolean isActiveTrgEND) {
        SignalStates.isActiveTrgEND = isActiveTrgEND;
    }

    /**
     * Return whether there is data synchronization existed in the switch variant.
     * @return <code>true</code> it is existed, otherwise <code>false</code>
     */
    public static boolean isDataSynExisted() {
        return isDataSynExisted;
    }

    /**
     * Set the state of whether there is data transfer existed in the switch variant.
     * @param isDataSynExisted <code>true</code> it is existed, otherwise <code>false</code>
     */
    public static void setDataSynExisted(boolean isDataSynExisted) {
        SignalStates.isDataSynExisted = isDataSynExisted;
    }
    
    

}
