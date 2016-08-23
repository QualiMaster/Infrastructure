package tests.eu.qualimaster.monitoring.profiling;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * The test suite for the Monitoring Profiling support. Do not rename this class.
 * 
 * @author Christopher Voges
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    InstantiationTest.class, 
    PerformanceTest.class, 
    QualityTest.class})
public class ProfilingTests {
}