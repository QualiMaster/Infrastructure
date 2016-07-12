package eu.qualimaster.coordination.commands;

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

}