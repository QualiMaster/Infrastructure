package switching.logging;

/**
 * Holds identifier for different log types.
 * 
 * @author Nowatzki
 */
public enum LogType {

    SIGNAL_RCV, SIGNAL_SEND, ACK, EMIT, TRANSFER, RCV_VIA_NET, QUEUE, SAFEPOINT, SYN_END, RCV_VIA_NODE, GEN, 
    SWITCH_REQUEST, SWITCH_COMPLETED, SWITCH_DETERMINED

}
