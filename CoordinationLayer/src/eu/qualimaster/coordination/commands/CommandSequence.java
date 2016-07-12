package eu.qualimaster.coordination.commands;

import java.util.List;

import eu.qualimaster.common.QMInternal;

/**
 * A Coordination Layer command with strict command ordering, i.e., the Coordination Layer must comply with that.
 * 
 * @author Holger Eichelberger
 */
public class CommandSequence extends AbstractCommandContainer {
    
    private static final long serialVersionUID = 6194722432008238258L;

    /**
     * Creates an empty command sequence.
     */
    public CommandSequence() {
        super();
    }    
    
    /**
     * Creates a command sequence from given commands.
     * 
     * @param commands the commands to be added initially
     */
    @QMInternal
    public CommandSequence(CoordinationCommand... commands) {
        super(commands);
    }

    /**
     * Creates a command sequence from given commands.
     * 
     * @param commands the commands to be added initially
     */
    public CommandSequence(List<CoordinationCommand> commands) {
        super(commands);
    }

    @Override
    public boolean keepOrdering() {
        return true;
    }

    @QMInternal
    @Override
    public CoordinationExecutionResult accept(ICoordinationCommandVisitor visitor) {
        return visitor.visitCommandSequence(this);
    }

}
