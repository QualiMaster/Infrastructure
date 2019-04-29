package tests.eu.qualimaster;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import tests.eu.qualimaster.events.ComponentKeyTests;
import tests.eu.qualimaster.events.EventHandlerTests;
import tests.eu.qualimaster.events.EventManagerTests;
import tests.eu.qualimaster.events.EventsTests;
import tests.eu.qualimaster.events.ForwardTests;
import tests.eu.qualimaster.events.PipelineStatusTrackerTest;
import tests.eu.qualimaster.events.RemoteHandlerTests;

/**
 * The test suite for the event manager/basic layer.
 * 
 * @author Holger Eichelberger
 */
@RunWith(Suite.class)
//beware of the sequence
@Suite.SuiteClasses({ConfigurationTests.class, EventHandlerTests.class, EventsTests.class, RemoteHandlerTests.class, 
    PipelineStatusTrackerTest.class, ForwardTests.class, PipelineOptionsTest.class, FrozenSystemStateTest.class, 
    ComponentKeyTests.class, AlgorithmChangeParameterTest.class, ResponseStoreTest.class, AdditionalTests.class, 
    EventManagerTests.class, FileTests.class, tests.eu.qualimaster.plugins.AllTests.class, 
    tests.eu.qualimaster.reflection.ReflectionHelperTests.class })
public class AllTests {

}
