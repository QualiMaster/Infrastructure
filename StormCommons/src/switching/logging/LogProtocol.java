package switching.logging;

import java.io.PrintWriter;
import java.util.Calendar;

/**
 * Provide a log protocol to supporting for writing the log.
 * 
 * @author Cui Qin
 *
 */
public class LogProtocol {
    private NodeType nodeType;
    private String nodeName;
    private PrintWriter out;
//    private LogWriter logWriter;
//    private boolean isStarted;

    /**
     * Constructor of the log protocol.
     * 
     * @param nodeType
     *            the node type
     * @param nodeName
     *            the node name
     */
    public LogProtocol(NodeType nodeType, String nodeName) {
        this.nodeType = nodeType;
        this.nodeName = nodeName;
    }
    
    /**
     * Constructor of the log protocol.
     * 
     * @param nodeType
     *            the node type
     * @param nodeName
     *            the node name
     * @param out
     *            the log writer
     */
    public LogProtocol(NodeType nodeType, String nodeName, PrintWriter out) {
        this.nodeType = nodeType;
        this.nodeName = nodeName;
        this.out = out;
    }

//    /**
//     * Constructor of the log protocol.
//     * 
//     * @param nodeType
//     *            the node type
//     * @param nodeName
//     *            the node name
//     * @param logWriter
//     *            the log writer
//     */
//    public LogProtocol(NodeType nodeType, String nodeName, LogWriter logWriter) {
//        this.nodeType = nodeType;
//        this.nodeName = nodeName;
//        this.logWriter = logWriter;
////        this.logWriter.start();
//        isStarted = false;
//    }
    
    /**
     * Creates a log prefix.
     * 
     * @return a log prefix
     */
    public String createLogPrefix() {
        String log = "," + nodeType + "," + nodeName + "," + Calendar.getInstance().getTimeInMillis() + ",";
        return log;
    }

    /**
     * Creates the general logs.
     * 
     * @param logBody
     *            the general log body
     * @return the general log
     */
    public String createGENLog(String logBody) {
        String result = LogType.GEN + createLogPrefix() + logBody;
        writeLog(result);
        return result;
    }
    
    /**
     * Creates the log indicating the switch is requested.
     * @return the switch request log
     */
    public String createSWRequestLog() {
        String result = LogType.SWITCH_REQUEST + createLogPrefix() + "The switch is requested!";
        writeLog(result);
        return result;
    }
    
    /**
     * Creates the log indicating the switch is completed.
     * @return the switch completed log
     */
    public String createSWCompletedLog() {
        String result = LogType.SWITCH_COMPLETED + createLogPrefix() + "The switch is completed!";
        writeLog(result);
        return result;
    }
    
    /**
     * Creates a signal receiving related log.
     * 
     * @param signalType
     *            the signal type
     * @param value
     *            the signal value
     * @return a signal receiving log
     */
    public String createSignalRCVLog(SignalName signalType, Object value) {
        String result;
        String logBody = "Received the parameter signal," + signalType + ",with the value of," + value + ",in the "
                + nodeName + "!";
        result = LogType.SIGNAL_RCV + createLogPrefix() + logBody;
        writeLog(result);
        return result;
    }

    /**
     * Creates a signal sending related log.
     * 
     * @param signalType
     *            the signal type
     * @param value
     *            the signal value
     * @param nodeSendTo
     *            the node sent to
     * @return a signal sending log
     */
    public String createSignalSENDLog(SignalName signalType, Object value, String nodeSendTo) {
        String result;
        String logBody = "Send the parameter signal," + signalType + ",with the value of," + value + ",to " + nodeSendTo
                + "!";
        result = LogType.SIGNAL_SEND + createLogPrefix() + logBody;
        writeLog(result);
        return result;
    }

    /**
     * Creates an acknowledgement log.
     * 
     * @param ackedId
     *            the acknowledged id
     * @return an acknowledgement log.
     */
    public String createACKLog(long ackedId) {
        String result = createTupleIdRelatedLog(LogType.ACK, "Acking", ackedId);
        writeLog(result);
        return result;
    }

    /**
     * Creates a data emitting related log.
     * 
     * @param tupleId
     *            the emitted tuple id
     * @return a data emitting log
     */
    public String createEMITLog(long tupleId) {
        String result = createTupleIdRelatedLog(LogType.EMIT, "Emitting the tuple", tupleId);
        writeLog(result);
        return result;
    }

    /**
     * Creates a data transferring related log.
     * 
     * @param tupleId
     *            the transferred tuple id
     * @param queueType
     *            the queue type
     * @return a data transferring log
     */
    public String createTRANSFERLog(QueueStatus queueType, long tupleId) {
        String result = createTupleIdRelatedLog(LogType.TRANSFER, "Transferring the tuple from " 
                + queueType.name(), tupleId); 
        writeLog(result);
        return result;
    }

    /**
     * Creates a log for data receiving through node connections.
     * 
     * @param tupleId
     *            the received tuple id
     * @return a log for data receiving through node connections
     */
    public String createRcvViaNODELog(long tupleId) {
        String result = createTupleIdRelatedLog(LogType.RCV_VIA_NODE, "Received the tuple", tupleId);
        writeLog(result);
        return result;
    }

    /**
     * Creates a log for data receiving through network.
     * 
     * @param tupleId
     *            the received tuple id
     * @return a log for data receiving through network
     */
    public String createRcvViaNETLog(long tupleId) {
        String result = createTupleIdRelatedLog(LogType.RCV_VIA_NET, "Received the tuple", tupleId);
        writeLog(result);
        return result;
    }

    /**
     * Creates the log for the queue status.
     * 
     * @param queueName
     *            the queue name
     * @param queueSize
     *            the queue size
     * @return the log for the queue status
     */
    public String createQUEUELog(QueueStatus queueName, long queueSize) {
        String result;
        String logBody = "The queue," + queueName + ",and its size," + queueSize;
        result = LogType.QUEUE + createLogPrefix() + logBody;
        writeLog(result);
        return result;
    }

    /**
     * Creates the safe point related log.
     * 
     * @return the safe point related log
     */
    public String createSWDeterminedLog() {
        String result;
        String logBody = "The switch is determined!";
        result = LogType.SWITCH_DETERMINED + createLogPrefix() + logBody;
        writeLog(result);
        return result;
    }

    /**
     * Creates the safe point related log.
     * 
     * @return the safe point related log
     */
    public String createSAFEPOINTLog() {
        String result;
        String logBody = "Handling the safepoint!";
        result = LogType.SAFEPOINT + createLogPrefix() + logBody;
        writeLog(result);
        return result;
    }
    
    /**
     * Creates the log indicating the end of the synchronization phase.
     * 
     * @return the log indicating the end of the synchronization phase
     */
    public String createSynENDLog() {
        String result;
        String logBody = "Reached the last transferred data!";
        result = LogType.SYN_END + createLogPrefix() + logBody;
        writeLog(result);
        return result;
    }

    /**
     * Creates the tuple id related log.
     * 
     * @param logType 
     *            the log type 
     * @param logSpecific
     *            the specific part of the log varying from the log types.
     * @param tupleId
     *            the tuple id
     * @return the tuple id related log
     */
    public String createTupleIdRelatedLog(LogType logType, String logSpecific, long tupleId) {
        String result;
        String logBody = logSpecific + "," + tupleId;
        result = logType + createLogPrefix() + logBody;
        return result;
    }
    
    /**
     * Writes the log.
     * @param log the log string
     */
    private void writeLog(String log) {
        if (null != out) {
            out.println(log);
            out.flush();
        }
//        if (null != logWriter) {
//            logWriter.pushLog(log);
//        }
    }
    
//    /**
//     * Return whether the log writer is started.
//     * @return <code>true</code> if it is started, otherwise <code>false</code>
//     */
//    public boolean isStarted() {
//        return isStarted;
//    }
//    
//    /**
//     * Starts the log writer.
//     */
//    public void start() {
//        logWriter.start();
//        isStarted = true;
//    }
//    
//    
//    /**
//     * Stops the log writer.
//     */
//    public void stop() {
//        logWriter.stop();
//        isStarted = false;
//    }
}
