/*
 * Copyright 2009-2016 University of Hildesheim, Software Systems Engineering
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.qualimaster.coordination;

import java.util.List;
import java.util.Stack;

import eu.qualimaster.common.signal.ParameterChange;
import eu.qualimaster.coordination.commands.AlgorithmChangeCommand;
import eu.qualimaster.coordination.commands.CommandSequence;
import eu.qualimaster.coordination.commands.CommandSet;
import eu.qualimaster.coordination.commands.CoordinationCommand;
import eu.qualimaster.coordination.commands.CoordinationExecutionResult;
import eu.qualimaster.coordination.commands.ICoordinationCommandVisitor;
import eu.qualimaster.coordination.commands.ParameterChangeCommand;

/**
 * An abstract coordination command execution visitor processing sets and sequences, in particular for reuse in testing.
 * 
 * @author Holger Eichelberger
 */
public abstract class AbstractCoordinationCommandExecutionVisitor implements ICoordinationCommandVisitor {

    private Stack<CoordinationCommand> commandStack = new Stack<CoordinationCommand>();
    private IExecutionTracer tracer;

    /**
     * Creates a visitor.
     * 
     * @param tracer the tracer (may be <b>null</b>)
     */
    protected AbstractCoordinationCommandExecutionVisitor(IExecutionTracer tracer) {
        this.tracer = tracer;
    }
    
    /**
     * Returns the tracer.
     * 
     * @return the tracer instance (may be <b>null</b>)
     */
    protected IExecutionTracer getTracer() {
        return tracer;
    }
    
    /**
     * Call to indicate starting the processing of a command.
     * 
     * @param command the command
     */
    protected void startingCommand(CoordinationCommand command) {
        commandStack.push(command);
    }
    
    /**
     * Returns whether current commands are processing.
     * 
     * @return <code>true</code> if commands are processing, <code>false</code> else
     */
    protected boolean isProcessingCommands() {
        return commandStack.isEmpty();
    }
    
    /**
     * Call to indicated an ending command.
     * 
     * @param command the command
     */
    protected void endingCommand(CoordinationCommand command) {
        commandStack.pop(); // no param
    }
    
    @Override
    public CoordinationExecutionResult visitCommandSequence(CommandSequence command) {
        CoordinationExecutionResult failed = null;
        startingCommand(command);
        CommandSequenceGroupingVisitor gVisitor = new CommandSequenceGroupingVisitor();
        gVisitor.setExecutor(this);
        for (int c = 0; success(failed) && c < command.getCommandCount(); c++) {
            failed = merge(failed, command.getCommand(c).accept(gVisitor));
        }
        if (success(failed)) {
            failed = merge(failed, gVisitor.flush());
        }
        if (null != tracer) {
            tracer.executedCommandSequence(command, failed);
        }
        return writeCoordinationLog(command, failed);
    }
    
    @Override
    public CoordinationExecutionResult visitCommandSet(CommandSet command) {
        CoordinationExecutionResult failed = null;
        startingCommand(command);
        CommandSetGroupingVisitor gVisitor = new CommandSetGroupingVisitor();
        for (int c = 0; c < command.getCommandCount(); c++) {
            command.getCommand(c).accept(gVisitor);
        }
        gVisitor.setExecutor(this);
        for (int c = 0; success(failed) && c < command.getCommandCount(); c++) {
            failed = merge(failed, command.getCommand(c).accept(gVisitor));
        }
        if (null != tracer) {
            tracer.executedCommandSet(command, failed);
        }
        return writeCoordinationLog(command, failed);
    }

    /**
     * Returns whether the given execution result is considered as a success in iterative execution.
     * 
     * @param failed the execution result
     * @return <code>true</code> for success, <code>false</code> else
     */
    private boolean success(CoordinationExecutionResult failed) {
        return null == failed || failed.continueIteration();
    }
    
    /**
     * Joins two execution results if possible.
     * 
     * @param old the old result
     * @param failed the new result
     * @return <code>old</code> if only old exist or both cannot be joined, <code>failed</code> if only failed is
     *   given or a joined execution result in case of same error codes
     */
    static CoordinationExecutionResult merge(CoordinationExecutionResult old, CoordinationExecutionResult failed) {
        CoordinationExecutionResult result;
        if (null == old) {
            result = failed;
        } else {
            if (null == failed) {
                result = old;
            } else {
                result = failed.merge(old);
            }
        }
        return result;
    }
    
    /**
     * Handles an algorithm change command with optional parameters. While 
     * {@link #handleAlgorithmChangeImpl(AlgorithmChangeCommand, List)} is actually implementing the handling of an 
     * algorithm changed command, this method also takes starting related sub-pipelines into account.
     * 
     * @param command the command
     * @param parameters explicit parameters (may be <b>null</b> than only the cached ones will be used)
     * @return the coordination execution result
     */
    protected abstract CoordinationExecutionResult handleAlgorithmChange(AlgorithmChangeCommand command, 
        List<ParameterChange> parameters);
    
    /**
     * Handles a parameter change command with potential further huckup changes.
     * 
     * @param command the parameter change command
     * @param huckup the huckup / additional changes (ignored if <b>null</b>)
     * @return the execution result
     */
    protected abstract CoordinationExecutionResult handleParameterChange(ParameterChangeCommand<?> command, 
        List<ParameterChange> huckup);

    /**
     * Pops the actual command from the stack and in case of a failure, writes the message to logging
     * and the whole information to the coordination log if on top level. Does not force sending.
     * 
     * @param command the current command being executed
     * @param failing the failing command (may be <b>null</b> if no execution failure)
     * @return <code>failing</code>
     * @see #writeCoordinationLog(CoordinationCommand, CoordinationExecutionResult, boolean)
     */
    protected CoordinationExecutionResult writeCoordinationLog(CoordinationCommand command, 
        CoordinationExecutionResult failing) {
        return writeCoordinationLog(command, failing, false);
    }
    
    /**
     * Pops the actual command from the stack and in case of a failure, writes the message to logging
     * and the whole information to the coordination log if on top level.
     * 
     * @param command the current command being executed
     * @param failing the failing command (may be <b>null</b> if no execution failure)
     * @param forceSend send anyway regardless of {@link #DEFER_SUCCESSFUL_EXECUTION}
     * @return <code>failing</code>
     */
    protected CoordinationExecutionResult writeCoordinationLog(CoordinationCommand command, 
        CoordinationExecutionResult failing, boolean forceSend) {
        endingCommand(command);
        return failing;
    }

}
