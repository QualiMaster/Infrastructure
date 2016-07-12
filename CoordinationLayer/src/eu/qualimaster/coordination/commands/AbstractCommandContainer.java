package eu.qualimaster.coordination.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import eu.qualimaster.common.QMInternal;

/**
 * Just a set of commands.
 * 
 * @author Holger Eichelberger
 */
public abstract class AbstractCommandContainer extends CoordinationCommand {
    
    private static final long serialVersionUID = 8092146436927839331L;
    private List<CoordinationCommand> commands = new ArrayList<CoordinationCommand>();

    /**
     * Creates an empty command sequence.
     */
    public AbstractCommandContainer() {
        this.commands = new ArrayList<CoordinationCommand>();
    }    
    
    /**
     * Creates a command sequence from given commands.
     * 
     * @param commands the commands to be added initially
     */
    @QMInternal
    public AbstractCommandContainer(CoordinationCommand[] commands) {
        this();
        Collections.addAll(this.commands, commands);
    }

    /**
     * Creates a command sequence from given commands.
     * 
     * @param commands the commands to be added initially
     */
    public AbstractCommandContainer(List<CoordinationCommand> commands) {
        this();
        this.commands.addAll(commands);
    }

    /**
     * Returns the number of commands.
     * 
     * @return the number of commands
     */
    public int getCommandCount() {
        return commands.size();
    }

    /**
     * Returns the specified command.
     * 
     * @param index the 0-based index of the desired command
     * @return the specified command
     * @throws IndexOutOfBoundsException if <code>index &lt;0 || index&gt;={@link #getCommandCount()}</code>
     */
    public CoordinationCommand getCommand(int index) {
        return commands.get(index);
    }

    /**
     * Adds a command.
     * 
     * @param command the command to be added
     */
    public void add(CoordinationCommand command) {
        commands.add(command);
    }
    
    /**
     * Returns whether the Coordination Layer must keep the ordering in the container.
     *  
     * @return <code>true</code> if the ordering is important, <code>false</code> if not
     */
    public abstract boolean keepOrdering();

    @Override
    protected boolean prepareExecution() {
        return null != simplify();
    }
    
    @QMInternal
    @Override
    public CoordinationCommand simplify() {
        CoordinationCommand result;

        // clean up
        for (int c = commands.size() - 1; c >= 0; c--) {
            CoordinationCommand cmd = commands.get(c).simplify();
            if (null == cmd) {
                commands.remove(c);
            } else {
                commands.set(c, cmd);
            }
        }

        // simplify
        int cSize = commands.size();
        if (0 == cSize) {
            result = null; // not needed
        } else if (1 == cSize) {
            result = commands.get(0);
        } else {
            result = this;
        }
        return result;
    }

}
