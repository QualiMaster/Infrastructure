package tests.eu.qualimaster.monitoring;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
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
import tests.eu.qualimaster.monitoring.genTopo.EndOfDataEventHandler;
import tests.eu.qualimaster.monitoring.genTopo.GenTopology;
import tests.eu.qualimaster.monitoring.genTopo.HwTopology;
import tests.eu.qualimaster.monitoring.genTopo.HwTopologyInt;
import tests.eu.qualimaster.monitoring.genTopo.ManTopology;
import tests.eu.qualimaster.monitoring.genTopo.ProfilingSourceTopology;
import tests.eu.qualimaster.monitoring.genTopo.SubTopology;
import tests.eu.qualimaster.monitoring.genTopo.SwitchTopology;
import tests.eu.qualimaster.storm.Naming;
import tests.eu.qualimaster.storm.TestTopology;
import tests.eu.qualimaster.storm.Topology;
import eu.qualimaster.easy.extension.internal.AlgorithmProfileHelper.ProfileData;
import eu.qualimaster.base.pipeline.RecordingTopologyBuilder;
import eu.qualimaster.coordination.CoordinationConfiguration;
import eu.qualimaster.coordination.INameMapping;
import eu.qualimaster.coordination.ParallelismChangeRequest;
import eu.qualimaster.coordination.StormUtils;
import eu.qualimaster.coordination.ZkUtils;
import eu.qualimaster.coordination.StormUtils.TopologyTestInfo;
import eu.qualimaster.coordination.commands.ParallelismChangeCommand;
import eu.qualimaster.coordination.commands.PipelineCommand;
import eu.qualimaster.coordination.commands.ProfileAlgorithmCommand;
import eu.qualimaster.coordination.events.AlgorithmProfilingEvent.DetailMode;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent.Status;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.infrastructure.PipelineOptions;
import eu.qualimaster.infrastructure.PipelineStatusTracker;
import eu.qualimaster.monitoring.MonitoringConfiguration;
import eu.qualimaster.monitoring.MonitoringManager;
import eu.qualimaster.monitoring.events.FrozenSystemState;
import eu.qualimaster.monitoring.events.SubTopologyMonitoringEvent;
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

    private File profiles;
    
    /**
     * Executed before a single test.
     */
    @Before
    public void setUp() {
        Utils.setModelProvider(Utils.INFRASTRUCTURE_TEST_MODEL_PROVIDER);
        Utils.configure();
        super.setUp();

        profiles = new File(FileUtils.getTempDirectory(), "profiles");
        FileUtils.deleteQuietly(profiles);
        profiles.mkdirs();
        Properties prop = new Properties();
        prop.put(MonitoringConfiguration.PROFILE_LOCATION, profiles.getAbsolutePath());
        MonitoringConfiguration.configure(prop);
        
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
        FileUtils.deleteQuietly(profiles);
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

        sleep(10000);
        
        EventManager.cleanup();
        // get system state here as otherwise stopping the pipeline may accidentally clear the dynamic part
        // of the state
        SystemState state = new SystemState(MonitoringManager.getSystemState());
        FrozenSystemState frozenState = state.freeze();
        
        new PipelineCommand(Naming.PIPELINE_NAME, PipelineCommand.Status.STOP).execute();
        sleep(2000);
        env.shutdown();
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
        //assertNotMeasured(part, ResourceUsage.USED_MEMORY); // this needs SPASS-meter
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
        if (!isJenkins()) {
            testTopology(new GenTopology());
        }
    }
    
    /**
     * Tests a topology emulating a manual sub-topology.
     */
    @Test
    public void testManTopology() {
        if (!isJenkins()) {
            testTopology(new ManTopology());
        }
    }

    /**
     * Tests a topology emulating a generated tightly integrated hardware sub-topology.
     */
    @Test
    public void testHwTopology() {
        if (!isJenkins()) {
            testTopology(new HwTopology());
        }
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
    @Test(timeout = 2 * 60 * 1000)
    public void testSwitchTopologySink() {
        if (!isJenkins()) {
            testTopology(new SwitchTopology(true));
        }
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
        EndOfDataEventHandler eodHandler = installEodHandler(topo);

        @SuppressWarnings("rawtypes")
        Map topoCfg = createTopologyConfiguration();
        // different sequence due to test
        PipelineOptions opt = new PipelineOptions();
        RecordingTopologyBuilder builder = new RecordingTopologyBuilder(opt);
        
        SubTopologyMonitoringEvent evt = topo.createTopology(topoCfg, builder);
        StormTopology topology = builder.createTopology();
        
        Map<String, TopologyTestInfo> topologies = new HashMap<String, TopologyTestInfo>();
        topo.registerSubTopologies(topologies);
        topo.handleOptions(topoCfg, opt);
        TopologyTestInfo ti = new TopologyTestInfo(topology, new File(Utils.getTestdataDir(), mappingFile), topoCfg);
        ti.setSubTopologyEvent(evt);
        topologies.put(topo.getName(), ti);
        env.setTopologies(topologies);
        PipelineOptions pOpt = new PipelineOptions();
        pOpt.setOption(PipelineCommand.KEY_SUPPRESS_ACTIVE_CHECK, Boolean.TRUE);
        new PipelineCommand(topo.getName(), PipelineCommand.Status.START, pOpt).execute();
        sleep(500);
        EventManager.send(new PipelineLifecycleEvent(topo.getName(), 
            PipelineLifecycleEvent.Status.CHECKED, null)); // fake as we have no adaptation layer started
        getPipelineStatusTracker().waitFor(topo.getName(), Status.CREATED, 30000);
        sleep(1000);
        // mapped to the created family
        topo.started();
        long pipRunTime = System.currentTimeMillis();
        //Tracing.handleEvent(new AlgorithmProfilingEvent(topo.getName(), "TestFamily", "CorrelationSW", 
        //    AlgorithmProfilingEvent.Status.START));
        sleep(topo.plannedExecutionTime());
        EventManager.cleanup();
        // get system state here as otherwise stopping the pipeline may accidentally clear the dynamic part
        // of the state
        SystemState state = new SystemState(MonitoringManager.getSystemState());
        INameMapping mapping = MonitoringManager.getNameMapping(topo.getName()); // preserve

        pipRunTime = System.currentTimeMillis() - pipRunTime;
        stopPipeline(topo, eodHandler);
        env.shutdown();
        StormUtils.forTesting(null, null);
        uninstallEodHandler(eodHandler);

        SystemPart platform = state.getPlatform();
        Assert.assertNotNull(platform);

        env.cleanup();
        
        if (!isJenkins()) { // for now, copy seems to cause problems
            topo.assertState(state, mapping, pipRunTime);
        }
    }
    
    /**
     * Creates, installs and returns and end-of-data event handler if requested by <code>topo</code>.
     * 
     * @param topo the topology to query for the installation
     * @return the created handler, may be <b>null</b> if none was installed
     */
    private static EndOfDataEventHandler installEodHandler(AbstractTopology topo) {
        EndOfDataEventHandler result = null;
        if (null != topo && topo.installGenericEoDEventHandler()) {
            result = new EndOfDataEventHandler();
            EventManager.register(result);
        }
        return result;
    }
    
    /**
     * Uninstalls a potential end-of-data event handler.
     * 
     * @param handler the handler (may be <b>null</b>, call is ignored then)
     */
    private static void uninstallEodHandler(EndOfDataEventHandler handler) {
        if (null != handler) {
            EventManager.unregister(handler);
        }
    }

    /**
     * Stops the pipeline described by <code>topo</code> if there is no <code>handler</code> or the <code>handler</code>
     * did not receive an end-of-data event.
     * 
     * @param topo the topology
     * @param handler the end-of-data event handler (may be <b>null</b>)
     */
    private static void stopPipeline(AbstractTopology topo, EndOfDataEventHandler handler) {
        if (null == handler || !handler.wasReceived()) {
            new PipelineCommand(topo.getName(), PipelineCommand.Status.STOP).execute();
            sleep(2000);
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
        builder.close(TestTopology.PIP_NAME, topoCfg);
        StormTopology topology = builder.createTopology();
        
        Map<String, TopologyTestInfo> topologies = new HashMap<String, TopologyTestInfo>();
        topologies.put(TestTopology.PIP_NAME, new TopologyTestInfo(topology, 
            new File(Utils.getTestdataDir(), "profilingPipeline.xml"), 
            topoCfg, createProfileData(TestTopology.PIP_NAME)));
        env.setTopologies(topologies);
        
        new ProfileAlgorithmCommand(Naming.NODE_PROCESS, Naming.NODE_PROCESS_ALG1).execute();
        
        // wait for shutdown through profiling
        PipelineStatusTracker tracker = new PipelineStatusTracker(false);
        tracker.waitFor(TestTopology.PIP_NAME, Status.STOPPED, 30000);
        sleep(2000); // allow profile to send end event and to react on it
        
        env.shutdown();
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

        GenTopology topo = new GenTopology(30);
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
        sleep(3000); // allow profile to send end event and to react on it

        File[] pFiles = profileLocation.listFiles();
        long logFileLength = -1;
        if (null != pFiles) {
            File f = null;
            for (int i = 0; null == f && i < pFiles.length; i++) {
                if (pFiles[i].isFile() && pFiles[i].getName().endsWith(".csv")) {
                    f = pFiles[i];
                }
            }
            logFileLength = null != f ? f.length() : -2;
        }

        env.shutdown();
        StormUtils.forTesting(null, null);
        IntSerializer.unregisterIfNeeded(localSer);
        configureDfsPath(oldDfs);
        configureProfilingLogLocation(oldPLoc);
        configureDetailedProfiling(oldDetails);

        // no system state as pipeline is already removed here
        // there must be the trace file

        FileUtils.deleteQuietly(profileLocation);
        env.cleanup();

        Assert.assertTrue("Profiling log problem " + logFileLength, logFileLength > 0);
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

    /**
     * Tests a topology emulating a generated hardware sub-topology (with sink).
     */
    @Test
    public void testSubTopology() {
        testTopology(new SubTopology());
    }
    
    /**
     * Tests just the speed of a profiling source.
     */
    @Test
    public void testProfilingSource() {
        testTopology(new ProfilingSourceTopology());
    }

    /**
     * Tests the pipeline parallelism executor.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testPipelineParallelismExecutor() throws IOException {
        if (ZkUtils.isQmStormVersion()) { // only supported by the patched version
            testPipelineCommands(TestExecutionMode.EXECUTOR_PARALLELISM);
        }
    }

    /**
     * Tests pipeline commands.
     * 
     * @param mode the execution mode
     * @throws IOException shall not occur
     */
    private void testPipelineCommands(TestExecutionMode mode) throws IOException {
        // System.setProperty("storm.conf.file", "test.yaml"); -> QMstormVersion
        LocalStormEnvironment env = new LocalStormEnvironment();
        // build the test topology
        RecordingTopologyBuilder builder = new RecordingTopologyBuilder();
        Topology.createTopology(builder);
        StormTopology topology = builder.createTopology();
        Map<String, TopologyTestInfo> topologies = new HashMap<String, TopologyTestInfo>();
        @SuppressWarnings("rawtypes")
        Map topoCfg = createTopologyConfiguration();
        //topoCfg.put(Config.TOPOLOGY_WORKERS, 3);
        topologies.put(Naming.PIPELINE_NAME, new TopologyTestInfo(topology, 
            new File(Utils.getTestdataDir(), "pipeline.xml"), topoCfg));
        env.setTopologies(topologies);
        clear();

        PipelineCommand cmd = new PipelineCommand(Naming.PIPELINE_NAME, PipelineCommand.Status.START);
        cmd.execute();
        fakeCheckedPipeline(Naming.PIPELINE_NAME);
        waitForExecution(1, 0, 1000); // pipeline status tracker <-> monitoring
        Assert.assertTrue(getTracer().contains(cmd));
        Assert.assertEquals(1, getTracer().getLogEntryCount());
        Assert.assertEquals(0, getFailedHandler().getFailedCount());
        clear();

        sleep(3000); // let Storm run for a while

        cmd = new PipelineCommand(Naming.PIPELINE_NAME, PipelineCommand.Status.CONNECT);
        cmd.execute();
        waitForExecution(1, 0, 1000);
        Assert.assertTrue(getTracer().contains(cmd));
        Assert.assertEquals(1, getTracer().getLogEntryCount());
        Assert.assertEquals(0, getFailedHandler().getFailedCount());
        clear();

        /*EventManager.send(new SourceVolumeAdaptationEvent(Naming.PIPELINE_NAME, Naming.NODE_SOURCE, 
            create("1656", 1839.5837325157627), create("1656", 0.47229364121072215), 
            create("1656", 0.0), create("1656", 3895L), 
            create("1656", 5734.583732515763), create("1656", 5195.447782491298)));*/
        
        handleMode(mode, 1);
        
        sleep(4000); // let Storm run for a while // 600000
        
        handleMode(mode, 2);

        cmd = new PipelineCommand(Naming.PIPELINE_NAME, PipelineCommand.Status.DISCONNECT);
        cmd.execute();
        waitForExecution(1, 0, 1000);
        //Assert.assertTrue(getTracer().contains(cmd));
        //Assert.assertEquals(1, getTracer().getLogEntryCount());
        //Assert.assertEquals(0, getFailedHandler().getFailedCount());
        clear();

        sleep(1000);
        cmd = new PipelineCommand(Naming.PIPELINE_NAME, PipelineCommand.Status.STOP);
        cmd.execute();
        waitForExecution(1, 0, 1000, new PipelineCommandFilter(PipelineCommand.Status.STOP));
        //Assert.assertTrue(getTracer().contains(cmd));
        //Assert.assertEquals(1, getTracer().getLogEntryCount());
        //Assert.assertEquals(0, getFailedHandler().getFailedCount());
        clear();

        sleep(2000); // let Storm run for a while
        env.shutdown();
        env.cleanup();
    }

    /**
     * Creates a map from a single key-value mapping.
     * 
     * @param <T> the value type
     * @param key the key
     * @param value the value
     * @return the map
     */
    @SuppressWarnings("unused")
    private <T> Map<String, T> create(String key, T value) {
        Map<String, T> result = new HashMap<String, T>();
        result.put(key, value);
        return result;
    }

    /**
     * Handles an execution mode.
     * 
     * @param mode the mode
     * @param callCount the call count
     * @throws IOException shall not occur
     */
    private void handleMode(TestExecutionMode mode, int callCount) throws IOException {
        if (mode.changesParallelism()) {
            if (1 == callCount) {
                sleep(1000); // let Storm run for a while
                Map<String, ParallelismChangeRequest> taskChanges = new HashMap<String, ParallelismChangeRequest>();
                if (TestExecutionMode.EXECUTOR_PARALLELISM == mode) {
                    taskChanges.put(Naming.NODE_PROCESS, new ParallelismChangeRequest(1));
                } else if (TestExecutionMode.WORKER_PARALLELISM == mode) {
                    String host = InetAddress.getLocalHost().getCanonicalHostName();
                    // same host (local cluster), but second supervisor
                    // test with StormUtils.ParallelismChangeRequest(1, host, 1)
                    taskChanges.put(Naming.NODE_PROCESS, new ParallelismChangeRequest(0, host, true));
                }
                new ParallelismChangeCommand(Naming.PIPELINE_NAME, taskChanges).execute(); // command just for testing
                sleep(8000); // let Storm run for a while
            } else { // revert the changes
                Map<String, ParallelismChangeRequest> taskChanges = new HashMap<String, ParallelismChangeRequest>();
                if (TestExecutionMode.EXECUTOR_PARALLELISM == mode) {
                    taskChanges.put(Naming.NODE_PROCESS, new ParallelismChangeRequest(-1));
                } else if (TestExecutionMode.WORKER_PARALLELISM == mode) {
                    sleep(10000);
                    String host = InetAddress.getLocalHost().getCanonicalHostName();
                    taskChanges.put(Naming.NODE_PROCESS, new ParallelismChangeRequest(0, host, true));
                }
                new ParallelismChangeCommand(Naming.PIPELINE_NAME, taskChanges).execute(); // command just for testing
                sleep(10000); // let Storm run for a while
            }
        } 
    }

    /**
     * Defines execution modes.
     * 
     * @author Holger Eichelberger
     */
    private enum TestExecutionMode {
        DEFAULT_EXECUTION(false),
        WORKER_PARALLELISM(true),
        EXECUTOR_PARALLELISM(true),
        LOAD_SHEDDING_SOURCE(false),
        LOAD_SHEDDING_PROCESSOR(false);

        private boolean changesParallelism;
        
        /**
         * Creates a new "constant".
         * 
         * @param changesParallelism whether parallelism changes shall happen
         */
        private TestExecutionMode(boolean changesParallelism) {
            this.changesParallelism = changesParallelism;
        }
        
        /**
         * Returns whether a parallelism change shall happen.
         * 
         * @return <code>true</code> in case of a change, <code>false</code> else
         */
        public boolean changesParallelism() {
            return changesParallelism;
        }
    }

}
