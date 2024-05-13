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
package eu.qualimaster.monitoring.handlers;

import java.util.Map;

import eu.qualimaster.monitoring.MonitoringEventHandler;
import eu.qualimaster.monitoring.events.PlatformMultiObservationHostMonitoringEvent;
import eu.qualimaster.monitoring.systemState.MachineSystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.observables.IObservable;

/**
 * Implements the handling of {@link PlatformMonitoringEvent}.
 * 
 * @author Holger Eichelberger
 */
public class PlatformMultiMonitoringHostEventHandler 
    extends MonitoringEventHandler<PlatformMultiObservationHostMonitoringEvent> {

    public static final PlatformMultiMonitoringHostEventHandler INSTANCE 
        = new PlatformMultiMonitoringHostEventHandler();

    /**
     * Creates an instance.
     */
    private PlatformMultiMonitoringHostEventHandler() {
        super(PlatformMultiObservationHostMonitoringEvent.class);
    }

    @Override
    protected void handle(PlatformMultiObservationHostMonitoringEvent event, SystemState state) {
        MachineSystemPart machine = state.getPlatform().obtainMachine(event.getHost());
        for (Map.Entry<IObservable, Double> entry : event.getObservations().entrySet()) {
            machine.setValue(entry.getKey(), entry.getValue(), null);
        }
    }

}
