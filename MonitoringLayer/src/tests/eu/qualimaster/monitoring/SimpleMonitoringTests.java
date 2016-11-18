package tests.eu.qualimaster.monitoring;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import tests.eu.qualimaster.coordination.AbstractCoordinationTests;
import tests.eu.qualimaster.coordination.TestNameMapping;
import tests.eu.qualimaster.coordination.Utils;
import tests.eu.qualimaster.storm.Naming;
import eu.qualimaster.common.signal.SignalMechanism;
import eu.qualimaster.coordination.CoordinationManager;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.monitoring.MonitoringManager;
import eu.qualimaster.monitoring.events.AlgorithmChangedMonitoringEvent;
import eu.qualimaster.monitoring.events.AlgorithmMonitoringEvent;
import eu.qualimaster.monitoring.events.FrozenSystemState;
import eu.qualimaster.monitoring.events.PipelineElementMultiObservationMonitoringEvent;
import eu.qualimaster.monitoring.events.PipelineElementObservationMonitoringEvent;
import eu.qualimaster.monitoring.events.PlatformMonitoringEvent;
import eu.qualimaster.monitoring.systemState.NodeImplementationSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineSystemPart;
import eu.qualimaster.monitoring.systemState.SystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.ResourceUsage;
import eu.qualimaster.observables.TimeBehavior;

/**
 * Single monitoring functionality tests without connecting to Storm 
 * or to SPASS-meter.
 */
public class SimpleMonitoringTests {
    
    /**
     * Executed before a single test.
     */
    @Before
    public void setUp() {
        SignalMechanism.setTestMode(true);
        Utils.setModelProvider(Utils.INFRASTRUCTURE_TEST_MODEL_PROVIDER);
        Utils.configure();
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
        MonitoringManager.clearState();
        CoordinationManager.stop();
        EventManager.stop();
        Utils.dispose();
    }

    /**
     * Tests single observations on pipeline elements.
     */
    @Test
    public void testSingleObservation() {
        MonitoringManager.handleEvent(new PipelineElementObservationMonitoringEvent(
            null, null, null, TimeBehavior.LATENCY, 1500));
        SystemState state = MonitoringManager.getSystemState();
        Assert.assertNotNull(state);
        SystemPart platform = state.getPlatform();
        Assert.assertNotNull(platform);
        PipelineSystemPart pPart = state.obtainPipeline(TestNameMapping.PIPELINE_NAME);
        Assert.assertNotNull(pPart);
        PipelineNodeSystemPart sourcePart = pPart.obtainPipelineNode(TestNameMapping.NODE_SOURCE);
        Assert.assertNotNull(sourcePart);
        PipelineNodeSystemPart processPart = pPart.obtainPipelineNode(TestNameMapping.NODE_PROCESS);
        Assert.assertNotNull(processPart);
        PipelineNodeSystemPart sinkPart = pPart.obtainPipelineNode(TestNameMapping.NODE_SINK);
        Assert.assertNotNull(sinkPart);

        // does not cause recording
        assertPart(platform, TimeBehavior.LATENCY, null);
        assertPart(pPart, TimeBehavior.LATENCY, 0);
        assertPart(sourcePart, TimeBehavior.LATENCY, 0);
        assertPart(sourcePart, ResourceUsage.BANDWIDTH, null);
        assertPart(processPart, TimeBehavior.LATENCY, 0);
        assertPart(processPart, ResourceUsage.BANDWIDTH, null);
        assertPart(sinkPart, TimeBehavior.LATENCY, 0);
        assertPart(sinkPart, ResourceUsage.BANDWIDTH, null);

        MonitoringManager.handleEvent(new PipelineElementObservationMonitoringEvent(
            TestNameMapping.PIPELINE_NAME, null, null, TimeBehavior.LATENCY, 1500));
        assertPart(platform, TimeBehavior.LATENCY, null);
        assertPart(pPart, TimeBehavior.LATENCY, 1500);
        assertPart(sourcePart, TimeBehavior.LATENCY, 0);
        assertPart(sourcePart, ResourceUsage.BANDWIDTH, null);
        assertPart(processPart, TimeBehavior.LATENCY, 0);
        assertPart(processPart, ResourceUsage.BANDWIDTH, null);
        assertPart(sinkPart, TimeBehavior.LATENCY, 0);
        assertPart(sinkPart, ResourceUsage.BANDWIDTH, null);

        MonitoringManager.handleEvent(new PipelineElementObservationMonitoringEvent(
            TestNameMapping.PIPELINE_NAME, TestNameMapping.NODE_SOURCE, null, TimeBehavior.LATENCY, 1000));
        assertPart(platform, TimeBehavior.LATENCY, null);
        assertPart(pPart, TimeBehavior.LATENCY, 1500);
        assertPart(sourcePart, TimeBehavior.LATENCY, 1000);
        assertPart(sourcePart, ResourceUsage.BANDWIDTH, null);
        assertPart(processPart, TimeBehavior.LATENCY, 0);
        assertPart(processPart, ResourceUsage.BANDWIDTH, null);
        assertPart(sinkPart, TimeBehavior.LATENCY, 0);
        assertPart(sinkPart, ResourceUsage.BANDWIDTH, null);

        MonitoringManager.handleEvent(new PipelineElementObservationMonitoringEvent(
            TestNameMapping.PIPELINE_NAME, TestNameMapping.NODE_PROCESS, null, TimeBehavior.LATENCY, 1700));
        assertPart(platform, TimeBehavior.LATENCY, null);
        assertPart(pPart, TimeBehavior.LATENCY, 1500);
        assertPart(sourcePart, TimeBehavior.LATENCY, 1000);
        assertPart(sourcePart, ResourceUsage.BANDWIDTH, null);
        assertPart(processPart, TimeBehavior.LATENCY, 1700);
        assertPart(processPart, ResourceUsage.BANDWIDTH, null);
        assertPart(sinkPart, TimeBehavior.LATENCY, 0);
        assertPart(sinkPart, ResourceUsage.BANDWIDTH, null);

        MonitoringManager.handleEvent(new PipelineElementObservationMonitoringEvent(
            TestNameMapping.PIPELINE_NAME, TestNameMapping.NODE_SINK, null, TimeBehavior.LATENCY, 600));
        assertPart(platform, TimeBehavior.LATENCY, null);
        assertPart(pPart, TimeBehavior.LATENCY, 1500);
        assertPart(sourcePart, TimeBehavior.LATENCY, 1000);
        assertPart(sourcePart, ResourceUsage.BANDWIDTH, null);
        assertPart(processPart, TimeBehavior.LATENCY, 1700);
        assertPart(processPart, ResourceUsage.BANDWIDTH, null);
        assertPart(sinkPart, TimeBehavior.LATENCY, 600);
        assertPart(sinkPart, ResourceUsage.BANDWIDTH, null);
    }

    /**
     * Asserts the given observation of a system part.
     * 
     * @param part the system part to be checked
     * @param observable the observable in <code>part</code> to be checked
     * @param value the actual value (may be <b>null</b> in case that no observation is expected due to resource 
     *     assignments)
     */
    private static void assertPart(SystemPart part, IObservable observable, int value) {
        assertPart(part, observable, (double) value);
    }
    
    /**
     * Asserts the given observation of a system part.
     * 
     * @param part the system part to be checked
     * @param observable the observable in <code>part</code> to be checked
     * @param value the actual value (may be <b>null</b> in case that no observation is expected due to resource 
     *     assignments)
     */
    private static void assertPart(SystemPart part, IObservable observable, Double value) {
        Assert.assertNotNull(part);
        boolean observableSupported = part.supportsObservation(observable);
        if (null == value) {
            Assert.assertFalse(observableSupported);
        } else {
            Assert.assertTrue(observableSupported);
            Assert.assertEquals(part.getObservedValue(observable), value.doubleValue(), 0.5);
        }
    }
    
    /**
     * Tests the aggregation on algorithm level, i.e., on an active algorithm.
     */
    @Test
    public void testAlgorithmAggregation() {
        SystemState state = MonitoringManager.getSystemState();
        state.obtainPipeline(TestNameMapping.PIPELINE_NAME).changeStatus(PipelineLifecycleEvent.Status.STARTED, 
            false, null);
        // set current algorithm
        MonitoringManager.handleEvent(new AlgorithmChangedMonitoringEvent(TestNameMapping.PIPELINE_NAME, 
            TestNameMapping.NODE_PROCESS, TestNameMapping.NODE_PROCESS_ALG1));
        // monitor value for pipeline node an and (propagated) for current algorithm
        MonitoringManager.handleEvent(new PipelineElementObservationMonitoringEvent(TestNameMapping.PIPELINE_NAME, 
            TestNameMapping.NODE_PROCESS, null, ResourceUsage.USED_MEMORY, 1500));

        assertAlgorithmAggregation(state);
        assertAlgorithmAggregation(new SystemState(state));
    }
    
    /**
     * Asserts the equality of the given primitive and the wrapper value.
     * 
     * @param expected the expected value
     * @param actual the actual value
     */
    private static void assertEquals(double expected, Double actual) {
        Assert.assertNotNull(actual);
        Assert.assertEquals(expected, actual, 0.01);
    }

    /**
     * Asserts the algorithm aggregation system state.
     * 
     * @param state the state to assert
     */
    private void assertAlgorithmAggregation(SystemState state) {
        Assert.assertNotNull(state);
        PipelineSystemPart pPart = state.obtainPipeline(TestNameMapping.PIPELINE_NAME);
        Assert.assertNotNull(pPart);
        PipelineNodeSystemPart nPart = pPart.obtainPipelineNode(TestNameMapping.NODE_PROCESS);
        assertPart(nPart, ResourceUsage.USED_MEMORY, 1500); // no aggregation at element level at the moment
        NodeImplementationSystemPart aPart = pPart.getAlgorithm(TestNameMapping.NODE_PROCESS_ALG1);
        assertPart(aPart, ResourceUsage.USED_MEMORY, 1500);

        assertSerializable(state);
        
        FrozenSystemState frozen = state.freeze();
        assertEquals(1500, frozen.getPipelineElementObservation(TestNameMapping.PIPELINE_NAME, 
            TestNameMapping.NODE_PROCESS, ResourceUsage.USED_MEMORY));
        assertEquals(1500, frozen.getAlgorithmObservation(TestNameMapping.PIPELINE_NAME, 
            TestNameMapping.NODE_PROCESS_ALG1, ResourceUsage.USED_MEMORY));
        
        assertSerializable(frozen);
    }
    
    /**
     * Tests the (composite) aggregation on platform level.
     */
    @Test
    public void testPlatformAggregation() {
        MonitoringManager.handleEvent(new PlatformMonitoringEvent(ResourceUsage.AVAILABLE_MACHINES, 5, "Storm"));
        MonitoringManager.handleEvent(new PlatformMonitoringEvent(ResourceUsage.AVAILABLE_MACHINES, 4, "Hadoop"));
        MonitoringManager.handleEvent(new PlatformMonitoringEvent(ResourceUsage.AVAILABLE_MACHINES, 2, "Maxeler"));
        MonitoringManager.handleEvent(new PlatformMonitoringEvent(ResourceUsage.AVAILABLE_DFES, 2, "Maxeler"));

        SystemState state = MonitoringManager.getSystemState();
        assertPlatformAggregation(state);
        assertPlatformAggregation(new SystemState(state)); // also on copy
    }

    /**
     * Asserts the platform aggregation state.
     * 
     * @param state the state to assert
     */
    private void assertPlatformAggregation(SystemState state) {
        Assert.assertNotNull(state);
        SystemPart platform = state.getPlatform();
        Assert.assertNotNull(platform);
        
        assertPart(platform, ResourceUsage.AVAILABLE_MACHINES, 11);
        assertPart(platform, ResourceUsage.AVAILABLE_DFES, 2);

        assertSerializable(state);

        FrozenSystemState frozen = state.freeze();
        assertEquals(11, frozen.getInfrastructureObservation(ResourceUsage.AVAILABLE_MACHINES));
        assertEquals(2, frozen.getInfrastructureObservation(ResourceUsage.AVAILABLE_DFES));
        
        assertSerializable(frozen);
    }
    
    /**
     * Tests the (composite) aggregation on platform level via the event manager.
     * 
     * @see #assertPlatformAggregation(SystemState)
     */
    @Test
    public void testPlatformAggregationViaEventManager() {
        EventManager.handle(new PlatformMonitoringEvent(ResourceUsage.USED_MACHINES, 5, "Storm"));
        EventManager.handle(new PlatformMonitoringEvent(ResourceUsage.USED_MACHINES, 4, "Hadoop"));
        EventManager.handle(new PlatformMonitoringEvent(ResourceUsage.USED_MACHINES, 2, "Maxeler"));
        EventManager.handle(new PlatformMonitoringEvent(ResourceUsage.USED_DFES, 2, "Maxeler"));

        EventManager.cleanup();
        
        SystemState state = MonitoringManager.getSystemState();
        assertPlatformAggregationEvt(state);
        assertPlatformAggregationEvt(new SystemState(state)); // also on copy
    }
    
    /**
     * Asserts the platform aggregation state.
     * 
     * @param state the state to assert
     */
    private void assertPlatformAggregationEvt(SystemState state) {
        Assert.assertNotNull(state);
        SystemPart platform = state.getPlatform();
        Assert.assertNotNull(platform);
        
        assertPart(platform, ResourceUsage.USED_MACHINES, 11);
        assertPart(platform, ResourceUsage.USED_DFES, 2);
        
        assertSerializable(state);

        FrozenSystemState frozen = state.freeze();
        assertEquals(11, frozen.getInfrastructureObservation(ResourceUsage.USED_MACHINES));
        assertEquals(2, frozen.getInfrastructureObservation(ResourceUsage.USED_DFES));
        
        assertSerializable(frozen);
    }
    
    /**
     * Tests single observations on pipeline elements.
     * 
     * @see #assertMultiObservation(SystemState)
     */
    @Test
    public void testMultiObservation() {
        SystemState state = MonitoringManager.getSystemState();
        state.obtainPipeline(TestNameMapping.PIPELINE_NAME).changeStatus(PipelineLifecycleEvent.Status.STARTED, 
            false, null);
        
        Map<IObservable, Double> observations = new HashMap<IObservable, Double>();
        observations.put(TimeBehavior.LATENCY, 600.0);
        observations.put(TimeBehavior.ENACTMENT_DELAY, 200.0);
        MonitoringManager.handleEvent(new PipelineElementMultiObservationMonitoringEvent(
            TestNameMapping.PIPELINE_NAME, TestNameMapping.NODE_PROCESS, null, observations));

        assertMultiObservation(state);
        assertMultiObservation(new SystemState(state)); // also on copy
    }
    
    /**
     * Implements the assertions of {@link #testMultiObservation()}.
     * 
     * @param state the state to assert
     */
    private void assertMultiObservation(SystemState state) {
        Assert.assertNotNull(state);
        SystemPart platform = state.getPlatform();
        Assert.assertNotNull(platform);
        PipelineSystemPart pPart = state.obtainPipeline(TestNameMapping.PIPELINE_NAME);
        Assert.assertNotNull(pPart);
        PipelineNodeSystemPart sourcePart = pPart.obtainPipelineNode(TestNameMapping.NODE_SOURCE);
        Assert.assertNotNull(sourcePart);
        PipelineNodeSystemPart processPart = pPart.obtainPipelineNode(TestNameMapping.NODE_PROCESS);
        Assert.assertNotNull(processPart);
        PipelineNodeSystemPart sinkPart = pPart.obtainPipelineNode(TestNameMapping.NODE_SINK);
        Assert.assertNotNull(sinkPart);
        
        assertPart(platform, TimeBehavior.LATENCY, null);
        assertPart(pPart, TimeBehavior.LATENCY, 0);
        assertPart(sourcePart, TimeBehavior.LATENCY, 0);
        assertPart(sourcePart, ResourceUsage.BANDWIDTH, null);
        assertPart(sourcePart, TimeBehavior.ENACTMENT_DELAY, 0);
        assertPart(processPart, TimeBehavior.LATENCY, 600);
        assertPart(processPart, TimeBehavior.ENACTMENT_DELAY, 200);
        assertPart(processPart, ResourceUsage.BANDWIDTH, null);
        assertPart(sinkPart, TimeBehavior.LATENCY, 0);
        assertPart(sinkPart, ResourceUsage.BANDWIDTH, null);
        assertPart(sinkPart, TimeBehavior.ENACTMENT_DELAY, 0);

        assertSerializable(state);
        
        FrozenSystemState frozen = state.freeze();
        assertEquals(0, frozen.getPipelineObservation(TestNameMapping.PIPELINE_NAME, TimeBehavior.LATENCY, 0.0));
        assertEquals(0, frozen.getPipelineElementObservation(TestNameMapping.PIPELINE_NAME, 
            TestNameMapping.NODE_SOURCE, TimeBehavior.LATENCY, 0.0));
        assertEquals(0, frozen.getPipelineElementObservation(TestNameMapping.PIPELINE_NAME, 
            TestNameMapping.NODE_SOURCE, TimeBehavior.ENACTMENT_DELAY, 0.0));        
        assertEquals(600, frozen.getPipelineElementObservation(TestNameMapping.PIPELINE_NAME, 
            TestNameMapping.NODE_PROCESS, TimeBehavior.LATENCY));
        assertEquals(200, frozen.getPipelineElementObservation(TestNameMapping.PIPELINE_NAME, 
            TestNameMapping.NODE_PROCESS, TimeBehavior.ENACTMENT_DELAY));        
        assertEquals(0, frozen.getPipelineElementObservation(TestNameMapping.PIPELINE_NAME, 
            TestNameMapping.NODE_SINK, TimeBehavior.LATENCY, 0.0));
        assertEquals(0, frozen.getPipelineElementObservation(TestNameMapping.PIPELINE_NAME, 
            TestNameMapping.NODE_SINK, TimeBehavior.ENACTMENT_DELAY, 0.0));
        
        assertSerializable(frozen);
    }
    
    /**
     * Tests a resource event handling (SPASS-meter).
     */
    @Test
    public void testResourceMeasurement() {
        SystemState state = MonitoringManager.getSystemState();
        state.obtainPipeline(Naming.PIPELINE_NAME).changeStatus(PipelineLifecycleEvent.Status.STARTED, false, null);
        
        EventManager.send(new AlgorithmMonitoringEvent(Naming.PIPELINE_NAME, Naming.NODE_PROCESS_ALG1_CLASS, 
            ResourceUsage.USED_MEMORY, 1000));
        EventManager.cleanup(); // force execution
        
        PipelineSystemPart pip = state.obtainPipeline(Naming.PIPELINE_NAME);
        Assert.assertNotNull(pip);
        NodeImplementationSystemPart alg = pip.getAlgorithm(Naming.NODE_PROCESS_ALG1);
        assertPart(alg, ResourceUsage.USED_MEMORY, 1000);
    }
    
    /**
     * Asserts that the given object is serializable.
     * 
     * @param object the state to serialize
     */
    private void assertSerializable(Object object) {
        try {
            ByteArrayOutputStream tmp = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(tmp);
            out.writeObject(object);
            out.close();
        } catch (IOException e) {
            Assert.fail("unexpected exception " + e.getMessage());
        }
    }
    
    /**
     * Tests obtaining a model artifact.
     */
    @Test
    public void testRegistryConnectorModels() {
        AbstractCoordinationTests.testLoadModels();
    }

}
