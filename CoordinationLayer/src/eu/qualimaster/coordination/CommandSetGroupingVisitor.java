/*
 * Copyright 2009-2015 University of Hildesheim, Software Systems Engineering
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.qualimaster.common.signal.ParameterChange;
import eu.qualimaster.coordination.commands.AbstractPipelineElementCommand;
import eu.qualimaster.coordination.commands.AlgorithmChangeCommand;
import eu.qualimaster.coordination.commands.CommandSequence;
import eu.qualimaster.coordination.commands.CommandSet;
import eu.qualimaster.coordination.commands.CoordinationCommand;
import eu.qualimaster.coordination.commands.CoordinationExecutionResult;
import eu.qualimaster.coordination.commands.ICoordinationCommandVisitor;
import eu.qualimaster.coordination.commands.LoadSheddingCommand;
import eu.qualimaster.coordination.commands.MonitoringChangeCommand;
import eu.qualimaster.coordination.commands.ParallelismChangeCommand;
import eu.qualimaster.coordination.commands.ParameterChangeCommand;
import eu.qualimaster.coordination.commands.PipelineCommand;
import eu.qualimaster.coordination.commands.ProfileAlgorithmCommand;
import eu.qualimaster.coordination.commands.ReplayCommand;
import eu.qualimaster.coordination.commands.ScheduleWavefrontAdaptationCommand;
import eu.qualimaster.coordination.commands.ShutdownCommand;
import eu.qualimaster.coordination.commands.UpdateCommand;

/**
 * Tries to group / execute the commands stored in a command set (reorderable).
 * Unless {@link #setExecutor(ICoordinationCommandVisitor)} is set, this visitor works
 * in a grouping phase, other in execution phase. Two iterations are needed.
 * 
 * @author Holger Eichelberger
 */
class CommandSetGroupingVisitor implements ICoordinationCommandVisitor {

    private Map<String, CommandGroup> groups = new HashMap<String, CommandGroup>();
    private AbstractCoordinationCommandExecutionVisitor executor;
    
    /**
     * Stores information about a group of commands for the same pipeline element.
     * 
     * @author Holger Eichelberger
     */
    private class CommandGroup {
        private List<AlgorithmChangeCommand> algCmds = new ArrayList<AlgorithmChangeCommand>();
        private List<ParameterChangeCommand<?>> paramCmds = new ArrayList<ParameterChangeCommand<?>>();
        
        /**
         * Adds an algorithm change command.
         * 
         * @param cmd the command
         */
        private void addCommand(AlgorithmChangeCommand cmd) {
            algCmds.add(cmd);
        }
        
        /**
         * Adds a parameter change command.
         * 
         * @param cmd the command
         */
        private void addCommand(ParameterChangeCommand<?> cmd) {
            paramCmds.add(cmd);
        }
        
        /**
         * Turns stored parameter change commands into parameter changes.
         * 
         * @param excludeFirst whether all or only the rest shall be returned.
         * @return the parameter changes, <b>null</b> if there are none
         */
        private List<ParameterChange> parameterChanges(boolean excludeFirst) {
            return toChanges(paramCmds, excludeFirst);
        }
        
    }

    /**
     * Turns parameter change commands into parameter changes.
     * 
     * @param paramCmds the parameter change commands
     * @param excludeFirst whether all or only the rest shall be returned.
     * @return the parameter changes, <b>null</b> if there are none
     */
    static List<ParameterChange> toChanges(List<ParameterChangeCommand<?>> paramCmds, boolean excludeFirst) {
        List<ParameterChange> result;
        if (paramCmds.size() - (excludeFirst ? 1 : 0) > 0) {
            result = new ArrayList<>();
            for (ParameterChangeCommand<?> cmd : paramCmds) {
                if (excludeFirst) {
                    excludeFirst = false;
                } else {
                    result.add(new ParameterChange(cmd.getParameter(), cmd.getValue()));
                }
            }
        } else {
            result = null;
        }
        return result;        
    }
    
    /**
     * Calculates the grouping key for <code>cmd</code>.
     * 
     * @param cmd the command
     * @return the key
     */
    static String getKey(AbstractPipelineElementCommand cmd) {
        return cmd.getPipeline() + "/" + cmd.getPipelineElement();
    }
    
    /**
     * Returns the group or creates it.
     * 
     * @param cmd the command
     * @return the group
     */
    private CommandGroup getElementGroup(AbstractPipelineElementCommand cmd) {
        String key = getKey(cmd);
        CommandGroup group = groups.get(key);
        if (null == group) {
            group = new CommandGroup();
            groups.put(key, group);
        }
        return group;
    }
    
    /**
     * Adds a command to the elements grouping.
     * 
     * @param cmd the command
     * @return <b>null</b>
     */
    private CoordinationExecutionResult handleElementsCommand(AbstractPipelineElementCommand cmd) {
        CoordinationExecutionResult result = null;
        if (null != executor) {
            String key = getKey(cmd);
            if (groups.containsKey(key)) {
                CommandGroup group = groups.get(key);
                if (null != group) {
                    if (1 == group.algCmds.size()) {
                        result = executor.handleAlgorithmChange(group.algCmds.get(0), group.parameterChanges(false));
                        groups.remove(key);
                    } else if (0 == group.algCmds.size()) {
                        result = executor.handleParameterChange(group.paramCmds.get(0), group.parameterChanges(true));
                        groups.remove(key);
                    } else {
                        groups.put(key, null); // mark as non-groupable
                    }
                } else { // marked as non-groupable
                    cmd.accept(executor);
                }
            } // else ... already done
        }
        return result;
    }

    /**
     * Adds a command to the other commands.
     * 
     * @param cmd the command
     * @return <b>null</b> without {@link #executor}, the execution result else
     */
    private CoordinationExecutionResult handleOther(CoordinationCommand cmd) {
        CoordinationExecutionResult result;
        if (null != executor) {
            result = cmd.accept(executor);
        } else {
            result = null;
        }
        return result;
    }
    
    /**
     * Defines the executor. Switches from collection to execution phase.
     * 
     * @param executor the executor
     */
    void setExecutor(AbstractCoordinationCommandExecutionVisitor executor) {
        this.executor = executor;
    }
    
    @Override
    public CoordinationExecutionResult visitAlgorithmChangeCommand(AlgorithmChangeCommand command) {
        CoordinationExecutionResult result;
        if (null == executor) {
            getElementGroup(command).addCommand(command);
            result = null;
        } else {
            result = handleElementsCommand(command);
        }
        return result;
    }

    @Override
    public CoordinationExecutionResult visitCommandSequence(CommandSequence command) {
        return handleOther(command);
    }

    @Override
    public CoordinationExecutionResult visitCommandSet(CommandSet command) {
        return handleOther(command);
    }

    @Override
    public CoordinationExecutionResult visitParameterChangeCommand(ParameterChangeCommand<?> command) {
        CoordinationExecutionResult result;
        if (null == executor) {
            getElementGroup(command).addCommand(command);
            result = null;
        } else {
            result = handleElementsCommand(command);
        }
        return result;
    }

    @Override
    public CoordinationExecutionResult visitPipelineCommand(PipelineCommand command) {
        return handleOther(command);
    }

    @Override
    public CoordinationExecutionResult visitScheduleWavefrontAdaptationCommand(
        ScheduleWavefrontAdaptationCommand command) {
        return handleOther(command);
    }

    @Override
    public CoordinationExecutionResult visitMonitoringChangeCommand(MonitoringChangeCommand command) {
        return handleOther(command);
    }

    @Override
    public CoordinationExecutionResult visitParallelismChangeCommand(ParallelismChangeCommand command) {
        return handleOther(command);
    }

    @Override
    public CoordinationExecutionResult visitProfileAlgorithmCommand(ProfileAlgorithmCommand command) {
        return handleOther(command);
    }

    @Override
    public CoordinationExecutionResult visitShutdownCommand(ShutdownCommand command) {
        return handleOther(command);
    }

    @Override
    public CoordinationExecutionResult visitUpdateCommand(UpdateCommand command) {
        return handleOther(command);
    }

    @Override
    public CoordinationExecutionResult visitReplayCommand(ReplayCommand command) {
        return handleOther(command);
    }

    @Override
    public CoordinationExecutionResult visitLoadScheddingCommand(LoadSheddingCommand command) {
        return handleOther(command);
    }

    @Override
    public CoordinationExecutionResult visitCloudExecutionCommand(CoordinationCommand command) {
        return null;
    }

}
