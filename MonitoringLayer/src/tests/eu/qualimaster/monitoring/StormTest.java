package tests.eu.qualimaster.monitoring;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Assert;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import tests.eu.qualimaster.coordination.AbstractCoordinationTests;
import tests.eu.qualimaster.coordination.IntSerializer;
import tests.eu.qualimaster.coordination.LocalStormEnvironment;
import tests.eu.qualimaster.coordination.Utils;
import tests.eu.qualimaster.monitoring.genTopo.AbstractTopology;
import tests.eu.qualimaster.monitoring.genTopo.GenTopology;
import tests.eu.qualimaster.monitoring.genTopo.HwTopology;
import tests.eu.qualimaster.monitoring.genTopo.HwTopologyInt;
import tests.eu.qualimaster.monitoring.genTopo.ManTopology;
import tests.eu.qualimaster.monitoring.genTopo.SwitchTopology;
import tests.eu.qualimaster.storm.Naming;
import tests.eu.qualimaster.storm.TestTopology;
import tests.eu.qualimaster.storm.Topology;
import eu.qualimaster.easy.extension.internal.AlgorithmProfileHelper.ProfileData;
import eu.qualimaster.base.pipeline.RecordingTopologyBuilder;
import eu.qualimaster.common.signal.ThriftConnection;
import eu.qualimaster.coordination.CoordinationConfiguration;
import eu.qualimaster.coordination.INameMapping;
import eu.qualimaster.coordination.StormUtils;
import eu.qualimaster.coordination.StormUtils.TopologyTestInfo;
import eu.qualimaster.coordination.commands.PipelineCommand;
import eu.qualimaster.coordination.commands.ProfileAlgorithmCommand;
import eu.qualimaster.coordination.events.AlgorithmProfilingEvent.DetailMode;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent.Status;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.infrastructure.PipelineOptions;
import eu.qualimaster.monitoring.MonitoringConfiguration;
import eu.qualimaster.monitoring.MonitoringManager;
import eu.qualimaster.monitoring.events.FrozenSystemState;
import eu.qualimaster.monitoring.parts.PartType;
import eu.qualimaster.monitoring.systemState.PipelineSystemPart;
import eu.qualimaster.monitoring.systemState.SystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.observables.FunctionalSuitability;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.ResourceUsage;
import eu.qualimaster.observables.Scalability;
import eu.qualimaster.observables.TimeBehavior;
import backtype.storm.generated.StormTopology;

/**
 * Tests the storm monitoring via thrift. Set environment variable 
 * "STORM_TEST_TIMEOUT_MS" to a value greater than 15.000 (ms).
 * 
 * @author Holger Eichelberger
 */
public class StormTest extends AbstractCoordinationTests {

    /**
     * Executed before a single test.
     */
    @Before
    public void setUp() {
        Utils.setModelProvider(Utils.INFRASTRUCTURE_TEST_MODEL_PROVIDER);
        Utils.configure();
        super.setUp();
        //CoordinationManager.registerTestMapping(TestNameMapping.INSTANCE);
        //Properties prop = new Properties();
        //prop.put(MonitoringConfiguration.THRIFT_MONITORING_DEBUG, true);
        //MonitoringConfiguration.configure(prop, false);
        MonitoringManager.start(); // with defaults
        
        //enableTracing();
    }
    
    /**
     * Enables algorithm / pipeline tracing for tests.
     */
    protected void enableTracing() {
        Properties prop = new Properties();
        File file = new File(System.getProperty("java.io.tmp"), "qm");
        System.out.println("MONITORING LOGGING FOLDER " + file.getAbsolutePath());        
        file.mkdirs();
        prop.put(MonitoringConfiguration.MONITORING_LOG_LOCATION, file.getAbsolutePath());
        MonitoringConfiguration.configure(prop, false);
        MonitoringManager.getSystemState().enableAlgorithmTracing(true);
    }
    
    /**
     * Executed after a single test.
     */
    @After
    public void tearDown() {
        MonitoringManager.stop();
        MonitoringManager.clearState();
        super.tearDown();
        Utils.dispose();
    }
    
    /**
     * Tests a topology.
     */
    @Test
    public void testBasicTopology() {
        LocalStormEnvironment env = new LocalStormEnvironment();

        @SuppressWarnings("rawtypes")
        Map topoCfg = createTopologyConfiguration();
        // different sequence due to test
        PipelineOptions opt = new PipelineOptions();
        //opt.setExecutorParallelism(Naming.NODE_PROCESS, 3);
        RecordingTopologyBuilder builder = new RecordingTopologyBuilder(opt);
        Topology.createTopology(builder);
        builder.close(Naming.PIPELINE_NAME, topoCfg);
        StormTopology topology = builder.createTopology();
        
        Map<String, TopologyTestInfo> topologies = new HashMap<String, TopologyTestInfo>();
        topologies.put(Naming.PIPELINE_NAME, new TopologyTestInfo(topology, 
            new File(Utils.getTestdataDir(), "pipeline.xml"), topoCfg));
        env.setTopologies(topologies);
        new PipelineCommand(Naming.PIPELINE_NAME, PipelineCommand.Status.START).execute();
        sleep(500);
        EventManager.send(new PipelineLifecycleEvent(Naming.PIPELINE_NAME, 
            PipelineLifecycleEvent.Status.CHECKED, null)); // fake as we have no adaptation layer started
        getPipelineStatusTracker().waitFor(Naming.PIPELINE_NAME, Status.STARTED, 30000);

        sleep(5000);
        
        EventManager.cleanup();
        // get system state here as otherwise stopping the pipeline may accidentally clear the dynamic part
        // of the state
        SystemState state = new SystemState(MonitoringManager.getSystemState());
        FrozenSystemState frozenState = state.freeze();
        
        new PipelineCommand(Naming.PIPELINE_NAME, PipelineCommand.Status.STOP).execute();
        sleep(2000);
        env.shutdown();
        ThriftConnection.setLocalCluster(null);
        StormUtils.forTesting(null, null);

        SystemPart platform = state.getPlatform();
        Assert.assertNotNull(platform);

        PipelineSystemPart pPart = state.obtainPipeline(Naming.PIPELINE_NAME);
        Assert.assertNotNull(pPart);
        
        System.out.println(platform); // for toString
        System.out.println(pPart);
        System.out.println(pPart.obtainPipelineNode(Naming.NODE_SOURCE));
        System.out.println(pPart.obtainPipelineNode(Naming.NODE_PROCESS));
        System.out.println(pPart.getAlgorithm(Naming.NODE_PROCESS_ALG1));
        System.out.println(pPart.obtainPipelineNode(Naming.NODE_SINK));
        System.out.println(frozenState);

        assertSystemPart(pPart.obtainPipelineNode(Naming.NODE_SOURCE));
        assertSystemPart(pPart.obtainPipelineNode(Naming.NODE_PROCESS));
        assertSystemPart(pPart.obtainPipelineNode(Naming.NODE_SINK));
        // alg2 does not become active in this test
        assertSystemPart(pPart.getAlgorithm(Naming.NODE_PROCESS_ALG2)); 
        
        Assert.assertEquals(1, platform.getObservedValue(ResourceUsage.AVAILABLE_MACHINES), 0.5);
        System.out.println(state.format());
        env.cleanup();
    }
    
    /**
     * Asserts measured / not measured observables of a pipeline part.
     * 
     * @param part the pipeline part
     */
    private static void assertSystemPart(SystemPart part) {
        Assert.assertNotNull(part);
        
        // currently not monitored / tested
        assertNotMeasured(part, ResourceUsage.USED_MEMORY); // this needs SPASS-meter
        assertNotMeasured(part, FunctionalSuitability.ACCURACY_CONFIDENCE);
        assertNotMeasured(part, Scalability.VOLUME);

        assertNotMeasured(part, FunctionalSuitability.COMPLETENESS);
        assertNotMeasured(part, FunctionalSuitability.BELIEVABILITY);
        assertNotMeasured(part, FunctionalSuitability.RELEVANCY);
        assertNotMeasured(part, Scalability.VARIETY);
        assertNotMeasured(part, Scalability.VELOCITY);
        if (PartType.ALGORITHM != part.getType()) {
            assertNotMeasured(part, Scalability.VOLATILITY);
            //assertMeasured(part, Scalability.ITEMS);
        }
        //assertNotMeasured(part, TimeBehavior.THROUGHPUT_VOLUME);
        //assertMeasured(part, TimeBehavior.LATENCY);
        assertMeasured(part, TimeBehavior.THROUGHPUT_ITEMS); // should be around number of items
    }

    /**
     * Asserts that the given <code>observable</code> was measured on <code>part</code> with 
     * a value greater than <code>0</code>.
     * 
     * @param part the system part
     * @param observable the observable
     */
    private static void assertMeasured(SystemPart part, IObservable observable) {
        assertMeasured(part, observable, false);
    }

    /**
     * Asserts that the given <code>observable</code> was measured on <code>part</code>.
     * 
     * @param part the system part
     * @param observable the observable
     * @param mayBeEquals whether the value must be greater than <code>0</code> (<code>false</code>) or whether it 
     *   may be <code>0</code> (<code>true</code>)
     */
    private static void assertMeasured(SystemPart part, IObservable observable, boolean mayBeEquals) {
        Assert.assertTrue("no value for " + part.getName() + " " + observable + " " 
            + part.getObservedValue(observable), part.hasValue(observable));
        if (mayBeEquals) {
            Assert.assertTrue(part.getObservedValue(observable) >= 0);
        } else {
            Assert.assertTrue("no measured value for " + part.getName() + " " + observable, 
                part.getObservedValue(observable) > 0);
        }
    }

    /**
     * Asserts that the given <code>observable</code> was not measured on <code>part</code>.
     * 
     * @param part the system part
     * @param observable the observable
     */
    private static void assertNotMeasured(SystemPart part, IObservable observable) {
        Assert.assertTrue(part.supportsObservation(observable) && !part.hasValue(observable));
    }
    
    /**
     * Tests a topology emulating a generated sub-topology.
     */
    @Test
    public void testGenTopology() {
        testTopology(new GenTopology());
    }
    
    /**
     * Tests a topology emulating a manual sub-topology.
     */
    @Test
    public void testManTopology() {
        testTopology(new ManTopology());
    }

    /**
     * Tests a topology emulating a generated tightly integrated hardware sub-topology.
     */
    @Test
    public void testHwTopology() {
        testTopology(new HwTopology());
    }
    
    /**
     * Tests a topology emulating a generated loosely hardware sub-topology (with sink).
     */
    @Test
    public void testHwTopologyIntegrated() {
        if (!isJenkins()) {
            testTopology(new HwTopologyInt(true));
        }
    }

    /**
     * Tests a topology emulating a generated hardware sub-topology (with sink).
     */
    @Test
    public void testSwitchTopologySink() {
        testTopology(new SwitchTopology(true));
    }
    
    /**
     * Enables debugging output for thrift monitoring.
     */
    protected static void enableThriftMonitoringDebug() {
        Properties prop = new Properties();
        prop.put(MonitoringConfiguration.THRIFT_MONITORING_DEBUG, "true");
        MonitoringConfiguration.configure(prop, false);
    }
    
    /**
     * Tests a topology.
     * 
     * @param topo the topology creation class
     */
    private void testTopology(AbstractTopology topo) {
        final String mappingFile = topo.getMappingFileName();
        LocalStormEnvironment env = new LocalStormEnvironment();

        @SuppressWarnings("rawtypes")
        Map topoCfg = createTopologyConfiguration();
        // different sequence due to test
        PipelineOptions opt = new PipelineOptions();
        RecordingTopologyBuilder builder = new RecordingTopologyBuilder(opt);
        
        topo.createTopology(topoCfg, builder);
        StormTopology topology = builder.createTopology();
        
        Map<String, TopologyTestInfo> topologies = new HashMap<String, TopologyTestInfo>();
        topologies.put(topo.getName(), new TopologyTestInfo(topology, 
            new File(Utils.getTestdataDir(), mappingFile), topoCfg));
        env.setTopologies(topologies);
        new PipelineCommand(topo.getName(), PipelineCommand.Status.START).execute();
        sleep(500);
        EventManager.send(new PipelineLifecycleEvent(topo.getName(), 
            PipelineLifecycleEvent.Status.CHECKED, null)); // fake as we have no adaptation layer started
        getPipelineStatusTracker().waitFor(topo.getName(), Status.STARTED, 30000);
        long pipRunTime = System.currentTimeMillis();
        // mapped to the created family
        //Tracing.handleEvent(new AlgorithmProfilingEvent(topo.getName(), "TestFamily", "CorrelationSW", 
        //    AlgorithmProfilingEvent.Status.START));
        sleep(10000);
        EventManager.cleanup();
        // get system state here as otherwise stopping the pipeline may accidentally clear the dynamic part
        // of the state
        SystemState state = new SystemState(MonitoringManager.getSystemState());
        INameMapping mapping = MonitoringManager.getNameMapping(topo.getName()); // preserve

        pipRunTime = System.currentTimeMillis() - pipRunTime;
        new PipelineCommand(topo.getName(), PipelineCommand.Status.STOP).execute();
        sleep(2000);
        env.shutdown();
        ThriftConnection.setLocalCluster(null);
        StormUtils.forTesting(null, null);

        SystemPart platform = state.getPlatform();
        Assert.assertNotNull(platform);

        env.cleanup();
        
        if (!isJenkins()) { // for now, copy seems to cause problems
            topo.assertState(state, mapping, pipRunTime);
        }
    }

    /**
     * Tests whether profiling with a test pipeline works.
     */
    @Test
    public void testProfiling() {
        File profileLocation = new File(FileUtils.getTempDirectory(), "profilingTest");
        FileUtils.deleteQuietly(profileLocation);
        profileLocation.mkdirs();
        String oldPLoc = configureProfilingLogLocation(profileLocation.getAbsolutePath());
        String oldDfs = configureDfsPath(FileUtils.getTempDirectory().getAbsolutePath());
        boolean localSer = IntSerializer.registerIfNeeded();
        LocalStormEnvironment env = new LocalStormEnvironment();

        @SuppressWarnings("rawtypes")
        Map topoCfg = createTopologyConfiguration();
        // different sequence due to test
        PipelineOptions opt = new PipelineOptions();
        //opt.setExecutorParallelism(Naming.NODE_PROCESS, 3);
        RecordingTopologyBuilder builder = new RecordingTopologyBuilder(opt);
        TestTopology.createTopology(builder);
        builder.close(Naming.PIPELINE_NAME, topoCfg);
        StormTopology topology = builder.createTopology();
        
        Map<String, TopologyTestInfo> topologies = new HashMap<String, TopologyTestInfo>();
        topologies.put(TestTopology.PIP_NAME, new TopologyTestInfo(topology, 
            new File(Utils.getTestdataDir(), "pipeline.xml"), topoCfg, createProfileData(TestTopology.PIP_NAME)));
        env.setTopologies(topologies);
        
        new ProfileAlgorithmCommand(Naming.NODE_PROCESS, Naming.NODE_PROCESS_ALG1).execute();
        
        // wait for shutdown through profiling
        getPipelineStatusTracker().waitFor(Naming.PIPELINE_NAME, Status.STOPPED, 30000);
        sleep(2000); // allow profile to send end event and to react on it
        
        env.shutdown();
        ThriftConnection.setLocalCluster(null);
        StormUtils.forTesting(null, null);
        IntSerializer.unregisterIfNeeded(localSer);
        configureDfsPath(oldDfs);
        configureProfilingLogLocation(oldPLoc);

        // no system state as pipeline is already removed here
        // there must be the trace file
        File[] pFiles = profileLocation.listFiles();
        Assert.assertNotNull(pFiles);
        File found = null; // there might be more files due to profiling
        for (int f = 0; null == found && f < pFiles.length; f++) {
            if (pFiles[f].getName().endsWith(".csv")) {
                found = pFiles[f];
            }
        }
        Assert.assertNotNull(found);
        Assert.assertTrue(found.exists());
        Assert.assertTrue(found.length() > 0);

        FileUtils.deleteQuietly(profileLocation);
        env.cleanup();
    }

    /**
     * Tests whether profiling with a test pipeline works.
     */
    @Test
    public void testProfilingGenTopology() {
        File profileLocation = new File(FileUtils.getTempDirectory(), "profilingTest");
        FileUtils.deleteQuietly(profileLocation);
        profileLocation.mkdirs();
        String oldPLoc = configureProfilingLogLocation(profileLocation.getAbsolutePath());
        String oldDfs = configureDfsPath(FileUtils.getTempDirectory().getAbsolutePath());
        DetailMode oldDetails = configureDetailedProfiling(DetailMode.ALGORITHMS); // or .TASKS
        boolean localSer = IntSerializer.registerIfNeeded();
        LocalStormEnvironment env = new LocalStormEnvironment();

        GenTopology topo = new GenTopology();
        @SuppressWarnings("rawtypes")
        Map topoCfg = createTopologyConfiguration();
        // different sequence due to test
        PipelineOptions opt = new PipelineOptions();
        //opt.setExecutorParallelism(Naming.NODE_PROCESS, 3);
        RecordingTopologyBuilder builder = new RecordingTopologyBuilder(opt);
        topo.createTopology(topoCfg, builder);
        StormTopology topology = builder.createTopology();
        
        Map<String, TopologyTestInfo> topologies = new HashMap<String, TopologyTestInfo>();
        topologies.put(topo.getName(), new TopologyTestInfo(topology, 
            new File(Utils.getTestdataDir(), "testGenPip.xml"), topoCfg, createProfileData(topo.getName())));
        env.setTopologies(topologies);
        
        new ProfileAlgorithmCommand("TestFamily", "CorrelationSW").execute();
        
        // wait for shutdown through profiling
        getPipelineStatusTracker().waitFor(topo.getName(), Status.STOPPED, 30000);
        sleep(2000); // allow profile to send end event and to react on it
        
        env.shutdown();
        ThriftConnection.setLocalCluster(null);
        StormUtils.forTesting(null, null);
        IntSerializer.unregisterIfNeeded(localSer);
        configureDfsPath(oldDfs);
        configureProfilingLogLocation(oldPLoc);
        configureDetailedProfiling(oldDetails);

        // no system state as pipeline is already removed here
        // there must be the trace file
        File[] pFiles = profileLocation.listFiles();
        Assert.assertNotNull(pFiles);
        Assert.assertTrue(1 == pFiles.length);
        Assert.assertTrue(pFiles[0].exists());
        Assert.assertTrue(pFiles[0].length() > 0);

        FileUtils.deleteQuietly(profileLocation);
        env.cleanup();
    }

    
    /**
     * Creates a profile data instance with simple profiling information.
     * 
     * @param pipelineName the pipeline name
     * @return the profiling data instance
     */
    private static ProfileData createProfileData(String pipelineName) {
        File testDataDir = Utils.getTestdataDir();
        File pipeline = new File(testDataDir, "pipeline.jar"); // does not exist
        File data = new File(testDataDir, "profile.data");
        File control = new File(testDataDir, "profile.ctl");
        return new ProfileData(pipelineName, pipeline, data, control);
    }

    /**
     * Configures the profiling log location.
     * 
     * @param location the new location
     * @return the old location
     */
    private static String configureProfilingLogLocation(String location) {
        String result = MonitoringConfiguration.getProfilingLogLocation();
        Properties prop = new Properties();
        prop.put(MonitoringConfiguration.PROFILING_LOG_LOCATION, location);
        MonitoringConfiguration.configure(prop, false);
        return result;
    }
    
    /**
     * Enables or disables detailed profiling.
     * 
     * @param mode the new detail mode
     * @return the old mode
     */
    private static DetailMode configureDetailedProfiling(DetailMode mode) {
        DetailMode result = MonitoringConfiguration.getProfilingMode();
        Properties prop = new Properties();
        prop.put(CoordinationConfiguration.DETAILED_PROFILING, mode.name());
        MonitoringConfiguration.configure(prop, false);
        return result;
    }
    
    /**
     * Configures the DFS path.
     * 
     * @param path the new DFS path
     * @return the old DFS path
     */
    private static String configureDfsPath(String path) {
        String result = MonitoringConfiguration.getDfsPath();
        Properties prop = new Properties();
        prop.put(MonitoringConfiguration.PATH_DFS, path);
        MonitoringConfiguration.configure(prop, false);
        return result;
    }

}
