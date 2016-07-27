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
import eu.qualimaster.monitoring.AbstractMonitoringTask;
import eu.qualimaster.monitoring.MonitoringEventHandler;
import eu.qualimaster.monitoring.MonitoringManager;
import eu.qualimaster.monitoring.events.AlgorithmMonitoringEvent;
import eu.qualimaster.monitoring.systemState.NodeImplementationSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineSystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.observables.IObservable;

/**
 * Implements the handling of {@link AlgorithmMonitoringEvent}.
 * 
 * @author Holger Eichelberger
 */
public class AlgorithmMonitoringEventHandler extends MonitoringEventHandler<AlgorithmMonitoringEvent> {

    public static final AlgorithmMonitoringEventHandler INSTANCE = new AlgorithmMonitoringEventHandler();
    
    /**
     * Creates an instance.
     */
    private AlgorithmMonitoringEventHandler() {
        super(AlgorithmMonitoringEvent.class);
    }

    @Override
    protected void handle(AlgorithmMonitoringEvent event, SystemState state) {
        INameMapping mapping = MonitoringManager.getNameMappingForClass(event.getAlgorithmId());
        if (null != mapping) {
            IObservable obs = event.getObservable();
            if (!obs.isInternal()) {
                Algorithm algorithm = mapping.getAlgorithmByClassName(event.getAlgorithmId());
                PipelineSystemPart pip = getActivePipeline(state, event.getTopologyId()); // TODO check!
                if (null != pip) {
                    NodeImplementationSystemPart algPart = pip.getAlgorithm(algorithm.getName());
                    algPart.setValue(obs, event.getValue(), event.getTopologyId());
                    AbstractMonitoringTask.sendSummaryEvent(algPart, mapping.getPipelineName(), 
                        MonitoringManager.DEMO_MSG_PROCESSING_ALGORITHM);
                }
            }
        }
    }

}
