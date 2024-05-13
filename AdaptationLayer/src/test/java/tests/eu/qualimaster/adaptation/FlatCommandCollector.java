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
package tests.eu.qualimaster.adaptation;

import java.util.ArrayList;
import java.util.List;

import eu.qualimaster.coordination.commands.AbstractCommandContainer;
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
 * Collections commands for testing and flattens collections.
 * 
 * @author Holger Eichelberger
 */
public class FlatCommandCollector implements ICoordinationCommandVisitor {

    private List<CoordinationCommand> commands = new ArrayList<CoordinationCommand>();

    /**
     * Returns the (flattened) result of visiting.
     * 
     * @return all atomic commands
     */
    public List<CoordinationCommand> getResult() {
        return commands;
    }
    
    @Override
    public CoordinationExecutionResult visitAlgorithmChangeCommand(AlgorithmChangeCommand command) {
        commands.add(command);
        return null;
    }

    /**
     * Visits a command container.
     * 
     * @param command the command container
     * @return the execution result (<b>null</b> by default)
     */
    private CoordinationExecutionResult visitCommandContainer(AbstractCommandContainer command) {
        for (int c = 0; c < command.getCommandCount(); c++) {
            command.getCommand(c).accept(this);
        }
        return null;
    }
    
    @Override
    public CoordinationExecutionResult visitCommandSequence(CommandSequence command) {
        return visitCommandContainer(command);
    }

    @Override
    public CoordinationExecutionResult visitCommandSet(CommandSet command) {
        return visitCommandContainer(command);
    }

    @Override
    public CoordinationExecutionResult visitParameterChangeCommand(ParameterChangeCommand<?> command) {
        commands.add(command);
        return null;
    }

    @Override
    public CoordinationExecutionResult visitPipelineCommand(PipelineCommand command) {
        commands.add(command);
        return null;
    }

    @Override
    public CoordinationExecutionResult visitScheduleWavefrontAdaptationCommand(
        ScheduleWavefrontAdaptationCommand command) {
        commands.add(command);
        return null;
    }

    @Override
    public CoordinationExecutionResult visitMonitoringChangeCommand(MonitoringChangeCommand command) {
        commands.add(command);
        return null;
    }

    @Override
    public CoordinationExecutionResult visitParallelismChangeCommand(ParallelismChangeCommand command) {
        commands.add(command);
        return null;
    }

    @Override
    public CoordinationExecutionResult visitProfileAlgorithmCommand(ProfileAlgorithmCommand command) {
        commands.add(command);
        return null;
    }

    @Override
    public CoordinationExecutionResult visitShutdownCommand(ShutdownCommand command) {
        commands.add(command);
        return null;
    }

    @Override
    public CoordinationExecutionResult visitUpdateCommand(UpdateCommand command) {
        commands.add(command);
        return null;
    }

    @Override
    public CoordinationExecutionResult visitReplayCommand(ReplayCommand command) {
        commands.add(command);
        return null;
    }

    @Override
    public CoordinationExecutionResult visitLoadScheddingCommand(LoadSheddingCommand command) {
        commands.add(command);
        return null;
    }

    @Override
    public CoordinationExecutionResult visitCloudExecutionCommand(CoordinationCommand command) {
        commands.add(command);
        return null;
    }

}
