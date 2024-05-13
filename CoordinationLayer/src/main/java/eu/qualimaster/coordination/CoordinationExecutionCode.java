package eu.qualimaster.coordination;

/**
 * Execution codes.
 * 
 * @author Holger Eichelberger
 */
public class CoordinationExecutionCode {

    public static final int SUCCESSFUL = 0;
    public static final int SIGNAL_SENDING_ERROR = 1;

    public static final int STARTING_PIPELINE = 3;
    public static final int STOPPING_PIPELINE = 4;
    public static final int CHANGING_PARALLELISM = 5;
    public static final int PROFILING = 6;
    
    public static final int UNKNOWN_COMMAND = -1;
    public static final int NOT_IMPLEMENTED = -2;
    public static final int RESPONSE_TIMEOUT = -3;
    
}
