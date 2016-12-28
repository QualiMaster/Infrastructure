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
package tests.eu.qualimaster.monitoring;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import eu.qualimaster.coordination.CoordinationManager;
import eu.qualimaster.coordination.INameMapping;
import eu.qualimaster.coordination.IdentityMapping;
import eu.qualimaster.monitoring.MonitoringConfiguration;
import eu.qualimaster.monitoring.events.ComponentKey;
import eu.qualimaster.monitoring.events.FrozenSystemState;
import eu.qualimaster.monitoring.parts.PartType;
import eu.qualimaster.monitoring.systemState.AlgorithmParameter;
import eu.qualimaster.monitoring.systemState.NodeImplementationSystemPart;
import eu.qualimaster.monitoring.systemState.HwNodeSystemPart;
import eu.qualimaster.monitoring.systemState.MachineSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineSystemPart;
import eu.qualimaster.monitoring.systemState.PlatformSystemPart;
import eu.qualimaster.monitoring.systemState.SystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.monitoring.tracing.IParameterProvider;
import eu.qualimaster.monitoring.tracing.Tracing;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.ResourceUsage;
import eu.qualimaster.observables.Scalability;
import eu.qualimaster.observables.TimeBehavior;

/**
 * Some system state tests.
 * 
 * @author Holger Eichelberger
 */
public class SystemStateTest {

    private static final String PIPELINE_NAME = "pip";
    private static final String PROCESSOR_NODE_NAME = "processor";
    private SystemState state;
    private PipelineSystemPart pip;
    private PipelineNodeSystemPart node;
    private INameMapping mapping;

    /**
     * Executed before a single test.
     */
    @Before
    public void setUp() {
        mapping = new IdentityMapping(PIPELINE_NAME);
        CoordinationManager.registerTestMapping(mapping);
        state = new SystemState();
        pip = state.obtainPipeline(PIPELINE_NAME);
        node = pip.obtainPipelineNode(PROCESSOR_NODE_NAME);
    }
    
    /**
     * Executed after a single test.
     */
    @After
    public void tearDown() {
        CoordinationManager.unregisterNameMapping(mapping);
        pip = null;
        node = null;
        state.clear();
        state = null;
    }
    
    /**
     * Tests the executor/task aggregation for the same thread.
     */
    @Test
    public void aggregateTasksInSameThreadTest() {
        setUsage(node, "localhost", 1024, 1, 25);
        assertEquals(1, node, ResourceUsage.EXECUTORS);
        assertEquals(1, node, ResourceUsage.TASKS);
        assertEquals(1, node, ResourceUsage.HOSTS);
        setUsage(node, "localhost", 1024, 2, 25);
        assertEquals(1, node, ResourceUsage.EXECUTORS);
        assertEquals(2, node, ResourceUsage.TASKS);
        assertEquals(1, node, ResourceUsage.HOSTS);
    }
    
    /**
     * Tests the executor/task aggregation for the different threads.
     */
    @Test
    public void aggregateTasksInDifferentThreadsTest() {
        setUsage(node, "localhost", 1024, 1, 25);
        assertEquals(1, node, ResourceUsage.EXECUTORS);
        assertEquals(1, node, ResourceUsage.TASKS);
        assertEquals(1, node, ResourceUsage.HOSTS);
        setUsage(node, "localhost", 1024, 2, 26);
        assertEquals(2, node, ResourceUsage.EXECUTORS);
        assertEquals(2, node, ResourceUsage.TASKS);
        assertEquals(1, node, ResourceUsage.HOSTS);
    }
    
    /**
     * Tests the executor/task aggregation for the different workers.
     */
    @Test
    public void aggregateTasksInDifferentWorkersTest() {
        setUsage(node, "localhost", 1024, 1, 25);
        assertEquals(1, node, ResourceUsage.EXECUTORS);
        assertEquals(1, node, ResourceUsage.TASKS);
        assertEquals(1, node, ResourceUsage.HOSTS);
        setUsage(node, "localhost", 1025, 2, 25);
        assertEquals(2, node, ResourceUsage.EXECUTORS);
        assertEquals(2, node, ResourceUsage.TASKS);
        assertEquals(1, node, ResourceUsage.HOSTS);
    }

    /**
     * Tests the executor/task aggregation for the different workers.
     */
    @Test
    public void aggregateTasksInDifferentHostsTest() {
        setUsage(node, "localhost", 1024, 1, 25);
        assertEquals(1, node, ResourceUsage.EXECUTORS);
        assertEquals(1, node, ResourceUsage.TASKS);
        assertEquals(1, node, ResourceUsage.HOSTS);
        setUsage(node, "localhost1", 1024, 2, 25);
        assertEquals(2, node, ResourceUsage.EXECUTORS);
        assertEquals(2, node, ResourceUsage.TASKS);
        assertEquals(2, node, ResourceUsage.HOSTS);
    }

    /**
     * Test changes in parallelization.
     */
    @Test
    public void changeParallelizationTest() {
        // two tasks in the same executor
        setUsage(node, "localhost", 1024, 1, 25);
        setUsage(node, "localhost", 1024, 2, 25);
        assertEquals(1, node, ResourceUsage.EXECUTORS);
        assertEquals(2, node, ResourceUsage.TASKS);
        assertEquals(1, node, ResourceUsage.HOSTS);
        
        // migrate to executor
        setUsage(node, "localhost", 1024, 2, 26);
        assertEquals(2, node, ResourceUsage.EXECUTORS);
        assertEquals(2, node, ResourceUsage.TASKS);
        assertEquals(1, node, ResourceUsage.HOSTS);

        // migrate to host
        setUsage(node, "localhost1", 1024, 2, 15);
        assertEquals(2, node, ResourceUsage.EXECUTORS);
        assertEquals(2, node, ResourceUsage.TASKS);
        assertEquals(2, node, ResourceUsage.HOSTS);
        
        // back to original host
        setUsage(node, "localhost", 1024, 2, 27);
        assertEquals(2, node, ResourceUsage.EXECUTORS);
        assertEquals(2, node, ResourceUsage.TASKS);
        assertEquals(1, node, ResourceUsage.HOSTS);

        // back to executor
        setUsage(node, "localhost", 1024, 2, 25);
        assertEquals(1, node, ResourceUsage.EXECUTORS);
        assertEquals(2, node, ResourceUsage.TASKS);
        assertEquals(1, node, ResourceUsage.HOSTS);
    }

    /**
     * Sets the usage of 1 executor and 1 task (default in monitoring) for
     * the given <code>node</code> and the given machine/thread settings.
     * 
     * @param node the pipeline node
     * @param host the host name
     * @param port the port number
     * @param taskId the task id
     * @param threadId the optional thread id (may be <b>null</b> if not used)
     */
    private void setUsage(PipelineNodeSystemPart node, String host, int port, int taskId, 
        Integer threadId) {
        ComponentKey key = new ComponentKey(host, port, taskId);
        if (null != threadId) {
            key.setThreadId(threadId);
        }
        node.setValue(ResourceUsage.EXECUTORS, 1.0, key);
        node.setValue(ResourceUsage.TASKS, 1.0, key);
    }
    
    /**
     * Asserts the observed value for <code>observable</code> in <code>part</code>.
     * 
     * @param expected the expected value
     * @param part the system part to look into
     * @param observable the observable to assert the value for
     */
    public static void assertEquals(int expected, SystemPart part, IObservable observable) {
        Double actual = part.getObservedValue(observable);
        Assert.assertNotNull(actual);
        Assert.assertEquals(expected, actual, 0.005);
    }
    
    /**
     * Asserts the observed value for <code>observable</code> in <code>part</code>.
     * 
     * @param expected the expected value
     * @param part the system part to look into
     * @param observable the observable to assert the value for
     */
    public static void assertEquals(double expected, SystemPart part, IObservable observable) {
        assertEquals(expected, part, observable, 0.005);
    }
    
    /**
     * Asserts the observed value for <code>observable</code> in <code>part</code>.
     * 
     * @param expected the expected value
     * @param part the system part to look into
     * @param observable the observable to assert the value for
     * @param precision the precision to compare against
     */
    public static void assertEquals(double expected, SystemPart part, IObservable observable, double precision) {
        Double actual = part.getObservedValue(observable);
        Assert.assertNotNull(actual);
        Assert.assertEquals(expected, actual, precision);
    }
    
    /**
     * Tests the available boolean flag.
     */
    @Test
    public void hwAvailableTest() {
        PlatformSystemPart platform = state.getPlatform();
        MachineSystemPart machine = platform.obtainMachine("machine");
        machine.setValue(ResourceUsage.AVAILABLE, 1.0, null);
        MachineSystemPart machine1 = platform.obtainMachine("machine1");
        HwNodeSystemPart hw = platform.obtainHwNode("hw");
        hw.setValue(ResourceUsage.AVAILABLE, 1.0, null);
        HwNodeSystemPart hw1 = platform.obtainHwNode("hw1");
        
        FrozenSystemState frozen = state.freeze();
        assertEquals(true, machine, frozen, ResourceUsage.AVAILABLE);
        assertEquals(false, machine1, frozen, ResourceUsage.AVAILABLE);
        assertEquals(true, hw, frozen, ResourceUsage.AVAILABLE);
        assertEquals(false, hw1, frozen, ResourceUsage.AVAILABLE);
    }
    
    /**
     * Asserts the equality of the given <code>expected</code> boolean value for <code>part</code> and 
     * <code>frozen</code> on <code>observable</code>.
     * 
     * @param expected the expected value
     * @param part the system part to test
     * @param frozen the frozen system state to test
     * @param observable the observable to test the value for
     */
    public static void assertEquals(boolean expected, SystemPart part, FrozenSystemState frozen, 
        IObservable observable) {
        double expectedVal = expected ? 1.0 : 0.0; 
        
        Double obs = part.getObservedValue(observable);
        Assert.assertNotNull(obs);
        Assert.assertEquals(expectedVal, obs, 0.05);
        if (PartType.CLUSTER == part.getType()) {
            obs = frozen.getHwNodeObservation(part.getName(), ResourceUsage.AVAILABLE, 0.0);
        } else if (PartType.MACHINE == part.getType()) {
            obs = frozen.getMachineObservation(part.getName(), ResourceUsage.AVAILABLE, 0.0);
        } else {
            obs = null;
        }
        Assert.assertNotNull(obs);
        Assert.assertEquals(expectedVal, obs, 0.05);
    }
    
    /**
     * Asserts equality of two {@link SystemPart system parts}.
     * 
     * @param expected the expected system part
     * @param actual the actual system part to be asserted
     */
    public static void assertEqualSystemParts(SystemPart expected, SystemPart actual) {
        Assert.assertNotNull(actual);
        Assert.assertEquals(expected.getName(), actual.getName());
        for (IObservable obs : expected.observables()) {
            Assert.assertTrue(actual.supportsObservation(obs));
            Assert.assertEquals(expected.getObservedValue(obs), actual.getObservedValue(obs), 0.005D);
        }
    }

    /**
     * Asserts equality of two {@link PipelineSystemPart pipeline system parts}.
     * 
     * @param expected the expected pipeline system part
     * @param actual the actual pipeline system part to be asserted
     */
    public static void assertEquals(PipelineSystemPart expected, PipelineSystemPart actual) {
        assertEqualSystemParts(expected, actual);
        for (PipelineNodeSystemPart node : expected.getNodes()) {
            PipelineNodeSystemPart aNode = actual.getNode(node.getName());
            assertEquals(node, aNode);
        }
        for (NodeImplementationSystemPart alg : expected.algorithms()) {
            NodeImplementationSystemPart aAlg = actual.getAlgorithm(alg.getName());
            assertEqualSystemParts(alg, aAlg);
        }
    }

    /**
     * Asserts equality of two {@link PipelineNodeSystemPart pipeline node system parts}.
     * 
     * @param expected the expected pipeline node system part
     * @param actual the actual pipeline node system part to be asserted
     */
    public static void assertEquals(PipelineNodeSystemPart expected, PipelineNodeSystemPart actual) {
        assertEqualSystemParts(expected, actual);
        NodeImplementationSystemPart current = expected.getCurrent();
        NodeImplementationSystemPart aCurrent = actual.getCurrent();
        if (null == current) {
            Assert.assertNull(aCurrent);
        } else {
            Assert.assertEquals(current, aCurrent);
        }
    }
    
    /**
     * Asserts equality of two {@link PlatformSystemPart platform system parts}.
     * 
     * @param expected the expected platform system part
     * @param actual the actual platform system part to be asserted
     */
    public static void assertEquals(PlatformSystemPart expected, PlatformSystemPart actual) {
        assertEqualSystemParts(expected, actual);
        for (MachineSystemPart part : expected.machines()) {
            MachineSystemPart aPart = actual.getMachine(part.getName());
            assertEqualSystemParts(part, aPart);
        }
        for (HwNodeSystemPart part : expected.hwNodes()) {
            HwNodeSystemPart aPart = actual.getHwNode(part.getName());
            assertEqualSystemParts(part, aPart);
        }
    }

    /**
     * Tests state copying.
     */
    @Test
    public void testStateCopy() {
        SystemState state = new SystemState(); // create trace
        PipelineSystemPart pip = state.obtainPipeline(PIPELINE_NAME);
        pip.setValue(ResourceUsage.CAPACITY, 0.5, null);
        PipelineNodeSystemPart node = pip.obtainPipelineNode(PROCESSOR_NODE_NAME);
        setUsage(node, "localhost1", 1024, 1, 25);
        node.setValue(ResourceUsage.CAPACITY, 0.5, null);

        SystemState copy = new SystemState(state);
        // trace must be the same
        Assert.assertTrue(state.getPlatform().getTrace() == copy.getPlatform().getTrace());
        PipelineSystemPart cPip = copy.getPipeline(PIPELINE_NAME);
        assertEquals(state.getPlatform(), copy.getPlatform());
        assertEquals(pip, cPip);
    }
    
    /**
     * Tests infrastructure tracing.
     */
    @Test
    public void testInfrastructureTrace() {
        IParameterProvider pp = new IParameterProvider() {

            @Override
            public Map<String, List<AlgorithmParameter>> getAlgorithmParameters() {
                return null;
            }
            
        };
        String old = MonitoringConfiguration.getMonitoringLogInfraLocation();
        Properties prop = new Properties();
        File dir = new File(System.getProperty("java.io.tmpdir"));
        prop.setProperty(MonitoringConfiguration.MONITORING_LOG_INFRA_LOCATION, dir.getAbsolutePath());
        MonitoringConfiguration.configure(prop);
        boolean oldTraceTesting = Tracing.inTesting(true);
        PlatformSystemPart.resetTrace();
        
        SystemState state = new SystemState(); // create trace
        PipelineSystemPart pip = state.obtainPipeline(PIPELINE_NAME);
        pip.setValue(ResourceUsage.CAPACITY, 0.5, null);
        PipelineNodeSystemPart node = pip.obtainPipelineNode(PROCESSOR_NODE_NAME);
        setUsage(node, "localhost1", 1024, 1, 25);
        node.setValue(ResourceUsage.CAPACITY, 0.5, null);
        
        Tracing.traceInfrastructure(state, pp);
        state.closePlatformTrace();
        
        File file = new File(dir, Tracing.PREFIX_INFA + Tracing.TRACE_FILE_SUFFIX);
        Assert.assertTrue(file.exists());
        // read file contents??
        
        prop.setProperty(MonitoringConfiguration.MONITORING_LOG_INFRA_LOCATION, old);
        MonitoringConfiguration.configure(prop);
        Tracing.inTesting(oldTraceTesting);
    }

    /**
     * A test for clearing observations. Timing is driven by a real test case, which indicated
     * that the time frame delegating observable does not really clear.
     * 
     * @throws InterruptedException shall not occur
     */
    @Test
    public void clearingTest() throws InterruptedException {
        node.clear(TimeBehavior.THROUGHPUT_ITEMS); // reset firstUpdate!
        node.clear(Scalability.ITEMS);
        node.setValue(TimeBehavior.THROUGHPUT_ITEMS, 750, null); // incremental - 0 change, frame 1000
        Thread.sleep(3000);
        Assert.assertEquals(750 / 3, node.getObservedValue(Scalability.ITEMS), 5);
        
        node.clear(TimeBehavior.THROUGHPUT_ITEMS); // reset firstUpdate!
        node.clear(Scalability.ITEMS);
        node.setValue(TimeBehavior.THROUGHPUT_ITEMS, 1000, null);
        Thread.sleep(3000);
        Assert.assertEquals(1000 / 3, node.getObservedValue(Scalability.ITEMS), 5);
    }

}
