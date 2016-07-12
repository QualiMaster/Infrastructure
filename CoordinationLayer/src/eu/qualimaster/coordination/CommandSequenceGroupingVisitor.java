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
import java.util.List;

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
 * Tries to group commands in a sequence together. Call {@link #setExecutor(CoordinationCommandExecutionVisitor)} before
 * and {@link #flush()} afterwards.
 *  
 * @author Holger Eichelberger
 */
public class CommandSequenceGroupingVisitor implements ICoordinationCommandVisitor {

    private String key;
    private CoordinationCommandExecutionVisitor executor;
    private AlgorithmChangeCommand algCmd;
    private List<ParameterChangeCommand<?>> paramCmds = new ArrayList<ParameterChangeCommand<?>>();
    
    /**
     * Defines the executor. Switches from collection to execution phase.
     * 
     * @param executor the executor
     */
    void setExecutor(CoordinationCommandExecutionVisitor executor) {
        this.executor = executor;
        this.key = null;
        this.algCmd = null;
        this.paramCmds.clear();
    }
    
    /**
     * Flushes the commands stored in this visitor.
     * 
     * @return the execution result
     */
    public CoordinationExecutionResult flush() {
        CoordinationExecutionResult result = null;
        if (null != algCmd) {
            result = executor.handleAlgorithmChange(algCmd, 
                 CommandSetGroupingVisitor.toChanges(paramCmds, false));
        } else if (!paramCmds.isEmpty()) {
            result = executor.handleParameterChange(paramCmds.get(0), 
                 CommandSetGroupingVisitor.toChanges(paramCmds, true));
        }
        key = null;
        algCmd = null;
        paramCmds.clear();
        return result;
    }

    /**
     * Checks whether <code>command</code> shall cause a {@link #flush()}.
     * 
     * @param command the command
     * @param flush enforce a flush
     * @return the execution result
     */
    private CoordinationExecutionResult checkFlush(AbstractPipelineElementCommand command, boolean flush) {
        CoordinationExecutionResult result = null;
        String key = CommandSetGroupingVisitor.getKey(command);
        if (this.key == null || !this.key.equals(key) || flush) {
            result = flush();
        }
        this.key = key;
        return result;
    }
    
    /**
     * Handle other commands.
     * 
     * @param command the command
     * @return the execution result
     */
    private CoordinationExecutionResult handleOther(CoordinationCommand command) {
        CoordinationExecutionResult result = flush();
        if (null == result) {
            result = command.accept(executor);
        }
        return result;
    }
    
    @Override
    public CoordinationExecutionResult visitAlgorithmChangeCommand(AlgorithmChangeCommand command) {
        CoordinationExecutionResult result = checkFlush(command, algCmd != null);
        algCmd = command;
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
        CoordinationExecutionResult result = checkFlush(command, false);
        paramCmds.add(command);
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
