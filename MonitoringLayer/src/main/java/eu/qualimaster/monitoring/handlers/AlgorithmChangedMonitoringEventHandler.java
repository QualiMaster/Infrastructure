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
package eu.qualimaster.monitoring.handlers;

import eu.qualimaster.coordination.INameMapping;
import eu.qualimaster.coordination.INameMapping.Algorithm;
import eu.qualimaster.monitoring.MonitoringEventHandler;
import eu.qualimaster.monitoring.events.AlgorithmChangedMonitoringEvent;
import eu.qualimaster.monitoring.profiling.AlgorithmProfilePredictionManager;
import eu.qualimaster.monitoring.systemState.NodeImplementationSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineSystemPart;
import eu.qualimaster.monitoring.systemState.SystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;

/**
 * Implements the handling of {@link AlgorithmChangedMonitoringEvent}.
 * 
 * @author Holger Eichelberger
 */
public class AlgorithmChangedMonitoringEventHandler extends MonitoringEventHandler<AlgorithmChangedMonitoringEvent> {

    public static final AlgorithmChangedMonitoringEventHandler INSTANCE = new AlgorithmChangedMonitoringEventHandler();
    
    /**
     * Creates an instance.
     */
    private AlgorithmChangedMonitoringEventHandler() {
        super(AlgorithmChangedMonitoringEvent.class);
    }

    @Override
    protected void handle(AlgorithmChangedMonitoringEvent event, SystemState state) {
        handle(event, state, true);
        handle(event, state, false);
    }

    /**
     * Handles an event of the supported event type.
     * 
     * @param event the event to be handled
     * @param state the actual system state to be modified
     * @param forMainPipeline handle for main pipeline (<code>true</code>) or try handling for a 
     *   sub-pipeline (<code>false</code>)
     */
    private void handle(AlgorithmChangedMonitoringEvent event, SystemState state, boolean forMainPipeline) {
        SystemPart target = determineAggregationPart(event, state, forMainPipeline);
        if (target instanceof PipelineNodeSystemPart) {
            INameMapping mapping = getNameMapping(event.getPipeline());
            String algName = event.getAlgorithm();
            Algorithm alg = mapping.getAlgorithmByImplName(event.getAlgorithm());
            if (null != alg) {
                algName = alg.getName();
            }
            PipelineSystemPart pip = state.obtainPipeline(event.getPipeline());
            NodeImplementationSystemPart aPart = pip.getAlgorithm(algName);
            if (null != aPart) {
                ((PipelineNodeSystemPart) target).setCurrent(aPart);
            } else {
                getLogger().info("cannot find/map back algorithm '" + event.getAlgorithm() + "': ignoring " + event);
            }
        }
        if (null != target) {
            AlgorithmProfilePredictionManager.notifyAlgorithmChanged(event);
        }
    }

}
