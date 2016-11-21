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

import java.util.Map;

import eu.qualimaster.monitoring.MonitoringEventHandler;
import eu.qualimaster.monitoring.events.PipelineElementMultiObservationMonitoringEvent;
import eu.qualimaster.monitoring.systemState.StateUtils;
import eu.qualimaster.monitoring.systemState.SystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.observables.IObservable;

/**
 * Implements the handling of {@link PipelineElementMultiObservationMonitoringEvent}.
 * 
 * @author Holger Eichelberger
 */
public class PipelineElementMultiObservationMonitoringEventHandler 
    extends MonitoringEventHandler<PipelineElementMultiObservationMonitoringEvent> {

    public static final PipelineElementMultiObservationMonitoringEventHandler INSTANCE 
        = new PipelineElementMultiObservationMonitoringEventHandler();

    /**
     * Creates an instance.
     */
    private PipelineElementMultiObservationMonitoringEventHandler() {
        super(PipelineElementMultiObservationMonitoringEvent.class);
    }

    @Override
    protected void handle(PipelineElementMultiObservationMonitoringEvent event, SystemState state) {
        SystemPart target = determineAggregationPart(event, state);
        if (null != target) {
            Object key = event.getKey();
            boolean updateCapacity = false;
            for (Map.Entry<IObservable, Double> ent : event.getObservations().entrySet()) {
                IObservable observable = ent.getKey();
                if (!observable.isInternal()) {
                    StateUtils.setValue(target, observable, ent.getValue(), key);
                    updateCapacity |= StateUtils.changesLatency(observable);
                }
            }
            if (updateCapacity) {
                StateUtils.updateCapacity(target, key, false);
            }
            StateUtils.checkTaskAndExecutors(target, key);
        }
    }

}
