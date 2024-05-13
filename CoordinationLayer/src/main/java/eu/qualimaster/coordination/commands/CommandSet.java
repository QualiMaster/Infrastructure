package eu.qualimaster.coordination.commands;

import java.util.List;

import eu.qualimaster.common.QMInternal;

/**
 * A Coordination Layer command with loose command ordering, i.e., the Coordination Layer may change the ordering.
 * 
 * @author Holger Eichelberger
 */
public class CommandSet extends AbstractCommandContainer {

    private static final long serialVersionUID = -3808776493872399134L;

    /**
     * Creates an empty command sequence.
     */
    public CommandSet() {
        super();
    }    
    
    /**
     * Creates a command sequence from given commands.
     * 
     * @param commands the commands to be added initially
     */
    @QMInternal
    public CommandSet(CoordinationCommand... commands) {
        super(commands);
    }

    /**
     * Creates a command sequence from given commands.
     * 
     * @param commands the commands to be added initially
     */
    public CommandSet(List<CoordinationCommand> commands) {
        super(commands);
    }
    
    @Override
    public boolean keepOrdering() {
        return false;
    }

    @QMInternal
    @Override
    public CoordinationExecutionResult accept(ICoordinationCommandVisitor visitor) {
        return visitor.visitCommandSet(this);
    }

}
