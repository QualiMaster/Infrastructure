package tests.eu.qualimaster.coordination;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import tests.eu.qualimaster.storm.Naming;
import eu.qualimaster.common.shedding.DefaultLoadShedders;
import eu.qualimaster.common.shedding.DefaultLoadSheddingParameter;
import eu.qualimaster.coordination.CoordinationConfiguration;
import eu.qualimaster.coordination.CoordinationManager;
import eu.qualimaster.coordination.ParallelismChangeRequest;
import eu.qualimaster.coordination.commands.AlgorithmChangeCommand;
import eu.qualimaster.coordination.commands.CommandSequence;
import eu.qualimaster.coordination.commands.CommandSet;
import eu.qualimaster.coordination.commands.CoordinationCommand;
import eu.qualimaster.coordination.commands.CoordinationCommandNotifier;
import eu.qualimaster.coordination.commands.MonitoringChangeCommand;
import eu.qualimaster.coordination.commands.ParallelismChangeCommand;
import eu.qualimaster.coordination.commands.PipelineCommand;
import eu.qualimaster.coordination.commands.ReplayCommand;
import eu.qualimaster.coordination.commands.ScheduleWavefrontAdaptationCommand;
import eu.qualimaster.coordination.commands.CoordinationCommandNotifier.ICoordinationCommandNotifier;
import eu.qualimaster.coordination.commands.LoadSheddingCommand;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.infrastructure.PipelineOptions;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.MonitoringFrequency;
import eu.qualimaster.observables.TimeBehavior;
import eu.qualimaster.pipeline.AlgorithmChangeParameter;

/**
 * Coordination manager tests.
 */
public class ManagerTests extends AbstractCoordinationTests {
    
    /**
     * Implements a command notifier for testing.
     * 
     * @author Holger Eichelberger
     */
    private class CommandNotifier implements ICoordinationCommandNotifier {

        private Set<CoordinationCommand> events = new HashSet<CoordinationCommand>();
        
        /**
         * Creates a command notifier.
         */
        private CommandNotifier() {
            CoordinationCommandNotifier.addNotifier(this);
        }
        
        @Override
        public void notifySent(CoordinationCommand command) {
            events.add(command);
        }
        
        /**
         * Returns whether the given <code>command</code> was received.
         * 
         * @param command the command to check for
         * @return <code>true</code> if <code>command</code> was received, <code>false</code> else
         */
        public boolean received(CoordinationCommand command) {
            return events.remove(command);
        }
        
    }
    
    private CommandNotifier notifier = new CommandNotifier();
    
    /**
     * Executed before a single test.
     */
    @Before
    public void setUp() {
        Utils.setModelProvider(Utils.INFRASTRUCTURE_TEST_MODEL_PROVIDER);
        super.setUp();
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
     * Asserts that a notification for <code>command</code> was received.
     * 
     * @param command the command to checkf or
     */
    private void assertNotification(CoordinationCommand command) {
        Assert.assertTrue(notifier.received(command));
    }
    
    /**
     * Tests scheduling a wavefront adaptation.
     */
    @Test
    public void testScheduleWavefrontAdaptation() {
        clear();
        
        ScheduleWavefrontAdaptationCommand cmd 
            = new ScheduleWavefrontAdaptationCommand(Naming.PIPELINE_NAME, Naming.NODE_SINK); 
        cmd.execute();
        waitForExecution(2, 0);
        Assert.assertTrue(getTracer().contains(cmd));
        // not implemented!
        //Assert.assertEquals(1, tracer.getLogEntryCount());
        Assert.assertEquals(1, getFailedHandler().getFailedCount());
        Assert.assertEquals(0, getFailedHandler().getSuccessfulCount());
        assertNotification(cmd);
        clear();
    }
    
    /**
     * Tests the messages causing a change of parallelism.
     */
    @Test
    public void testParallelismChange() {
        clear();
        
        Map<String, Integer> executors = new HashMap<String, Integer>();
        executors.put("source", 4);
        executors.put("sink", 2);
        ParallelismChangeCommand cmd = new ParallelismChangeCommand("pip", 5, executors);
        cmd.execute();
        waitForExecution(1, 0);
        Assert.assertTrue(getTracer().contains(cmd));
        assertCoordinationResponse(0, 1);
        assertNotification(cmd);
        clear();
        
        Map<String, ParallelismChangeRequest> requests = new HashMap<String, ParallelismChangeRequest>();
        requests.put("source", new ParallelismChangeRequest(4));
        requests.put("sink", new ParallelismChangeRequest(3, "localhost"));
        cmd = new ParallelismChangeCommand("pip", requests);
        cmd.execute();
        waitForExecution(1, 0);
        Assert.assertTrue(getTracer().contains(cmd));
        assertCoordinationResponse(0, 1);
        assertNotification(cmd);
        clear();
    }
    
    /**
     * Tests the messages causing a change of monitoring.
     */
    @Test
    public void testMonitoringChange() {
        clear();
        
        Map<MonitoringFrequency, Integer> freq = new HashMap<MonitoringFrequency, Integer>();
        freq.put(MonitoringFrequency.CLUSTER_MONITORING, 100);
        MonitoringChangeCommand cmd = new MonitoringChangeCommand(freq);
        EventManager.handle(cmd); // no coordinationCommandNotification!
        waitForExecution(1, 0);
        Assert.assertTrue(getTracer().contains(cmd));
        Assert.assertEquals(1, getTracer().getLogEntryCount());
        assertCoordinationResponse(0, 1);
        clear();
        
        Map<IObservable, Boolean> obs = new HashMap<IObservable, Boolean>();
        obs.put(TimeBehavior.ENACTMENT_DELAY, true);
        cmd = new MonitoringChangeCommand(null, obs);
        EventManager.handle(cmd); // no coordinationCommandNotification!
        waitForExecution(1, 0);
        Assert.assertTrue(getTracer().contains(cmd));
        Assert.assertEquals(1, getTracer().getLogEntryCount());
        assertCoordinationResponse(0, 1);
        clear();

        cmd = new MonitoringChangeCommand(freq, obs);
        EventManager.handle(cmd); // no coordinationCommandNotification!
        waitForExecution(1, 0);
        Assert.assertTrue(getTracer().contains(cmd));
        Assert.assertEquals(1, getTracer().getLogEntryCount());
        assertCoordinationResponse(0, 1);
        clear();

        freq.put(MonitoringFrequency.PIPELINE_MONITORING, 200);
        cmd = new MonitoringChangeCommand(Naming.PIPELINE_NAME, freq, obs);
        EventManager.handle(cmd); // no coordinationCommandNotification!
        waitForExecution(1, 0);
        Assert.assertTrue(getTracer().contains(cmd));
        Assert.assertEquals(1, getTracer().getLogEntryCount());
        assertCoordinationResponse(0, 1);
        clear();

        cmd = new MonitoringChangeCommand(Naming.PIPELINE_NAME, Naming.NODE_PROCESS, freq, obs);
        EventManager.handle(cmd); // no coordinationCommandNotification!
        waitForExecution(1, 0);
        Assert.assertTrue(getTracer().contains(cmd));
        Assert.assertEquals(1, getTracer().getLogEntryCount());
        assertCoordinationResponse(0, 1);
        clear();
    }

    /**
     * Asserts a coordination response.
     * 
     * @param failed the number of expected failed commands
     * @param successful the number of expected successful commands
     */
    private void assertCoordinationResponse(int failed, int successful) {
        if (!AbstractCoordinationTests.isJenkins()) {
            EventManager.cleanup(); // ensure that response is processed
            Assert.assertEquals(failed, getFailedHandler().getFailedCount());
            Assert.assertEquals(successful, getFailedHandler().getSuccessfulCount());
        }
    }

    /**
     * Tests a command set.
     */
    @Test
    public void testCommandSet() {
        clear();
        ScheduleWavefrontAdaptationCommand cmd1 
            = new ScheduleWavefrontAdaptationCommand(Naming.PIPELINE_NAME, Naming.NODE_SINK); 
        MonitoringChangeCommand cmd2 = new MonitoringChangeCommand(
            MonitoringFrequency.createMap(MonitoringFrequency.CLUSTER_MONITORING, 100));
        MonitoringChangeCommand cmd3 = new MonitoringChangeCommand(MonitoringFrequency.createAllMap(0));
        CommandSet cmd = new CommandSet(cmd1, cmd2);
        assertCommandSet(cmd, cmd1, false, cmd2, true);
        Assert.assertNotNull(cmd.simplify());
        Assert.assertEquals(2, cmd.getCommandCount());
        clear();
        
        cmd = new CommandSet(toList(cmd2, cmd3));
        assertCommandSet(cmd, cmd2, true, cmd3, true);
        Assert.assertNotNull(cmd.simplify());
        Assert.assertEquals(2, cmd.getCommandCount());
        clear();
        
        cmd = new CommandSet();
        cmd.add(cmd2);
        cmd.add(cmd3);
        assertCommandSet(cmd, cmd2, true, cmd3, true);
        Assert.assertNotNull(cmd.simplify());
        Assert.assertEquals(2, cmd.getCommandCount());
        clear();
        
        cmd = new CommandSet();
        cmd.add(new CommandSet());
        Assert.assertNull(cmd.simplify()); // can be flattened completely
        Assert.assertEquals(0, cmd.getCommandCount());
        clear();
    }
    
    /**
     * Tests a command set.
     */
    @Test
    public void testCommandSetSimplify() {
        clear();
        ScheduleWavefrontAdaptationCommand cmd1 
            = new ScheduleWavefrontAdaptationCommand(Naming.PIPELINE_NAME, Naming.NODE_SINK); 
        MonitoringChangeCommand cmd2 = new MonitoringChangeCommand(MonitoringFrequency.createAllMap(100));
        
        CommandSet cmd = new CommandSet();
        cmd.add(new CommandSet());
        cmd.add(new CommandSet());
        Assert.assertNull(cmd.simplify()); // can be flattened completely
        Assert.assertEquals(0, cmd.getCommandCount());
        clear();
        
        cmd = new CommandSet();
        CommandSet tmp = new CommandSet();
        tmp.add(cmd1);
        cmd.add(tmp);
        cmd.add(new CommandSet());
        Assert.assertNotNull(cmd.simplify()); // can be flattened to the one element
        Assert.assertEquals(1, cmd.getCommandCount());
        Assert.assertEquals(cmd1, cmd.getCommand(0)); // was flattened
        clear();

        cmd = new CommandSet();
        tmp = new CommandSet();
        tmp.add(cmd1);
        cmd.add(tmp);
        tmp = new CommandSet();
        tmp.add(cmd2);
        cmd.add(tmp);
        cmd.add(new CommandSet());
        Assert.assertNotNull(cmd.simplify()); // can be flattened to the one element
        Assert.assertEquals(2, cmd.getCommandCount());
        Assert.assertEquals(cmd1, cmd.getCommand(0)); // was flattened
        Assert.assertEquals(cmd2, cmd.getCommand(1)); // was flattened
        clear();

        cmd = new CommandSet();
        tmp = new CommandSet();
        tmp.add(cmd1);
        tmp.add(cmd2);
        cmd.add(tmp);
        cmd.add(new CommandSet());
        Assert.assertNotNull(cmd.simplify()); // cannot be flattened
        Assert.assertEquals(1, cmd.getCommandCount());
        Assert.assertEquals(tmp, cmd.getCommand(0)); // nothing happened
        clear();
    }

    /**
     * Tests a command set.
     */
    @Test
    public void testCommandSequenceSimplify() {
        clear();
        ScheduleWavefrontAdaptationCommand cmd1 
            = new ScheduleWavefrontAdaptationCommand(Naming.PIPELINE_NAME, Naming.NODE_SINK); 
        MonitoringChangeCommand cmd2 = new MonitoringChangeCommand(MonitoringFrequency.createAllMap(100));
        
        CommandSequence cmd = new CommandSequence();
        cmd.add(new CommandSet()); // combine
        cmd.add(new CommandSequence());
        Assert.assertNull(cmd.simplify()); // can be flattened completely
        Assert.assertEquals(0, cmd.getCommandCount());
        clear();
        
        cmd = new CommandSequence();
        CommandSequence tmp = new CommandSequence();
        tmp.add(cmd1);
        cmd.add(tmp);
        cmd.add(new CommandSequence());
        Assert.assertNotNull(cmd.simplify()); // can be flattened to the one element
        Assert.assertEquals(1, cmd.getCommandCount());
        Assert.assertEquals(cmd1, cmd.getCommand(0)); // was flattened
        clear();

        cmd = new CommandSequence();
        tmp = new CommandSequence();
        tmp.add(cmd1);
        cmd.add(tmp);
        tmp = new CommandSequence();
        tmp.add(cmd2);
        cmd.add(tmp);
        cmd.add(new CommandSequence());
        Assert.assertNotNull(cmd.simplify()); // can be flattened to the one element
        Assert.assertEquals(2, cmd.getCommandCount());
        Assert.assertEquals(cmd1, cmd.getCommand(0)); // was flattened
        Assert.assertEquals(cmd2, cmd.getCommand(1)); // was flattened
        clear();

        cmd = new CommandSequence();
        tmp = new CommandSequence();
        tmp.add(cmd1);
        tmp.add(cmd2);
        cmd.add(tmp);
        cmd.add(new CommandSet());
        Assert.assertNotNull(cmd.simplify()); // cannot be flattened
        Assert.assertEquals(1, cmd.getCommandCount());
        Assert.assertEquals(tmp, cmd.getCommand(0)); // nothing happened
        clear();
    }

    /**
     * Executes and asserts the command set.
     * 
     * @param cmd the command set
     * @param cmd1 the first command in the set
     * @param successful1 whether the first command shall be successful
     * @param cmd2 the second command in the set
     * @param successful2 whether the second command shall be successful
     */
    private void assertCommandSet(CommandSet cmd, CoordinationCommand cmd1, boolean successful1, 
        CoordinationCommand cmd2, boolean successful2) {
        EventManager.handle(cmd); // no coordinationCommandNotification!
        waitForExecution(2 * 3, 0);
        Assert.assertFalse(cmd.keepOrdering());
        Assert.assertTrue(getTracer().contains(cmd));
        int failedCount = 0;
        boolean tmp = getTracer().contains(cmd1);
        if (!successful1) {
            failedCount++;
        }
        Assert.assertTrue(tmp);
        if (successful1) { // stops immediately
            tmp = getTracer().contains(cmd2);
            if (!successful2) {
                tmp = !tmp;
                failedCount++;
            }
            Assert.assertTrue(tmp);
        }
        Assert.assertEquals(1, getTracer().getLogEntryCount());
        Assert.assertEquals(failedCount, getFailedHandler().getFailedCount());
        Assert.assertEquals(successful1 && successful2 ? 1 : 0, getFailedHandler().getSuccessfulCount());
    }
    
    /**
     * Tests a command set.
     */
    @Test
    public void testCommandSequence() {
        clear();
        
        MonitoringChangeCommand cmd1 = new MonitoringChangeCommand(MonitoringFrequency.createAllMap(0)); 
        MonitoringChangeCommand cmd2 = new MonitoringChangeCommand(MonitoringFrequency.createAllMap(100));
        CommandSequence cmd = new CommandSequence(cmd1, cmd2);
        assertCommandSequence(cmd, cmd1, cmd2);
        // no result is returned
        clear();
        
        cmd = new CommandSequence(toList(cmd1, cmd2));
        assertCommandSequence(cmd, cmd1, cmd2);
        // no result is returned
        clear();

        cmd = new CommandSequence();
        cmd.add(cmd1);
        cmd.add(cmd2);
        assertCommandSequence(cmd, cmd1, cmd2);
        // no result is returned
        clear();
    }
    
    /**
     * Executes and asserts the command sequence.
     * 
     * @param cmd the command sequence
     * @param cmd1 the first command in the sequence
     * @param cmd2 the second command in the sequence
     */    
    private void assertCommandSequence(CommandSequence cmd, CoordinationCommand cmd1, CoordinationCommand cmd2) {
        EventManager.handle(cmd); // no coordinationCommandNotification!
        waitForExecution(2 * 3, 0);
        Assert.assertTrue(cmd.keepOrdering());
        Assert.assertTrue(getTracer().contains(cmd));
        Assert.assertTrue(getTracer().contains(cmd1));
        Assert.assertTrue(getTracer().contains(cmd2));
        Assert.assertTrue(getTracer().before(cmd1, cmd2));
        Assert.assertEquals(1, getTracer().getLogEntryCount()); // scheduleAdaptation is not implemented
        assertCoordinationResponse(0, 1);
    }
    
    /**
     * Tests obtaining a model artifact.
     */
    @Test
    public void testRegistryConnectorModels() {
        testLoadModels();
    }
    
    /**
     * Tests the pipeline command / options.
     */
    @Test
    public void testPipelineCommand() {
        PipelineCommand cmd = new PipelineCommand("pipeline", PipelineCommand.Status.START);
        Assert.assertEquals("pipeline", cmd.getPipeline());
        Assert.assertEquals(PipelineCommand.Status.START, cmd.getStatus());
        Assert.assertEquals(new PipelineOptions(), cmd.getOptions());
        
        PipelineOptions options = new PipelineOptions();
        options.setNumberOfWorkers(5);
        options.setTaskParallelism("exec", 4);
        options.setWaitTime(10);
        cmd = new PipelineCommand("pipeline", PipelineCommand.Status.START, options);
        Assert.assertEquals(options, cmd.getOptions());
        
        cmd.execute();
        sleep(500);
        EventManager.send(new PipelineLifecycleEvent(Naming.PIPELINE_NAME, 
            PipelineLifecycleEvent.Status.CHECKED, null)); // fake as we have no adaptation layer started
        waitForExecution(2, 0); // + response
        if (!AbstractCoordinationTests.isJenkins()) {
            Assert.assertTrue(getTracer().contains(cmd));
        }
        assertNotification(cmd);
        clear();
    }
    
    /**
     * Tests the algorithm change command.
     */
    @Test
    public void testAlgorithmChangeCommand() {
        final String pipeline = "pipeline";
        final String element = "elt";
        final String algorithm = "alg";
        
        final int inPort = 1234;
        final int outPort = 4321;
        final int warmup = 50;
        final String host = "localhost";
        
        AlgorithmChangeCommand cmd = new AlgorithmChangeCommand(pipeline, element, algorithm);
        Assert.assertEquals(pipeline, cmd.getPipeline());
        Assert.assertEquals(element, cmd.getPipelineElement());
        Assert.assertEquals(algorithm, cmd.getAlgorithm());

        cmd.setIntParameter(AlgorithmChangeParameter.INPUT_PORT, inPort);
        cmd.setIntParameter(AlgorithmChangeParameter.OUTPUT_PORT, outPort);
        cmd.setIntParameter(AlgorithmChangeParameter.WARMUP_DELAY, warmup);
        cmd.setStringParameter(AlgorithmChangeParameter.COPROCESSOR_HOST, host);
        
        assertEquals(inPort, cmd.getIntParameter(AlgorithmChangeParameter.INPUT_PORT, null));
        assertEquals(outPort, cmd.getIntParameter(AlgorithmChangeParameter.OUTPUT_PORT, null));
        assertEquals(warmup, cmd.getIntParameter(AlgorithmChangeParameter.WARMUP_DELAY, null));
        assertEquals(host, cmd.getStringParameter(AlgorithmChangeParameter.COPROCESSOR_HOST, null));

        // execution happens in other tests, data does not fit to (not-running) pipeline
        clear();
    }

    /**
     * Asserts that <code>expected</code> and actual are equal.
     * 
     * @param expected the expected value
     * @param actual the actual value
     */
    private static void assertEquals(int expected, Integer actual) {
        Assert.assertNotNull(actual);
        Assert.assertEquals(expected, actual.intValue());
    }

    /**
     * Asserts that <code>expected</code> and actual are equal.
     * 
     * @param expected the expected value
     * @param actual the actual value
     */
    private static void assertEquals(String expected, String actual) {
        Assert.assertNotNull(actual);
        Assert.assertEquals(expected, actual);
    }

    
    /**
     * Tests a load shedding command.
     */
    @Test
    public void testLoadShedding() {
        clear();
        
        LoadSheddingCommand cmd 
            = new LoadSheddingCommand(Naming.PIPELINE_NAME, Naming.NODE_SINK, DefaultLoadShedders.NTH_ITEM); 
        cmd.setIntParameter(DefaultLoadSheddingParameter.NTH_TUPLE, 25);
        cmd.execute();
        waitForExecution(2, 0);
        Assert.assertTrue(getTracer().contains(cmd));
        // not implemented!
        //Assert.assertEquals(1, tracer.getLogEntryCount());
        Assert.assertEquals(0, getFailedHandler().getFailedCount());
        Assert.assertEquals(1, getFailedHandler().getSuccessfulCount());
        assertNotification(cmd);
        clear();
    }

    /**
     * Tests a load shedding command.
     */
    @Test
    public void testDataReplay() {
        clear();
        
        ReplayCommand cmd = new ReplayCommand(Naming.PIPELINE_NAME, Naming.NODE_SINK, false, 1); 
        cmd.execute();
        waitForExecution(2, 0);
        Assert.assertTrue(getTracer().contains(cmd));
        // not implemented!
        //Assert.assertEquals(1, tracer.getLogEntryCount());
        Assert.assertEquals(0, getFailedHandler().getFailedCount());
        Assert.assertEquals(1, getFailedHandler().getSuccessfulCount());
        assertNotification(cmd);
        clear();
    }

    /**
     * Manual test for unpacking and copying the pipeline settings. Loading the model may fail if QM.Extensions is not
     * available, but this is not relevant for this test.
     * 
     * @param args ignored
     */
    public static void main(String[] args) {
        Properties prop = new Properties();
        prop.put(CoordinationConfiguration.PIPELINE_ELEMENTS_REPOSITORY, 
            "https://projects.sse.uni-hildesheim.de/qm/maven/");
        prop.put(CoordinationConfiguration.CONFIG_MODEL_ARTIFACT_SPEC, 
            "eu.qualimaster:infrastructureModel:0.5.0-SNAPSHOT");
        prop.put(CoordinationConfiguration.PATH_DFS, FileUtils.getTempDirectoryPath());
        prop.put(CoordinationConfiguration.PIPELINE_SETTINGS_LOCATION, "pipSettings");
        CoordinationConfiguration.configure(prop, false);
        CoordinationManager.start();
        CoordinationManager.stop();
    }

}
