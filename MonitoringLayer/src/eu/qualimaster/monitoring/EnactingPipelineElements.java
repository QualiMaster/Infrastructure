package eu.qualimaster.monitoring;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.qualimaster.coordination.EnactmentCommandCollector;
import eu.qualimaster.coordination.commands.AbstractCoordinationCommandVisitor;
import eu.qualimaster.coordination.commands.AlgorithmChangeCommand;
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
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineSystemPart;
import eu.qualimaster.observables.AnalysisObservables;
import eu.qualimaster.observables.TimeBehavior;

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

/**
 * Stores actually enacting pipeline elements, to avoid sending further constraint violations. The layers
 * store this information individually so that infrastructure layers may be distributed.
 * 
 * @author Holger Eichelberger
 */
class EnactingPipelineElements {

    public static final EnactingPipelineElements INSTANCE = new EnactingPipelineElements();
    private Map<String, Long> enacting = Collections.synchronizedMap(new HashMap<String, Long>());
    private Map<String, Integer> enactingPipelines = Collections.synchronizedMap(new HashMap<String, Integer>());

    /**
     * Prevents external instantiation.
     */
    private EnactingPipelineElements() {
    }
    
    /**
     * Marks a pipeline element as enacting / in enactment.
     * 
     * @param pipeline the pipeline
     * @param pipelineElement the pipeline element
     * @param mark or unmark the pipeline / pipeline element
     */
    private void markAsEnacting(String pipeline, String pipelineElement, boolean mark) {
        List<String> path = PipelineUtils.constructPath(pipeline, pipelineElement);
        if (!path.isEmpty()) {
            String fPath = PipelineUtils.toFrozenStatePath(path);
            long enactmentDelay = -1;
            if (mark) {
                enacting.put(fPath, System.currentTimeMillis());
                Integer count = enactingPipelines.get(pipelineElement);
                if (null == count) {
                    count = 1;
                } else {
                    count++;
                }
                enactingPipelines.put(pipelineElement, count);
            } else {
                Long start = enacting.remove(fPath);
                if (null != start) {
                    enactmentDelay = System.currentTimeMillis() - start;
                }
                Integer count = enactingPipelines.get(pipelineElement);
                if (null != count) { // just to be sure - also for tests
                    if (1 == count) {
                        enactingPipelines.remove(pipelineElement);
                    } else {
                        enactingPipelines.put(pipelineElement, count - 1);
                    }
                }
            }
            PipelineSystemPart psp = MonitoringManager.getSystemState().getPipeline(pipeline);
            if (null != psp) {
                if (enactingPipelines.containsKey(pipeline)) {
                    psp.setValue(AnalysisObservables.IS_ENACTING, 
                        enactingPipelines.containsKey(pipeline) ? 1.0 : 0.0, null);
                }
                PipelineNodeSystemPart pnsp = psp.getPipelineNode(pipelineElement);
                if (null != pnsp) {
                    pnsp.setValue(AnalysisObservables.IS_ENACTING, mark ? 1.0 : 0.0, null);
                    pnsp.setValue(TimeBehavior.ENACTMENT_DELAY, enactmentDelay, null);
                }
            }
        }
    }
    
    /**
     * Returns whether an enactment is happening on the given frozen state path.
     * 
     * @param fPath the frozen state path to look for
     * @return <code>true</code> in case of a running enactment, <code>false</code> else
     */
    boolean isEnacting(String fPath) {
        return enacting.containsKey(fPath);
    }
    
    /**
     * Handles a coordination command.
     * 
     * @param command the command
     * @param mark whether the command shall be marked as enacting or unmarked
     */
    void handle(CoordinationCommand command, boolean mark) {
        if (null != command) {
            EnactmentCommandCollector collector = new EnactmentCommandCollector();
            command.accept(collector);
            List<CoordinationCommand> commands = collector.getResult();
            for (CoordinationCommand cmd : commands) {
                cmd.accept(getCommandVisitor(mark));
            }
        }
    }
    
    /**
     * Returns a command visitor for marking / unmarking pipeline elements.
     * 
     * @param mark whether the command shall be marked as enacting or unmarked
     * @return the visitor
     */
    public ICoordinationCommandVisitor getCommandVisitor(final boolean mark) {
        return new AbstractCoordinationCommandVisitor() {
            @Override
            public CoordinationExecutionResult visitAlgorithmChangeCommand(AlgorithmChangeCommand command) {
                markAsEnacting(command.getPipeline(), command.getPipelineElement(), mark);
                return null;
            }

            @Override
            public CoordinationExecutionResult visitParameterChangeCommand(ParameterChangeCommand<?> command) {
                markAsEnacting(command.getPipeline(), command.getPipelineElement(), mark);
                return null;
            }

            @Override
            public CoordinationExecutionResult visitPipelineCommand(PipelineCommand command) {
                // done by pipeline states
                return null;
            }

            @Override
            public CoordinationExecutionResult visitScheduleWavefrontAdaptationCommand(
                ScheduleWavefrontAdaptationCommand command) {
                return null; // TODO unclear, all to enacting
            }

            @Override
            public CoordinationExecutionResult visitMonitoringChangeCommand(MonitoringChangeCommand command) {
                return null;
            }

            @Override
            public CoordinationExecutionResult visitParallelismChangeCommand(ParallelismChangeCommand command) {
                return null; // TODO unclear
            }

            @Override
            public CoordinationExecutionResult visitProfileAlgorithmCommand(ProfileAlgorithmCommand command) {
                return null; // without adaptation
            }

            @Override
            public CoordinationExecutionResult visitShutdownCommand(ShutdownCommand command) {
                return null; // without adaptation
            }

            @Override
            public CoordinationExecutionResult visitUpdateCommand(UpdateCommand command) {
                return null; // without adaptation
            }

            @Override
            public CoordinationExecutionResult visitReplayCommand(ReplayCommand command) {
                markAsEnacting(command.getPipeline(), command.getPipelineElement(), mark);
                return null;
            }

            @Override
            public CoordinationExecutionResult visitLoadScheddingCommand(LoadSheddingCommand command) {
                markAsEnacting(command.getPipeline(), command.getPipelineElement(), mark);
                return null;
            }
            
            @Override
            public CoordinationExecutionResult visitCloudExecutionCommand(CoordinationCommand command) {
                return null;
            }
        };
    }
}
