package tests.eu.qualimaster.monitoring;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import tests.eu.qualimaster.monitoring.profiling.ProfilingTests;

/**
 * The test suite for the Data Management Layer. Do not rename this class.<br/>
 * Set environment variable 
 * "STORM_TEST_TIMEOUT_MS" to a value greater than 15.000 (ms).
 * 
 * @author Holger Eichelberger
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({SimpleMonitoringTests.class, ChangeMonitoringTests.class, StormTest.class,
    HwMonitoringTest.class, SystemStateTest.class, LogTest.class, TopologyTests.class, ReasoningTaskTests.class, 
    StormClusterMonitoringTest.class, ObservationTests.class, 
    CloudEnvironmentTests.class,
    ProfilingTests.class,
    // must be last
    MonitoringConfigurationTests.class})
public class AllTests {
}