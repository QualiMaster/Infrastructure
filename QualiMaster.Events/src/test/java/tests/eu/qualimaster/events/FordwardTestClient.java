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
import eu.qualimaster.events.EventHandler;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.monitoring.events.HardwareAliveEvent;
import eu.qualimaster.monitoring.events.MonitoringEvent;

/**
 * A test client for inter-JVM-message forwarding.
 * 
 * @author Holger Eichelberger
 */
public class FordwardTestClient {

    /**
     * Implements an event handler.
     * 
     * @author Holger Eichelberger
     */
    private static class PipelineLifecycleEventHandler extends EventHandler<PipelineLifecycleEvent> {
        
        /**
         * Creates an event handler.
         */
        protected PipelineLifecycleEventHandler() {
            super(PipelineLifecycleEvent.class);
        }

        @Override
        protected void handle(PipelineLifecycleEvent event) {
            System.out.println("LIFECYCLE " + event);            
        }

    }
    
    /**
     * Implements a monitoring handler.
     * 
     * @author Holger Eichelberger
     */
    private static class MonitoringHandler extends EventHandler<MonitoringEvent> {

        /**
         * Creates the handler.
         */
        public MonitoringHandler() {
            super(MonitoringEvent.class);
        }
        
        @Override
        protected void handle(MonitoringEvent event) {
            System.out.println("MONITORING " + event);
        }
    }

    /**
     * Starts and executes the client.
     * 
     * @param args ignored
     */
    public static void main(String[] args) {
        Configuration.configureLocal();
        EventManager.start();
        EventManager.register(new PipelineLifecycleEventHandler());
        EventManager.register(new MonitoringHandler());
        while (true) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
            EventManager.send(new HardwareAliveEvent("HARDWARE"));
        }
    }
    
}
