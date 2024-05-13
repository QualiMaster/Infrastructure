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
import eu.qualimaster.monitoring.events.CloudResourceMonitoringEvent;
import eu.qualimaster.monitoring.systemState.CloudEnvironmentSystemPart;
import eu.qualimaster.monitoring.systemState.PlatformSystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.observables.IObservable;

/**
 * Implements the handling of a {@link CloudResourceMonitoringEvent}.
 * 
 * @author Holger Eichelberger
 */
public class CloudResourceMonitoringEventHandler extends MonitoringEventHandler<CloudResourceMonitoringEvent> {

    public static final CloudResourceMonitoringEventHandler INSTANCE = new CloudResourceMonitoringEventHandler();

    /**
     * Creates an event handler.
     */
    private CloudResourceMonitoringEventHandler() {
        super(CloudResourceMonitoringEvent.class);
    }

    @Override
    protected void handle(CloudResourceMonitoringEvent event, SystemState state) {
        PlatformSystemPart psp = state.getPlatform();
        CloudEnvironmentSystemPart cloudEnv = psp.obtainCloudEnvironment(event.getCloudEnvironment());
        for (Map.Entry<IObservable, Double> entry : event.getObservations().entrySet()) {
            cloudEnv.setValue(entry.getKey(), entry.getValue(), null);
        }
    }

}
