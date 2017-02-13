package tests.eu.qualimaster.coordination;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import backtype.storm.ILocalCluster;
import backtype.storm.generated.StormTopology;
import backtype.storm.testing.TestJob;
import tests.eu.qualimaster.storm.Naming;
import tests.eu.qualimaster.storm.SignalCollector;
import tests.eu.qualimaster.storm.TestTopology;
import tests.eu.qualimaster.storm.SignalCollector.SignalEntry;
import tests.eu.qualimaster.storm.Topology;
import eu.qualimaster.base.pipeline.RecordingTopologyBuilder;
import eu.qualimaster.common.shedding.DefaultLoadShedders;
import eu.qualimaster.common.shedding.DefaultLoadSheddingParameter;
import eu.qualimaster.coordination.CoordinationConfiguration;
import eu.qualimaster.coordination.CoordinationManager;
import eu.qualimaster.coordination.ParallelismChangeRequest;
import eu.qualimaster.coordination.StormUtils;
import eu.qualimaster.coordination.StormUtils.TopologyTestInfo;
import eu.qualimaster.coordination.ZkUtils;
import eu.qualimaster.coordination.commands.AlgorithmChangeCommand;
import eu.qualimaster.coordination.commands.CoordinationCommand;
import eu.qualimaster.coordination.commands.LoadSheddingCommand;
import eu.qualimaster.coordination.commands.ParallelismChangeCommand;
import eu.qualimaster.coordination.commands.ParameterChangeCommand;
import eu.qualimaster.coordination.commands.PipelineCommand;
import eu.qualimaster.coordination.commands.ProfileAlgorithmCommand;
import eu.qualimaster.coordination.commands.ReplayCommand;
import eu.qualimaster.events.EventHandler;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.PipelineOptions;
import eu.qualimaster.infrastructure.EndOfDataEvent;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent.Status;

/**
 * Coordination manager tests. Set environment variable 
 * "STORM_TEST_TIMEOUT_MS" to a value greater than 15.000 (ms).
 */
public class StormTests extends AbstractCoordinationTests {
    
    private static final String IS_NULL = "*null*";
    
    /**
     * Executed before a single test.
     * 
     * @see #configure()
     */
    @Before
    public void setUp() {
        Utils.setModelProvider(Utils.INFRASTRUCTURE_TEST_MODEL_PROVIDER);
        Utils.configure();
        super.setUp();
        CoordinationManager.registerTestMapping(TestNameMapping.INSTANCE);
    }
    
    /**
     * Executed after a single test.
     */
    @After
    public void tearDown() {
        super.tearDown();
        Utils.dispose();
    }
    
    /**
     * Test execution modes for reusing test cases.
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
        
    // checkstyle: stop exception type check

    /**
     * Performs the pipeline commands test.
     * 
     * @param mode the intended execution mode
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

        handleMode(mode, 1);
        
        sleep(4000); // let Storm run for a while // 600000
        
        handleMode(mode, 2);

        cmd = new PipelineCommand(Naming.PIPELINE_NAME, PipelineCommand.Status.DISCONNECT);
        cmd.execute();
        waitForExecution(1, 0, 1000);
        Assert.assertTrue(getTracer().contains(cmd));
        Assert.assertEquals(1, getTracer().getLogEntryCount());
        Assert.assertEquals(0, getFailedHandler().getFailedCount());
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
     * Configures the DFS path to the actual temp path.
     * 
     * @return the path before
     */
    private String configureDfsPathTemp() {
        return configureDfsPath(FileUtils.getTempDirectoryPath());
    }
    
    /**
     * Configures the DFS path to <code>path</code> and returns the path before.
     * 
     * @param path the new path
     * @return the path before
     */
    private String configureDfsPath(String path) {
        String old = CoordinationConfiguration.getDfsPath();
        Properties prop = new Properties();
        prop.setProperty(CoordinationConfiguration.PATH_DFS, path);
        CoordinationConfiguration.configure(prop, false);
        return old;
    }
    
    /**
     * An end-of-data event handler for ending profiling tests.
     * 
     * @author Holger Eichelberger
     */
    private static class EndOfDataEventHandler extends EventHandler<EndOfDataEvent> {

        private long receivedTimestamp = -1;
        private String pipelineName;
        
        /**
         * Creates an event handler instance.
         * 
         * @param pipelineName the name of the pipeline to wait for
         */
        private EndOfDataEventHandler(String pipelineName) {
            super(EndOfDataEvent.class);
            this.pipelineName = pipelineName;
        }

        @Override
        protected void handle(EndOfDataEvent event) {
            if (event.getPipeline().equals(pipelineName)) {
                receivedTimestamp = System.currentTimeMillis();
            }
        }
        
        /**
         * Returns whether the expected event was received.
         * 
         * @return <code>true</code> if received, <code>false</code> else
         */
        private boolean received() {
            // give infra/Storm time to kill the pipeline
            return receivedTimestamp > 0 && System.currentTimeMillis() - receivedTimestamp > 3000;
        }
        
    }

    /**
     * Performs a test-run of the profiling functionality.
     */
    @Test
    public void testProfilingPipeline() {
        EndOfDataEventHandler handler = new EndOfDataEventHandler(TestTopology.PIP_NAME);
        EventManager.register(handler);
        boolean localSer = IntSerializer.registerIfNeeded();
        String pathDFS = configureDfsPathTemp();
        LocalStormEnvironment env = new LocalStormEnvironment();
        // build the test topology
        RecordingTopologyBuilder builder = new RecordingTopologyBuilder();
        TestTopology.createTopology(builder);
        StormTopology topology = builder.createTopology();
        Map<String, TopologyTestInfo> topologies = new HashMap<String, TopologyTestInfo>();
        @SuppressWarnings("rawtypes")
        Map topoCfg = createTopologyConfiguration();
        topologies.put(TestTopology.PIP_NAME, new TopologyTestInfo(topology, 
            new File(Utils.getTestdataDir(), "pipeline.xml"), topoCfg));
        env.setTopologies(topologies);
        clear();
   
        sleep(1000);
        
        ProfileAlgorithmCommand cmd = new ProfileAlgorithmCommand(Naming.NODE_PROCESS_FAMILY, Naming.NODE_PROCESS_ALG1);
        cmd.execute();

        // wait for end-of-data event but at maximum 10s
        long now = System.currentTimeMillis();
        while (!handler.received() && System.currentTimeMillis() - now < 10000) {
            sleep(1000);
        }
        env.shutdown();
        env.cleanup();
        configureDfsPath(pathDFS);
        IntSerializer.unregisterIfNeeded(localSer);
        EventManager.unregister(handler);
    }

    /**
     * Does the mode specific changes.
     * 
     * @param mode the desired execution mode
     * @param callCount how often is this method called during the test
     * @throws IOException in case of problems
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
                StormUtils.changeParallelism(Naming.PIPELINE_NAME, taskChanges, 
                    new ParallelismChangeCommand(Naming.PIPELINE_NAME, taskChanges)); // command just for testing
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
                StormUtils.changeParallelism(Naming.PIPELINE_NAME, taskChanges, 
                    new ParallelismChangeCommand(Naming.PIPELINE_NAME, taskChanges)); // command just for testing
                sleep(10000); // let Storm run for a while
            }
        } else if (TestExecutionMode.LOAD_SHEDDING_SOURCE == mode) {
            sendLoadSheddingCommand(Naming.NODE_SOURCE);
        } else if (TestExecutionMode.LOAD_SHEDDING_PROCESSOR == mode) {
            sendLoadSheddingCommand(Naming.NODE_PROCESS);
        }
    }
    
    /**
     * Sends a load shedding command to the test pipeline.
     * 
     * @param node the node to send the command to
     */
    private void sendLoadSheddingCommand(String node) {
        LoadSheddingCommand cmd = new LoadSheddingCommand(Naming.PIPELINE_NAME, node, DefaultLoadShedders.NTH_ITEM);
        cmd.setIntParameter(DefaultLoadSheddingParameter.NTH_TUPLE, 2);
        cmd.execute();

        waitForExecution(1, 0, 1000);
        Assert.assertTrue(getTracer().contains(cmd));
        Assert.assertEquals(1, getTracer().getLogEntryCount());
        Assert.assertEquals(0, getFailedHandler().getFailedCount());
        clear();

        sleep(5000);
    }

    // checkstyle: resume exception type check

    /**
     * Tests successful pipeline commands.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testPipelineCommand() throws IOException {
        if (!isJenkins()) {
            testPipelineCommands(TestExecutionMode.DEFAULT_EXECUTION);
        }
    }

    /**
     * Tests sending a replay command.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testReplayCommand() throws IOException {
        // System.setProperty("storm.conf.file", "test.yaml"); -> QMstormVersion
        LocalStormEnvironment env = new LocalStormEnvironment();
        // build the test topology
        RecordingTopologyBuilder builder = new RecordingTopologyBuilder();
        Topology.createReplayTopology(builder, Naming.PIPELINE_NAME);
        StormTopology topology = builder.createTopology();
        Map<String, TopologyTestInfo> topologies = new HashMap<String, TopologyTestInfo>();
        @SuppressWarnings("rawtypes")
        Map topoCfg = createTopologyConfiguration();
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

        PipelineLifecycleEvent fake = new PipelineLifecycleEvent(Naming.PIPELINE_NAME, 
            PipelineLifecycleEvent.Status.CREATED, null);
        EventManager.send(fake);

        sleep(3000); // let Storm run for a while
        
        ReplayCommand rCommand = new ReplayCommand(Naming.PIPELINE_NAME, Naming.NODE_SINK, true, 1);
        rCommand.execute();
        
        waitForExecution(1, 0, 1000);

        sleep(1000);
        cmd = new PipelineCommand(Naming.PIPELINE_NAME, PipelineCommand.Status.STOP);
        cmd.execute();
        
        sleep(2000); // let Storm run for a while
        env.shutdown();
        env.cleanup();
        
    }

    /**
     * Tests pipeline parallelization on executor level.
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
     * Tests pipeline parallelization on worker level.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testPipelineParallelismWorker() throws IOException {
        if (ZkUtils.isQmStormVersion()) { // only supported by the patched version
            testPipelineCommands(TestExecutionMode.WORKER_PARALLELISM);
        }
    }

    /**
     * Implements the test job for failing pipeline commands.
     * 
     * @author Holger Eichelberger
     */
    private class TestPipelineCommandsFailingJob implements TestJob {
        
        // checkstyle: stop exception type check

        @Override
        public void run(ILocalCluster cluster) throws Exception {
            Map<String, TopologyTestInfo> topologies = new HashMap<String, TopologyTestInfo>();
            // to testing topologies - must fail
            StormUtils.forTesting(cluster, topologies);
            clear();
            
            // try starting unknown
            
            PipelineOptions opt = new PipelineOptions();
            opt.setNumberOfWorkers(3);
            PipelineCommand cmd = new PipelineCommand("abba", PipelineCommand.Status.START, opt);
            cmd.execute();
            fakeCheckedPipeline("abba");
            waitForExecution(1, 0, 500); // pipeline status tracker <-> monitoring
            Assert.assertEquals(1, getFailedHandler().getFailedCount());
            clear();

            cmd = new PipelineCommand(null, PipelineCommand.Status.START, opt);
            cmd.execute();
            waitForExecution(0, 1);
            Assert.assertEquals(1, getFailedHandler().getFailedCount());
            clear();

            // TODO try connect / disconnect not started pipeline
            
            // try unknown command

            cmd = new PipelineCommand("abba", null);
            cmd.execute();
            waitForExecution(0, 1);
            Assert.assertTrue(getTracer().contains(cmd));
            Assert.assertEquals(1, getTracer().getLogEntryCount());
            Assert.assertEquals(1, getFailedHandler().getFailedCount());
            clear();
            
            cmd = new PipelineCommand("abba", PipelineCommand.Status.STOP);
            cmd.execute();
            waitForExecution(0, 1);
            Assert.assertTrue(getTracer().contains(cmd));
            Assert.assertEquals(1, getTracer().getLogEntryCount());
            Assert.assertEquals(1, getFailedHandler().getFailedCount());
            clear();

            cmd = new PipelineCommand(null, PipelineCommand.Status.STOP);
            cmd.execute();
            waitForExecution(0, 1);
            Assert.assertTrue(getTracer().contains(cmd));
            Assert.assertEquals(1, getTracer().getLogEntryCount());
            Assert.assertEquals(1, getFailedHandler().getFailedCount());
            clear();

            StormUtils.forTesting(null, null);
        }

        // checkstyle: resume exception type check

    }

    /**
     * Tests unsuccessful pipeline commands.
     */
    @Test
    public void testPipelineCommandFailing() {
        testInLocalCluster(new TestPipelineCommandsFailingJob());
    }
    
    /**
     * Tests the signals.
     */
    @Test
    public void testSignals() {
        LocalStormEnvironment env = new LocalStormEnvironment();
        RecordingTopologyBuilder builder = new RecordingTopologyBuilder();
        Topology.createTopology(builder);
        StormTopology topology = builder.createTopology();
        @SuppressWarnings("rawtypes")
        Map config = createTopologyConfiguration();
        builder.close(Naming.PIPELINE_NAME, config);

        Map<String, TopologyTestInfo> topologies = new HashMap<String, TopologyTestInfo>();
        topologies.put(Naming.PIPELINE_NAME, new TopologyTestInfo(topology, 
            new File(Utils.getTestdataDir(), "pipeline.xml"), config));
        env.setTopologies(topologies);
        clear();
        
        PipelineOptions opt = new PipelineOptions();
        opt.setExecutorArgument("process",  "delay", 3); // see output    
        new PipelineCommand(Naming.PIPELINE_NAME, PipelineCommand.Status.START, opt).execute();
        fakeCheckedPipeline(Naming.PIPELINE_NAME);
        getPipelineStatusTracker().waitFor(Naming.PIPELINE_NAME, Status.STARTED, 10000);
        clear();

        sleep(4000); // let Storm run for a while and start curator
        // we have no monitoring layer - jump over lifecylce
        PipelineLifecycleEvent fake = new PipelineLifecycleEvent(Naming.PIPELINE_NAME, 
            PipelineLifecycleEvent.Status.CREATED, null);
        EventManager.send(fake);
        sleep(1000); // let Storm run for a while and start curator
        
        CoordinationCommand cmd = new ParameterChangeCommand<Integer>(Naming.PIPELINE_NAME, 
            Naming.NODE_SOURCE, "param", 5);
        cmd.execute();
        waitForExecution(1, 0);
        
        Assert.assertTrue(getTracer().contains(cmd));
        Assert.assertEquals(1, getTracer().getLogEntryCount());
        Assert.assertEquals(0, getFailedHandler().getFailedCount());
        clear();

        sleep(4000); // let Storm run for a while
        cmd = new AlgorithmChangeCommand(Naming.PIPELINE_NAME, Naming.NODE_PROCESS, Naming.NODE_PROCESS_ALG2);
        cmd.execute();
        waitForExecution(1, 0);
        
        Assert.assertTrue(getTracer().contains(cmd));
        Assert.assertEquals(1, getTracer().getLogEntryCount());
        Assert.assertEquals(0, getFailedHandler().getFailedCount());
        clear();

        sleep(2000); // let Storm run for a while
        new PipelineCommand(Naming.PIPELINE_NAME, PipelineCommand.Status.STOP).execute();
        waitForExecution(1, 0);
        clear();

        List<SignalEntry> entries = SignalCollector.read(Naming.LOG_PROCESS);
        assertSignalEntry(entries, Naming.NODE_PROCESS_ALG2, IS_NULL, IS_NULL);
        assertSignalEntry(entries, SignalCollector.NAME_SHUTDOWN, IS_NULL, IS_NULL);
        
        entries = SignalCollector.read(Naming.LOG_SOURCE);
        assertSignalEntry(entries, IS_NULL, "param", "5");
        assertSignalEntry(entries, SignalCollector.NAME_SHUTDOWN, IS_NULL, IS_NULL);

        entries = SignalCollector.read(Naming.LOG_SINK);
        assertSignalEntry(entries, SignalCollector.NAME_SHUTDOWN, IS_NULL, IS_NULL);

        env.shutdown();
        env.cleanup();
    }
    
    /**
     * Returns whether the <code>expected</code> signal entry field matches the <code>actual</code> 
     * considering {@link #IS_NULL}.
     * 
     * @param expected the expected field value
     * @param actual the actual field value
     * @return <code>true</code> if <code>expected</code> and <code>actual</code> match, <code>false</code> else
     */
    private static boolean matchesSignalEntryField(String expected, String actual) {
        return (IS_NULL.equals(expected) && null == actual) 
            || expected.equals(actual);
    }

    /**
     * Asserts the existence of a signal entry in <code>entries</code>.
     * 
     * @param entries the entries to search
     * @param algorithm the algorithm name (ignored if <b>null</b>, may be {@link #IS_NULL})
     * @param parameterName the parameter name (ignored if <b>null</b>, may be {@link #IS_NULL})
     * @param parameterValue the parameter value (ignored if <b>null</b>, may be {@link #IS_NULL})
     */
    private static void assertSignalEntry(List<SignalEntry> entries, String algorithm, String parameterName, 
        String parameterValue) {
        Assert.assertNotNull("entries must be recorded - no entries", entries);
        boolean found = null == algorithm && null == parameterName && null == parameterValue; // let unspecified pass
        for (int e = 0; !found && e < entries.size(); e++) {
            SignalEntry entry = entries.get(e);
            int requiredCount = 0;
            int matchCount = 0;
            if (null != algorithm) {
                requiredCount++;
                if (matchesSignalEntryField(algorithm, entry.getAlgorithm())) {
                    matchCount++;
                }
            }
            if (null != parameterName) {
                requiredCount++;
                if (matchesSignalEntryField(parameterName, entry.getParameterName())) {
                    matchCount++;
                }
            }
            if (null != parameterValue) {
                requiredCount++;
                if (matchesSignalEntryField(parameterValue, entry.getParameterValue())) {
                    matchCount++;
                }
            }
            if (requiredCount > 0) {
                found = requiredCount == matchCount;
            }
        }
        Assert.assertTrue("signal entry for (" + algorithm + " " + parameterName + " " + parameterValue + ") not found",
            found);
    }
    
    /**
     * Tests the load shedding command on a pipeline source.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testLoadSheddingSource() throws IOException {
        testPipelineCommands(TestExecutionMode.LOAD_SHEDDING_SOURCE);        
    }

    /**
     * Tests the load shedding command on a pipeline source.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testLoadSheddingProcessor() throws IOException {
        testPipelineCommands(TestExecutionMode.LOAD_SHEDDING_PROCESSOR);        
    }

}
