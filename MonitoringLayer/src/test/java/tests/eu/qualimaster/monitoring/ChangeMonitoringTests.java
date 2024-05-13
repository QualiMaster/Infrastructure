package tests.eu.qualimaster.monitoring;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import tests.eu.qualimaster.coordination.TestNameMapping;
import eu.qualimaster.coordination.CoordinationManager;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.monitoring.MonitoringManager;
import eu.qualimaster.monitoring.events.ChangeMonitoringEvent;
import eu.qualimaster.observables.MonitoringFrequency;

/**
 * Tests for changing the monitoring.
 * 
 * @author Holger Eichelberger
 */
public class ChangeMonitoringTests {

    /**
     * Executed before a single test.
     */
    @Before
    public void setUp() {
        EventManager.start();
        CoordinationManager.start();
        CoordinationManager.registerTestMapping(TestNameMapping.INSTANCE);
        MonitoringManager.start(false);
    }
    
    /**
     * Executed after a single test.
     */
    @After
    public void tearDown() {
        MonitoringManager.stop();
        CoordinationManager.stop();
        EventManager.stop();
    }
    
    /**
     * Simple test, as changing monitoring is currently not implemented.
     */
    @Test
    public void testChangeMonitoring() {
        MonitoringManager.handleEvent(new ChangeMonitoringEvent(MonitoringFrequency.createAllMap(100), null));
        // TODO implement asserts -> shall lead to events
    }
    
}
