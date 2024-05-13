package eu.qualimaster.coordination.commands;

import eu.qualimaster.common.QMInternal;
import eu.qualimaster.common.QMName;
import eu.qualimaster.common.QMNoSimulation;
import eu.qualimaster.events.AbstractReturnableEvent;
import eu.qualimaster.events.EventManager;

/**
 * Abstract Coordination Layer command.
 * 
 * @author Holger Eichelberger
 */
public abstract class CoordinationCommand extends AbstractReturnableEvent {
    
    private static final long serialVersionUID = 1053944335618155461L;

    /**
     * Send this command to the coordination layer for executing it.
     * This method calls {@link #prepareExecution()} and calls 
     * {@link CoordinationCommandNotifier#notifySent(CoordinationCommand)}.
     * Please note that sending a coordination command directly via 
     * {@link EventManager#handle(eu.qualimaster.events.IEvent)} or 
     * {@link EventManager#send(eu.qualimaster.events.IEvent)} may lead to an execution of the command, 
     * but not its preparation (simplification/compression) and a notification.
     */
    @QMName(name = "exec") // rename virtually for VIL
    @QMNoSimulation() // do not really execute this method during a simulation / test
    public void execute() {
        if (prepareExecution()) {
            EventManager.send(this);
            CoordinationCommandNotifier.notifySent(this);
        }
    }

    /**
     * Called before {@link #execute()}.
     * 
     * @return <code>true</code> (default) if execution shall take place, <code>false</code> else
     */
    protected boolean prepareExecution() {
        return true;
    }
    
    /**
     * Returns a simplified version of this command, e.g., if a container just contains one element. This 
     * method is used to optimize the resources needed for sending and executing this command, while preserving
     * its semantic and allowing lazy construction in the adaptation specification.
     * 
     * @return the simplified version (may be <b>null</b> if this command is not needed)
     */
    @QMInternal
    public CoordinationCommand simplify() {
        return this;
    }
 
    /**
     * Visits this command for actual execution in the execution layer.
     * 
     * @param visitor the visitor which performs the execution
     * @return <b>null</b> if successful, the failed command and a code for failing else
     */
    @QMInternal
    public abstract CoordinationExecutionResult accept(ICoordinationCommandVisitor visitor);

}