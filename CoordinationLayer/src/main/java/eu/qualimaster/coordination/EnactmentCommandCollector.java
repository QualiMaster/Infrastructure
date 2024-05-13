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

import java.util.ArrayList;
import java.util.List;

import eu.qualimaster.coordination.commands.AbstractCoordinationCommandVisitor;
import eu.qualimaster.coordination.commands.AlgorithmChangeCommand;
import eu.qualimaster.coordination.commands.CoordinationCommand;
import eu.qualimaster.coordination.commands.CoordinationExecutionResult;
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
 * Collects (flat) enactment commands.
 * 
 * @author Holger Eichelberger
 */
public class EnactmentCommandCollector extends AbstractCoordinationCommandVisitor {

    private List<CoordinationCommand> result = new ArrayList<CoordinationCommand>();

    /**
     * Returns the collected result.
     * 
     * @return the result
     */
    public List<CoordinationCommand> getResult() {
        return result;
    }
    
    @Override
    public CoordinationExecutionResult visitAlgorithmChangeCommand(AlgorithmChangeCommand command) {
        result.add(command);
        return null;
    }

    @Override
    public CoordinationExecutionResult visitParameterChangeCommand(ParameterChangeCommand<?> command) {
        result.add(command);
        return null;
    }

    @Override
    public CoordinationExecutionResult visitPipelineCommand(PipelineCommand command) {
        // not real enactment
        return null;
    }

    @Override
    public CoordinationExecutionResult visitScheduleWavefrontAdaptationCommand(
        ScheduleWavefrontAdaptationCommand command) {
        // unclear -> signature
        return null;
    }

    @Override
    public CoordinationExecutionResult visitMonitoringChangeCommand(MonitoringChangeCommand command) {
        // unclear -> signature
        return null;
    }

    @Override
    public CoordinationExecutionResult visitParallelismChangeCommand(ParallelismChangeCommand command) {
        result.add(command);
        return null;
    }

    @Override
    public CoordinationExecutionResult visitProfileAlgorithmCommand(ProfileAlgorithmCommand command) {
        // no enactment
        return null;
    }

    @Override
    public CoordinationExecutionResult visitShutdownCommand(ShutdownCommand command) {
        // no enactment
        return null;
    }

    @Override
    public CoordinationExecutionResult visitUpdateCommand(UpdateCommand command) {
        // no enactment
        return null;
    }

    @Override
    public CoordinationExecutionResult visitReplayCommand(ReplayCommand command) {
        result.add(command);
        return null;
    }

    @Override
    public CoordinationExecutionResult visitLoadScheddingCommand(LoadSheddingCommand command) {
        result.add(command);
        return null;
    }

    @Override
    public CoordinationExecutionResult visitCloudExecutionCommand(CoordinationCommand command) {
        return null;
    }

}
