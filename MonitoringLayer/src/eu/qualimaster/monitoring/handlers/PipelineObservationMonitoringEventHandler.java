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
import eu.qualimaster.monitoring.events.PipelineObservationMonitoringEvent;
import eu.qualimaster.monitoring.systemState.PipelineSystemPart;
import eu.qualimaster.monitoring.systemState.StateUtils;
import eu.qualimaster.monitoring.systemState.SystemState;

/**
 * Implements the handling of {@link PipelineObservationMonitoringEvent}.
 * 
 * @author Holger Eichelberger
 */
public class PipelineObservationMonitoringEventHandler 
    extends MonitoringEventHandler<PipelineObservationMonitoringEvent> {

    public static final PipelineObservationMonitoringEventHandler INSTANCE 
        = new PipelineObservationMonitoringEventHandler();

    /**
     * Creates an instance.
     */
    private PipelineObservationMonitoringEventHandler() {
        super(PipelineObservationMonitoringEvent.class);
    }

    @Override
    protected void handle(PipelineObservationMonitoringEvent event, SystemState state) {
        PipelineSystemPart pPart = getActivePipeline(state, event.getPipeline());
        if (null != pPart) {
            StateUtils.setValue(pPart, event.getObservable(), 
                event.getObservation(), event.getKey());
        }
    }

}
