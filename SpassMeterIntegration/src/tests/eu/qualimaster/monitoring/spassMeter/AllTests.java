package tests.eu.qualimaster.monitoring.spassMeter;

import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.events.EventHandler;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.monitoring.events.AlgorithmMonitoringEvent;
import eu.qualimaster.monitoring.events.PlatformMonitoringEvent;

/**
 * Executes all tests. Currently, we do not need a test suite here...
 * 
 * <b>Important:</b> Please use ANT to run this test as the "real" jars with the packaged
 * integration are in <code>dist</code>. 
 * 
 * @author Holger Eichelberger
 */
public class AllTests {

    /**
     * Implements a platform monitoring event handler to collect platform monitoring events.
     * 
     * @author Holger Eichelberger
     */
    private static class PlatformEventHandler extends EventHandler<PlatformMonitoringEvent> {

        private int count;

        /**
         * The platform event handler.
         */
        protected PlatformEventHandler() {
            super(PlatformMonitoringEvent.class);
        }

        @Override
        protected void handle(PlatformMonitoringEvent event) {
            count++;
        }
        
        /**
         * Returns the number of collected events.
         * 
         * @return the number of collected events
         */
        public int getCount() {
            return count;
        }
        
    }

    /**
     * Implements an algorithm monitoring event handler to collect platform monitoring events.
     * 
     * @author Holger Eichelberger
     */
    private static class MonitoringEventHandler extends EventHandler<AlgorithmMonitoringEvent> {

        private int count;

        /**
         * The monitoring event handler.
         */
        protected MonitoringEventHandler() {
            super(AlgorithmMonitoringEvent.class);
        }

        @Override
        protected void handle(AlgorithmMonitoringEvent event) {
            count++;
        }

        /**
         * Returns the number of collected events.
         * 
         * @return the number of collected events
         */
        public int getCount() {
            return count;
        }

    }

    /**
     * Tests the instrumented code by receiving events from the QM event manager.
     */
    @Test
    public void testInstrumentation() {
        PlatformEventHandler pHandler = new PlatformEventHandler();
        MonitoringEventHandler mHandler = new MonitoringEventHandler();
        EventManager.register(pHandler);
        EventManager.register(mHandler);
        MemTest.doTest();
        try {
            Thread.sleep(2000); // spass-qm-event processing
        } catch (InterruptedException e) {
        }
        EventManager.cleanup();
        EventManager.stop();
        Assert.assertTrue(pHandler.getCount() > 0);
        Assert.assertTrue(mHandler.getCount() > 0);
    }

}
