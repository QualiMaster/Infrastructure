package tests.eu.qualimaster.coordination;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import backtype.storm.Config;
import backtype.storm.Testing;
import backtype.storm.testing.MkClusterParam;
import backtype.storm.testing.TestJob;
import eu.qualimaster.coordination.CoordinationConfiguration;
import eu.qualimaster.coordination.CoordinationManager;
import eu.qualimaster.coordination.CoordinationUtils;
import eu.qualimaster.coordination.IExecutionTracer;
import eu.qualimaster.coordination.RepositoryConnector;
import eu.qualimaster.coordination.RepositoryConnector.Models;
import eu.qualimaster.coordination.commands.AlgorithmChangeCommand;
import eu.qualimaster.coordination.commands.CommandSequence;
import eu.qualimaster.coordination.commands.CommandSet;
import eu.qualimaster.coordination.commands.CoordinationCommand;
import eu.qualimaster.coordination.commands.CoordinationExecutionResult;
import eu.qualimaster.coordination.commands.LoadSheddingCommand;
import eu.qualimaster.coordination.commands.MonitoringChangeCommand;
import eu.qualimaster.coordination.commands.ParallelismChangeCommand;
import eu.qualimaster.coordination.commands.ParameterChangeCommand;
import eu.qualimaster.coordination.commands.PipelineCommand;
import eu.qualimaster.coordination.commands.ReplayCommand;
import eu.qualimaster.coordination.commands.ScheduleWavefrontAdaptationCommand;
import eu.qualimaster.coordination.events.CoordinationCommandExecutionEvent;
import eu.qualimaster.events.EventHandler;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.PipelineStatusTracker;
import net.ssehub.easy.instantiation.core.model.buildlangModel.Script;
import tests.eu.qualimaster.TestHelper;
import tests.eu.qualimaster.storm.Naming;

/**
 * Abstract reusable test base for the coordination layer.
 * 
 * @author Holger Eichelberger
 */
public class AbstractCoordinationTests {

    private static final int TIMEOUT = 8000; // ms, loading models through repository connector
    private static final Set<String> JENKINS = new HashSet<String>();
    private TestTracer tracer;
    private CoordinationCommandExecutionEventHandler failedHandler;
    private PipelineStatusTracker tracker;
    
    static {
        JENKINS.add("jenkins.sse.uni-hildesheim.de");
    }
    
    /**
     * Runs a test job in a local Storm cluster.
     * 
     * @param job the test job
     */
    protected void testInLocalCluster(TestJob job) {
        //UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();
        //Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
        MkClusterParam mkClusterParam = new MkClusterParam();
        mkClusterParam.setSupervisors(1); // must be uneven
        Config daemonConf = new Config();
        daemonConf.put(Config.STORM_LOCAL_MODE_ZMQ, false);
        mkClusterParam.setDaemonConf(daemonConf);
        Testing.withSimulatedTimeLocalCluster(mkClusterParam, job);
        //Thread.setDefaultUncaughtExceptionHandler(handler);
        sleep(LocalStormEnvironment.WAIT_AT_END); // wait for shutting down cluster services
    }

    /**
     * Creates the topology configuration for the default topology.
     * 
     * @return the topology configuration
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Map createTopologyConfiguration() {
        Map stormConf = backtype.storm.utils.Utils.readStormConfig();
        stormConf.put(Config.TOPOLOGY_DEBUG, true);
        stormConf.put(Config.TOPOLOGY_WORKERS, 1);
        stormConf.put(Config.TOPOLOGY_MAX_TASK_PARALLELISM, 2);
        stormConf.put(Config.STORM_ZOOKEEPER_PORT, CoordinationConfiguration.getZookeeperPort());
        return stormConf;
    }
   
    /**
     * Implements a logging event handler for failed coordination commands.
     * 
     * @author Holger Eichelberger
     */
    protected class CoordinationCommandExecutionEventHandler extends EventHandler<CoordinationCommandExecutionEvent> {

        private List<CoordinationCommandExecutionEvent> successful = new ArrayList<CoordinationCommandExecutionEvent>();
        private List<CoordinationCommandExecutionEvent> failed = new ArrayList<CoordinationCommandExecutionEvent>();

        /**
         * Creates the handler.
         */
        public CoordinationCommandExecutionEventHandler() {
            super(CoordinationCommandExecutionEvent.class);
        }
        
        @Override
        protected void handle(CoordinationCommandExecutionEvent event) {
            System.out.println("RECEIVED " + event.isSuccessful() + " " + event);
            if (event.isSuccessful()) { 
                successful.add(event);
            } else {
                failed.add(event);
            }
        }
        
        /**
         * Clears the failed log.
         */
        public void clear() {
            failed.clear();            
            successful.clear();
        }
        
        /**
         * Returns the number of failed events received.
         * 
         * @return the number of failed events
         */
        public int getFailedCount() {
            return failed.size();
        }
        
        /**
         * Returns the number of successful events.
         * 
         * @return the number of successful events
         */
        public int getSuccessfulCount() {
            return successful.size();
        }
        
        @Override
        public String toString() {
            return "CommandExecutionHandler successful: " + successful + " failed: " + failed;
        }
        
    }
    
    /**
     * Implements a test tracer to follow and test the execution.
     * 
     * @author Holger Eichelberger
     */
    protected class TestTracer implements IExecutionTracer {

        private List<CoordinationCommand> commands = new ArrayList<CoordinationCommand>();
        private List<CoordinationExecutionResult> results = new ArrayList<CoordinationExecutionResult>();
        private int logEntryCount;

        /**
         * Returns the number of commands collected by this tracer.
         * 
         * @param filter the command types to be considered
         * @return the number of commands
         */
        public int getCommandCount(Class<?>... filter) {
            int count;
            if (0 == filter.length) {
                count = commands.size();
            } else {
                count = 0;
                for (int i = 0; i < commands.size(); i++) {
                    for (int f = 0; f < filter.length; f++) {
                        if (filter[f].isInstance(commands.get(i))) {
                            count++;
                        }
                    }
                }
            }
            return count;
        }
        
        /**
         * Returns the number of log entries collected.
         * 
         * @return the number of log entries
         */
        public int getLogEntryCount() {
            return logEntryCount;
        }
        
        /**
         * Clears this instance.
         */
        public void clear() {
            commands.clear();
            results.clear();
            logEntryCount = 0;
        }
        
        /**
         * Handles an executed command.
         * 
         * @param command the command
         * @param result the result
         */
        private void handle(CoordinationCommand command, CoordinationExecutionResult result) {
            System.out.println("RECEIVED COMMAND " + command);            
            commands.add(command);
            if (null != result) {
                results.add(result);
            }
        }
        
        /**
         * Returns whether the set of executed commands contains the given <code>command</code>.
         * 
         * @param command the command to search for
         * @return <code>true</code> if <code>command</code> was executed, <code>false</code> else
         */
        public boolean contains(CoordinationCommand command) {
            return commands.contains(command);
        }

        /**
         * Returns whether the set of executed commands contains the given commands and <code>first</code>
         * was executed before <code>second</code>.
         * 
         * @param first the first command search for
         * @param second the second command search for
         * @return <code>true</code> if <code>command</code> was executed, <code>false</code> else
         */
        public boolean before(CoordinationCommand first, CoordinationCommand second) {
            int pos1 = commands.indexOf(first);
            int pos2 = commands.indexOf(second);
            return pos1 >= 0 && pos1 < pos2;
        }
        
        @Override
        public void executedAlgorithmChangeCommand(AlgorithmChangeCommand command, CoordinationExecutionResult result) {
            handle(command, result);
        }

        @Override
        public void executedParameterChangeCommand(ParameterChangeCommand<?> command, 
            CoordinationExecutionResult result) {
            handle(command, result);
        }

        @Override
        public void executedCommandSequence(CommandSequence sequence, CoordinationExecutionResult result) {
            handle(sequence, result);
        }

        @Override
        public void executedCommandSet(CommandSet set, CoordinationExecutionResult result) {
            handle(set, result);
        }

        @Override
        public void executedPipelineCommand(PipelineCommand command, CoordinationExecutionResult result) {
            handle(command, result);
        }

        @Override
        public void executedScheduleWavefrontAdaptationCommand(ScheduleWavefrontAdaptationCommand command,
            CoordinationExecutionResult result) {
            handle(command, result);
        }

        @Override
        public void executedMonitoringChangeCommand(MonitoringChangeCommand command, 
            CoordinationExecutionResult result) {
            handle(command, result);
        }

        @Override
        public void logEntryWritten(String text) {
            logEntryCount++;
        }

        @Override
        public String toString() {
            return "Tracer " + commands + " " + results + " " + logEntryCount;
        }

        @Override
        public void executedParallelismChangeCommand(ParallelismChangeCommand command,
            CoordinationExecutionResult result) {
            handle(command, result);
        }

        @Override
        public void executedReplayCommand(ReplayCommand command, CoordinationExecutionResult result) {
            handle(command, result);
        }

        @Override
        public void executedLoadScheddingCommand(LoadSheddingCommand command, CoordinationExecutionResult result) {
            handle(command, result);
        }

    }
    
    /**
     * Executed before a single test.
     * 
     * @see #configure()
     */
    @Before
    public void setUp() {
        Naming.clearLogs();        
        configure();
        EventManager.start();
        failedHandler = new CoordinationCommandExecutionEventHandler();
        EventManager.register(failedHandler);
        tracker = new PipelineStatusTracker();
        EventManager.register(tracker);
        tracer = new TestTracer();
        CoordinationManager.setTracer(tracer);
        CoordinationManager.start();
    }

    /**
     * Configures the QM environment for testing. May be overridden by specific
     * test cases if required.
     */
    protected void configure() {
        Utils.configure(TestHelper.LOCAL_ZOOKEEPER_PORT); // as used by local cluster
    }
    
    /**
     * Executed after a single test.
     */
    @After
    public void tearDown() {
        CoordinationManager.stop();
        EventManager.unregister(tracker);
        EventManager.stop();
        EventManager.unregister(failedHandler);
    }
    
    /**
     * Returns the pipeline status tracer instance.
     * 
     * @return the status tracker instance
     */
    protected PipelineStatusTracker getPipelineStatusTracker() {
        return tracker;
    }
    
    /**
     * Returns the execution tracer.
     * 
     * @return the tracer
     */
    protected TestTracer getTracer() {
        return tracer;
    }
    
    /**
     * Returns the failed handler.
     * 
     * @return the failed handler
     */
    protected CoordinationCommandExecutionEventHandler getFailedHandler() {
        return failedHandler;
    }

    /**
     * Clears internal test-related information.
     */
    protected void clear() {
        tracer.clear();
        failedHandler.clear();
    }

    /**
     * Turns commands into a list of commands.
     * 
     * @param commands the commands to be turned into the list
     * @return the list of commands
     */
    protected static List<CoordinationCommand> toList(CoordinationCommand... commands) {
        List<CoordinationCommand> result = new ArrayList<CoordinationCommand>();
        for (CoordinationCommand cmd : commands) {
            result.add(cmd);
        }
        return result;
    }
    
    /**
     * Checks whether waiting shall be continued due to the command count.
     * 
     * @param expectedCommandCount the expected command count (ignored if not not positive)
     * @param filter the command types to be considered
     * @return <code>true</code> if waiting shall be continued, <code>false</code> else
     */
    protected boolean checkCommandCount(int expectedCommandCount, Class<?>... filter) {
        return expectedCommandCount <= 0 || tracer.getCommandCount(filter) < expectedCommandCount;
    }

    /**
     * Checks whether waiting shall be continued due to the failed count.
     * 
     * @param failedCount the expected failed count (ignored if not not positive)
     * @return <code>true</code> if waiting shall be continued, <code>false</code> else
     */
    protected boolean checkFailedCount(int failedCount) {
        return failedCount <= 0 || failedHandler.getFailedCount() < failedCount;
    }

    /**
     * Checks whether waiting shall be continued due to the overall {@link #TIMEOUT}.
     * 
     * @param start the start timestamp
     * @return <code>true</code> if waiting shall be continued, <code>false</code> else
     */
    protected boolean checkTimestamp(long start) {
        long now = System.currentTimeMillis();
        return (now - start) < TIMEOUT;
    }
    
    /**
     * Waits for the execution of commands. Stops after {@link #TIMEOUT}.
     * 
     * @param expectedCommandCount the number of commands to wait for (ignored if negative)
     * @param failedCount the number of failed enactments (ignored if not positive)
     * @param filter the command types to be considered
     */
    protected void waitForExecution(int expectedCommandCount, int failedCount, 
        Class<?>... filter) {
        waitForExecution(expectedCommandCount, failedCount, 0, filter);
    }

    /**
     * Waits for the execution of commands. Stops after {@link #TIMEOUT}.
     * 
     * @param expectedCommandCount the number of commands to wait for (ignored if negative)
     * @param failedCount the number of failed enactments (ignored if not positive)
     * @param sleepAfter additional time to wait
     * @param filter the command types to be considered
     */
    protected void waitForExecution(int expectedCommandCount, int failedCount, int sleepAfter,
        Class<?>... filter) {
        long timestamp = System.currentTimeMillis();
        while (checkTimestamp(timestamp) && checkCommandCount(expectedCommandCount, filter) 
            && checkFailedCount(failedCount)) {
            sleep(100);
        }
        if (sleepAfter > 0) {
            sleep(sleepAfter);
        }
    }

    /**
     * Sleeps the current thread for the given time.
     * 
     * @param millis the time to sleep
     */
    public static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

    /**
     * Tests loading the models.
     */
    public static void testLoadModels() {
        // prepare/register the model artifact - happens in setup
        Models models = CoordinationUtils.getCoordinationModels();
        net.ssehub.easy.varModel.confModel.Configuration config = models.getConfiguration();
        Assert.assertNotNull(config);
        Script script = models.getAdaptationScript();
        Assert.assertNotNull(script);

        String pipArtifact = RepositoryConnector.getPipelineArtifact(models, Naming.PIPELINE_NAME);
        Assert.assertEquals("eu.qualiMaster:TestPipeline:0.0.1", pipArtifact);
    }
    
    /**
     * Returns whether the running machine is identified as Jenkins and some tests shall not run.
     * 
     * @return <code>true</code> if the running machine is Jenkins, <code>false</code> else
     */
    public static boolean isJenkins() {
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            hostname = "localhost";
        }
        return JENKINS.contains(hostname);
    }
    
}
