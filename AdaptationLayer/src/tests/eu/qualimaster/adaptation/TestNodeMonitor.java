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
package tests.eu.qualimaster.adaptation;

import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.adaptation.platform.NodeMonitor;
import eu.qualimaster.events.EventHandler;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.monitoring.events.PlatformMultiObservationHostMonitoringEvent;
import eu.qualimaster.observables.ResourceUsage;

/**
 * Tests {@link NodeMonitor}.
 * 
 * @author Holger Eichelberger
 */
public class TestNodeMonitor {
    
    /**
     * A test event handler.
     * 
     * @author Holger Eichelberger
     */
    private static class TestEventHandler extends EventHandler<PlatformMultiObservationHostMonitoringEvent> {

        private int countFullEvent = 0;
        private int countPartialEvent = 0;
        
        /**
         * Creates a handler instance.
         */
        protected TestEventHandler() {
            super(PlatformMultiObservationHostMonitoringEvent.class);
        }

        @Override
        protected void handle(PlatformMultiObservationHostMonitoringEvent event) {
            if (event.getObservations().containsKey(ResourceUsage.AVAILABLE_CPUS)) {
                countFullEvent++;
            } else if (event.getObservations().containsKey(ResourceUsage.LOAD)) {
                countPartialEvent++;
            }
        }
        
        /**
         * Asserts the expected events.
         */
        private void assertEvents() {
            Assert.assertTrue("no full events", countFullEvent > 0);
            Assert.assertTrue("no partial events", countPartialEvent > 0);
        }
        
    }
    
    /**
     * Tests the node monitor.
     */
    @Test
    public void testNodeMonitor() {
        EventManager.start(false, true);
        TestEventHandler handler = new TestEventHandler();
        EventManager.register(handler);
        NodeMonitor monitor = new NodeMonitor();
        monitor.start();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
        }
        monitor.stop();
        handler.assertEvents();
        EventManager.cleanup();
        EventManager.stop();
    }

}
