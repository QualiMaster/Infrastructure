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

import eu.qualimaster.monitoring.MonitoringEventHandler;
import eu.qualimaster.monitoring.events.SourceVolumeMonitoringEvent;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.monitoring.volumePrediction.VolumePredictionManager;

/**
 * A handler for source volume monitoring events.
 * 
 * @author Holger Eichelberger
 */
public class SourceVolumeMonitoringEventHandler extends MonitoringEventHandler<SourceVolumeMonitoringEvent> {

    public static final SourceVolumeMonitoringEventHandler INSTANCE = new SourceVolumeMonitoringEventHandler();
    
    /**
     * Creates an instance.
     */
    protected SourceVolumeMonitoringEventHandler() {
        super(SourceVolumeMonitoringEvent.class);
    }

    @Override
    protected void handle(SourceVolumeMonitoringEvent event, SystemState state) {
        VolumePredictionManager.notifySourceVolumeMonitoringEvent(event);
    }

}