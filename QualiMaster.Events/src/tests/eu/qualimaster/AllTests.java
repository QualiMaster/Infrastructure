package tests.eu.qualimaster;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import tests.eu.qualimaster.events.ComponentKeyTests;
import tests.eu.qualimaster.events.EventHandlerTests;
import tests.eu.qualimaster.events.EventsTests;
import tests.eu.qualimaster.events.ForwardTests;
import tests.eu.qualimaster.events.PipelineStatusTrackerTest;
import tests.eu.qualimaster.events.RemoteHandlerTests;

/**
 * The test suite for the event handler.
 * 
 * @author Holger Eichelberger
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ConfigurationTests.class, EventHandlerTests.class, EventsTests.class, RemoteHandlerTests.class, 
    PipelineStatusTrackerTest.class, ForwardTests.class, PipelineOptionsTest.class, FrozenSystemStateTest.class, 
    ComponentKeyTests.class, AlgorithmChangeParameterTest.class, ResponseStoreTest.class, AdditionalTests.class })
public class AllTests {

}
