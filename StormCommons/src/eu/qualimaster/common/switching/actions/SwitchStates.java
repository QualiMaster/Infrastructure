package eu.qualimaster.common.switching.actions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import eu.qualimaster.base.serializer.KryoSwitchTupleSerializer;
/**
 * Records the states used in the switch.
 * @author Cui Qin
 *
 */
public class SwitchStates {
    /**
     * An enumeration listing all action-related states.
     * @author Cui Qin
     *
     */
    public enum ActionState {
        ALGORITHM_CHANGE,
        SWITCH_POINT_REACHED,
        WARMUP_OVER,
        WAITING_AFTER_WARMUP_OVER,
        DISABLE_SIGNAL_ARRIVED,
        ENACT_SIGNAL_ARRIVED,
        LASTPROCESSEDID_SIGNAL_ARRIVED,
        DISABLE_LASTPROCESSEDID_SIGNAL_ARRIVED,
        STOPPED_SIGNAL_ARRIVED,
        HEADID_SIGNAL_ARRIVED,
        TRANSFER_SIGNAL_ARRIVED,
        TRANSFERRED_SIGNAL_ARRIVED,
        EMIT_SIGNAL_ARRIVED,
        SYNCHRONIZED_SIGNAL_ARRIVED,
        GOTOPASSIVE_SIGNAL_ARRIVED,
        GOTOACTIVE_SIGNAL_ARRIVED,
        WAITING_AFTER_DIABLE_SIGNAL_OVER,
        FIRST_TRANSFERRED_DATA_ARRIVED,
        DATA_TRANSFERRED,
        DATA_SYNCHRONIZED,
        DATA_JUST_ARRIVED,
        OLD_ALG_DRAINED,
        WAITING_AFTER_SWITCH_POINT_OVER,
        OUTPUT_PRODUCED
    };
    
    private static long lastProcessedId = 0;
    private static long lastEmittedId = 0;
    private static long headId = 0;
    private static long firstTupleId = 0;
    private static int numTransferredData = 0;
    private static long algStartPoint = 0;
    private static long switchPoint = 0;
    private static long switchRequestPoint = 0;
    private static long determinationBegin = 0;
    private static boolean isActiveOrgINT = true;
    private static boolean isActiveTrgINT = false;
    private static boolean isActiveOrgEND = true;
    private static boolean isActiveTrgEND = false;
    private static boolean isTransferAll = false;
    private static boolean isPassivateTrgINT = false;
    private static boolean isPassivateOrgINT = false;
    private static boolean isTransferringTrgINT = false;
    private static boolean isTransferringOrgINT = false;
    private static boolean isEmitOrgPRE = true;
    private static boolean isEmitTrgPRE = false;
    private static boolean isEmitOrgEND = true;
    private static boolean isEmitTrgEND = false;
    private static int targetPort = 6027;
    private static int synQueueSizeOrgINT = 10;
    private static int synQueueSizeTrgINT = 50;
    private static KryoSwitchTupleSerializer kryoSerOrgINT = null;
    
    /**
     * Executes actions found in the action map.
     * @param state the action state
     * @param actionMap the action map containing the action list for each action state
     * @param value the value to be updated at runtime if given (only for <code>SendSignalAction</code>)
     */
    public static void executeActions(ActionState state, Map<ActionState, List<IAction>> actionMap, 
            Serializable value) {
        List<IAction> actionList = new ArrayList<IAction>();
        if (actionMap.containsKey(state)) {
            actionList = actionMap.get(state);
        }
        for (int i = 0; i < actionList.size(); i++) {
            if (null != value & (actionList.get(i) instanceof SendSignalAction)) {
                SendSignalAction action = (SendSignalAction) actionList.get(i);
                action.updateValue(value);
            }
            actionList.get(i).execute();
        }
    }
    
    /**
     * Returns the number of data items to be transferred.
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
        SwitchStates.numTransferredData = numTransferredData;
    }

    /**
     * Returns the id of the last processed tuple.
     * 
     * @return the last processed id
     */
    public static long getLastProcessedId() {
        return lastProcessedId;
    }

    /**
     * Sets the id of the last processed tuple.
     * 
     * @param lastProcessedId
     *            the last processed id
     */
    public static void setLastProcessedId(long lastProcessedId) {
        SwitchStates.lastProcessedId = lastProcessedId;
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
        SwitchStates.lastEmittedId = lastEmittedId;
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
        SwitchStates.headId = headId;
    }
    
    /**
     * Return the id of the first tuple to be transferred, used in the target
     * intermediary node.
     * 
     * @return the id of the first tuple to be transferred, used the target
     *         intermediary node
     */
    public static long getFirstTupleId() {
        return firstTupleId;
    }

    /**
     * Record the id of the first tuple to be transferred, used in the target
     * intermediary node.
     * 
     * @param firstTupleId
     *            the id of the first tuple to be transferred, used in the
     *            target intermediary node
     */
    public static void setFirstTupleId(long firstTupleId) {
        SwitchStates.firstTupleId = firstTupleId;
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
        SwitchStates.isActiveOrgINT = isActiveOrgINT;
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
        SwitchStates.isActiveTrgINT = isActiveTrgINT;
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
        SwitchStates.isActiveOrgEND = isActiveOrgEND;
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
        SwitchStates.isActiveTrgEND = isActiveTrgEND;
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
        SwitchStates.isTransferAll = isTransferAll;
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
        SwitchStates.isPassivateTrgINT = isPassivateTrgINT;
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
        SwitchStates.isPassivateOrgINT = isPassivateOrgINT;
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
        SwitchStates.isTransferringTrgINT = isTransferringTrgINT;
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
        SwitchStates.isTransferringOrgINT = isTransferringOrgINT;
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
        SwitchStates.kryoSerOrgINT = kryoSerOrgINT;
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
        SwitchStates.targetPort = targetPort;
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
        SwitchStates.algStartPoint = algStartPoint;
    }

    /**
     * Return the switch point.
     * @return the switch point
     */
    public static long getSwitchPoint() {
        return switchPoint;
    }

    /**
     * Set the switch point.
     * @param switchPoint the switch point
     */
    public static void setSwitchPoint(long switchPoint) {
        SwitchStates.switchPoint = switchPoint;
    }

    /**
     * Returns the timestamp when the switch is requested.
     * @return the timestamp when the switch is requested
     */
    public static long getSwitchRequestPoint() {
        return switchRequestPoint;
    }

    /**
     * Sets the timestamp when the switch is requested.
     * @param switchRequestPoint the timestamp when the switch is requested
     */
    public static void setSwitchRequestPoint(long switchRequestPoint) {
        SwitchStates.switchRequestPoint = switchRequestPoint;
    }
    
    /**
     * Returns the timestamp when the determination starts.
     * @return the timestamp when the determination starts
     */
    public static long getDeterminationBegin() {
        return determinationBegin;
    }

    /**
     * Sets the timestamp when the determination starts.
     * @param determinationBegin the timestamp when the determination starts
     */
    public static void setDeterminationBegin(long determinationBegin) {
        SwitchStates.determinationBegin = determinationBegin;
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
        SwitchStates.synQueueSizeOrgINT = synQueueSizeOrgINT;
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
        SwitchStates.synQueueSizeTrgINT = synQueueSizeTrgINT;
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
        SwitchStates.isEmitOrgPRE = isOrgEmitPRE;
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
        SwitchStates.isEmitTrgPRE = isTrgEmitPRE;
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
        SwitchStates.isEmitOrgEND = isEmittingOrgEND;
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
        SwitchStates.isEmitTrgEND = isEmittingTrgEND;
    }
}
