package eu.qualimaster.coordination.events;

import eu.qualimaster.common.QMInternal;
import eu.qualimaster.coordination.CoordinationExecutionCode;
import eu.qualimaster.coordination.commands.CoordinationCommand;
import eu.qualimaster.events.IResponseEvent;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;

/**
 * Informs the adaptation about a failed enactment.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public class CoordinationCommandExecutionEvent extends CoordinationEvent implements IResponseEvent {
    
    private static final long serialVersionUID = 6202055673711726076L;
    private CoordinationCommand command;
    private CoordinationCommand cause;
    private String messageId;
    private String receiverId;
    private int code;
    private String message;
    
    /**
     * Creates a coordination command execution event.
     * 
     * @param command the initial command to be executed
     * @param cause the causing command for that event (may indicate the failure)
     * @param code the execution result (see {@link CoordinationExecutionCode})
     * @param message the message describing the failing
     */
    public CoordinationCommandExecutionEvent(CoordinationCommand command, CoordinationCommand cause, int code, 
        String message) {
        this.cause = cause;
        this.code = code;
        this.message = message;
        this.command = command;
        this.messageId = command.getMessageId();
        this.receiverId = command.getSenderId();
    }

    /**
     * Creates a successful event as a response to a pipeline lifecycle phase.
     * 
     * @param event the lifecycle phase closing event
     */
    public CoordinationCommandExecutionEvent(PipelineLifecycleEvent event) {
        this.cause = null;
        this.code = CoordinationExecutionCode.SUCCESSFUL;
        this.message = null;
        this.messageId = event.getCauseMessageId();
        this.receiverId = event.getCauseSenderId();
    }

    /**
     * Returns the initial command.
     * 
     * @return the initial command (may be <b>null</b>)
     */
    public CoordinationCommand getCommand() {
        return command;
    }

    /**
     * Returns the causing command.
     * 
     * @return the causing command (may be <b>null</b>)
     */
    public CoordinationCommand getCause() {
        return cause;
    }

    /**
     * Returns the code describing the execution result.
     * 
     * @return the execution return code (see {@link CoordinationExecutionCode})
     */
    public int getCode() {
        return code;
    }
    
    /**
     * Returns the execution message.
     * 
     * @return the execution message (may be empty if successful)
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Returns whether the execution was successful.
     * 
     * @return <code>true</code> if successful, <code>false</code> else
     */
    public boolean isSuccessful() {
        return CoordinationExecutionCode.SUCCESSFUL == code;
    }

    @Override
    public String getReceiverId() {
        return receiverId;
    }

    @Override
    public String getMessageId() {
        return messageId;
    }

}
