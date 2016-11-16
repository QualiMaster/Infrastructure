package eu.qualimaster.coordination.commands;

import eu.qualimaster.coordination.CoordinationExecutionCode;

/**
 * Represents an execution result.
 * 
 * @author Holger Eichelberger
 */
public class CoordinationExecutionResult {
    
    private CoordinationCommand command;
    private int code;
    private String message;

    /**
     * Creates an execution failed result.
     * 
     * @param command the failing command
     * @param message the failure message
     * @param code the failure code (see {@link eu.qualimaster.coordination.CoordinationExecutionCode})
     */
    public CoordinationExecutionResult(CoordinationCommand command, String message, int code) {
        this.command = command;
        this.code = code;
        this.message = message;
    }
    
    /**
     * The executed command.
     * 
     * @return the command
     */
    public CoordinationCommand getCommand() {
        return command;
    }

    /**
     * A code indicating the reason for failure.
     * 
     * @return the code
     */
    public int getCode() {
        return code;
    }

    /**
     * Returns the failure message.
     * 
     * @return the failure message
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Returns whether iterative execution shall fail if this execution result is seen.
     * Must implement {@link #merge(CoordinationExecutionResult)} accordingly.
     * 
     * @return <code>true</code> continue, <code>false</code> else
     */
    public boolean continueIteration() {
        return code == CoordinationExecutionCode.SIGNAL_SENDING_ERROR;
    }
    
    /**
     * Merges two execution results.
     * 
     * @param result the other result
     * @return the merged result
     */
    public CoordinationExecutionResult merge(CoordinationExecutionResult result) {
        return new CoordinationExecutionResult(result.getCommand(), result.getMessage()
            + ", " + getMessage(), result.getCode());
    }
    
    @Override
    public String toString() {
        return "[" + message + " " + code + " " + command + "]"; 
    }

}