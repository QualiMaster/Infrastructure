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

import eu.qualimaster.common.signal.ConnectTaskMonitoringEvent;
import eu.qualimaster.monitoring.MonitoringEventHandler;
import eu.qualimaster.monitoring.systemState.SystemState;

/**
 * A handler for connect task monitoring events.
 * 
 * @author Holger Eichelberger
 */
public class ConnectTaskMonitoringEventHandler extends MonitoringEventHandler<ConnectTaskMonitoringEvent> {

    public static final ConnectTaskMonitoringEventHandler INSTANCE = new ConnectTaskMonitoringEventHandler();
    
    /**
     * Creates an instance.
     */
    private ConnectTaskMonitoringEventHandler() {
        super(ConnectTaskMonitoringEvent.class);
    }

    @Override
    protected void handle(ConnectTaskMonitoringEvent event, SystemState state) {
        // TODO search for use of key, discard old uses
    }

}
