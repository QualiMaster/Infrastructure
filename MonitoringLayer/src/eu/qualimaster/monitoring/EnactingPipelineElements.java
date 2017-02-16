package eu.qualimaster.monitoring;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.monitoring.events.FrozenSystemState;
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
public class EnactingPipelineElements {

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
                if (1 == modifyCount(enactingPipelines, pipeline, 1)) {
                    enacting.put(pipeline, 0L);
                }
                if (1 == modifyCount(enactingPipelines, fPath, 1)) {
                    enacting.put(fPath, System.currentTimeMillis());
                }
            } else {
                if (0 == modifyCount(enactingPipelines, pipeline, -1)) {
                    enacting.remove(pipeline);
                }
                if (0 == modifyCount(enactingPipelines, fPath, -1)) {
                    Long start = enacting.remove(fPath);
                    if (null != start) {
                        enactmentDelay = System.currentTimeMillis() - start;
                    }
                }
            }
            PipelineSystemPart psp = MonitoringManager.getSystemState().obtainPipeline(pipeline);
            if (null != psp) {
                psp.setValue(AnalysisObservables.IS_ENACTING, isEnacting(pipeline) ? 1.0 : 0.0, null);
                PipelineNodeSystemPart pnsp = psp.obtainPipelineNode(pipelineElement);
                if (null != pnsp) {
                    pnsp.setValue(AnalysisObservables.IS_ENACTING, isEnacting(fPath) ? 1.0 : 0.0, null);
                    if (enactmentDelay > 0) {
                        pnsp.setValue(TimeBehavior.ENACTMENT_DELAY, enactmentDelay, null);
                    }
                }
            }
        }
    }
    
    /**
     * Modified a counter stored in a map.
     * 
     * @param map the map to be modified
     * @param id the id of the map entry
     * @param change the value change
     * @return the actual value after modification (<code>0</code> even if there was no modification or the entry did 
     *   not exist)
     */
    private static int modifyCount(Map<String, Integer> map, String id, int change) {
        Integer count = map.get(id);
        if (change > 0) {
            if (null == count) {
                count = 0;
            } 
            count += change;
            map.put(id, count);
        } else if (change < 0) {
            if (null != count) { // just to be sure - also for tests
                count = count + change;
                if (count <= 0) {
                    map.remove(id);
                    count = 0;
                } else {
                    map.put(id, count);
                }
            }
        }
        if (null == count) {
            count = 0;
        }
        return count;
    }
    
    /**
     * Returns whether an enactment is happening on the given frozen state path.
     * 
     * @param fPath the frozen state path to look for
     * @return <code>true</code> in case of a running enactment, <code>false</code> else
     */
    public boolean isEnacting(String fPath) {
        return enacting.containsKey(fPath);
    }
    
    /**
     * Handles a coordination command.
     * 
     * @param command the command
     * @param mark whether the command shall be marked as enacting or unmarked
     */
    public void handle(CoordinationCommand command, boolean mark) {
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
     * Handles pipeline lifecycle events.
     * 
     * @param event the event to handle
     */
    public void handle(PipelineLifecycleEvent event) {
        switch (event.getStatus()) {
        case STARTING:
        case STOPPED:
            clearAll(enacting, event.getPipeline());
            clearAll(enactingPipelines, event.getPipeline());
            break;
        default:
            break;
        }
    }
    
    /**
     * Clears all entries from map with key equals <code>name</code> or starting with 
     * <code>name</code> + {@link FrozenSystemState#SEPARATOR} as prefix.
     *
     * @param <T> the value type
     * @param map the map to clear
     * @param name the name for entries to clear
     */
    private static <T> void clearAll(Map<String, T> map, String name) {
        String namePrefix = name + FrozenSystemState.SEPARATOR;
        Iterator<Map.Entry<String, T>> iter = map.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, T> entry = iter.next();
            String key = entry.getKey();
            if (key.equals(name) || key.startsWith(namePrefix)) {
                iter.remove();
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
                return markAsEnacting(command, mark);
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

    /**
     * Marks the executors given in <code>command</code> as enacting.
     * 
     * @param command the command
     * @param mark or unmark the pipeline / pipeline elements
     * @return <b>null</b>
     */
    private CoordinationExecutionResult markAsEnacting(ParallelismChangeCommand command, boolean mark) {
        String pipeline = command.getPipeline();
        if (null != command.getExecutors()) {
            for (String exec : command.getExecutors().keySet()) {
                markAsEnacting(pipeline, exec, mark);
            }
        } else if (null != command.getIncrementalChanges()) {
            for (String exec : command.getIncrementalChanges().keySet()) {
                markAsEnacting(pipeline, exec, mark);
            }
        }
        return null;
    }
    
}
