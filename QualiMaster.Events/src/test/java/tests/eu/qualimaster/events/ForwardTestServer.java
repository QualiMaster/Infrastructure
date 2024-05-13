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
package tests.eu.qualimaster.events;

import eu.qualimaster.Configuration;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent.Status;
import eu.qualimaster.monitoring.events.ChangeMonitoringEvent;
import eu.qualimaster.observables.MonitoringFrequency;

/**
 * A test server for inter-JVM-message forwarding.
 * 
 * @author Holger Eichelberger
 */
public class ForwardTestServer {

    /**
     * Starts and executes the server.
     * 
     * @param args ignored
     */
    public static void main(String[] args) {
        Configuration.configureLocal();
        EventManager.start(false, true);
        while (true) {
            try {
                Thread.sleep(1000);
                EventManager.send(new PipelineLifecycleEvent("test", Status.UNKNOWN, null));
                Thread.sleep(1000);
                EventManager.send(new ChangeMonitoringEvent(MonitoringFrequency.createAllMap(100), null));
            } catch (InterruptedException e) {
            }
        }
    }
    
}
