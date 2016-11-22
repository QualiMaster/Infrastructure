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

import eu.qualimaster.monitoring.MonitoringEventHandler;
import eu.qualimaster.monitoring.events.PipelineElementObservationMonitoringEvent;
import eu.qualimaster.monitoring.systemState.StateUtils;
import eu.qualimaster.monitoring.systemState.SystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.observables.IObservable;

/**
 * Implements the handling of {@link PipelineElementObservationMonitoringEvent}.
 * 
 * @author Holger Eichelberger
 */
public class PipelineElementObservationMonitoringEventHandler 
    extends MonitoringEventHandler<PipelineElementObservationMonitoringEvent> {

    public static final PipelineElementObservationMonitoringEventHandler INSTANCE 
        = new PipelineElementObservationMonitoringEventHandler();
    
    /**
     * Creates an event handler.
     */
    private PipelineElementObservationMonitoringEventHandler() {
        super(PipelineElementObservationMonitoringEvent.class);
    }

    @Override
    protected void handle(PipelineElementObservationMonitoringEvent event, SystemState state) {
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
    private void handle(PipelineElementObservationMonitoringEvent event, SystemState state, boolean forMainPipeline) {
        SystemPart target = determineAggregationPart(event, state, forMainPipeline);
        if (null != target) {
            Object key = event.getKey();
            IObservable observable = event.getObservable();
            if (!observable.isInternal()) {
                StateUtils.setValue(target, observable, event.getObservation(), key);
                if (StateUtils.changesLatency(observable)) {
                    StateUtils.updateCapacity(target, key, false);
                }
            }
            StateUtils.checkTaskAndExecutors(target, key);
        }
    }

}
