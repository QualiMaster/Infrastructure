package tests.eu.qualimaster.coordination;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.storm.curator.utils.DebugUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import backtype.storm.Config;
import backtype.storm.Testing;
import backtype.storm.testing.MkClusterParam;
import backtype.storm.testing.TestJob;
import eu.qualimaster.common.signal.SignalMechanism;
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
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
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
     * A command filter interface.
     * 
     * @author Holger Eichelberger
     */
    public interface ICommandFilter {

        /**
         * Enables the given command.
         * 
         * @param command the command
         * @return <code>true</code> if <code>command</code> is enabled (shall be in), <code>false</code> else (for out)
         */
        public boolean enable(CoordinationCommand command);
    }

    /**
     * A class-based command filter.
     * 
     * @author Holger Eichelberger
     */
    public static class ClassBasedCommandFilter implements ICommandFilter {
        
        private Class<?>[] filter;

        /**
         * Creates an instance.
         * 
         * @param filter the classes that describe enabled commands
         */
        public ClassBasedCommandFilter(Class<?>... filter) {
            this.filter = filter;
        }

        @Override
        public boolean enable(CoordinationCommand command) {
            boolean enable = false;
            for (int f = 0; !enable && f < filter.length; f++) {
                if (filter[f].isInstance(command)) {
                    enable = true;
                }
            }
            return enable;
        }
        
    }

    /**
     * A command filter based on the pipeline status.
     * 
     * @author Holger Eichelberger
     */
    public static class PipelineCommandFilter implements ICommandFilter {
        
        private PipelineCommand.Status status;

        /**
         * Creates a filter instance.
         * 
         * @param status the status that shall be enabled (others will be ignored)
         */
        public PipelineCommandFilter(PipelineCommand.Status status) {
            this.status = status;
        }

        @Override
        public boolean enable(CoordinationCommand command) {
            boolean enable = false;
            if (command instanceof PipelineCommand) {
                enable = ((PipelineCommand) command).getStatus().equals(status);
            }
            return enable;
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
         * @param filter the filter to be considered (may be <b>null</b> for all)
         * @return the number of commands
         */
        public int getCommandCount(ICommandFilter filter) {
            int count;
            if (null == filter) {
                count = commands.size();
            } else {
                count = 0;
                for (int i = 0; i < commands.size(); i++) {
                    if (filter.enable(commands.get(i))) {
                        count++;
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
        System.setProperty(DebugUtils.PROPERTY_DONT_LOG_CONNECTION_ISSUES, "true");
        SignalMechanism.setTestMode(true);
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
        SignalMechanism.clear();
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
     * @param filter the command filter to be considered (may be <b>null</b> for all commands)
     * @return <code>true</code> if waiting shall be continued, <code>false</code> else
     */
    protected boolean checkCommandCount(int expectedCommandCount, ICommandFilter filter) {
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
     */
    protected void waitForExecution(int expectedCommandCount, int failedCount) {
        waitForExecution(expectedCommandCount, failedCount, null);
    }
    
    /**
     * Waits for the execution of commands. Stops after {@link #TIMEOUT}.
     * 
     * @param expectedCommandCount the number of commands to wait for (ignored if negative)
     * @param failedCount the number of failed enactments (ignored if not positive)
     * @param filter the filter to be considered (may be <b>null</b> for all commands)
     */
    protected void waitForExecution(int expectedCommandCount, int failedCount, ICommandFilter filter) {
        waitForExecution(expectedCommandCount, failedCount, 0, filter);
    }
    
    /**
     * Waits for the execution of commands. Stops after {@link #TIMEOUT}.
     * 
     * @param expectedCommandCount the number of commands to wait for (ignored if negative)
     * @param failedCount the number of failed enactments (ignored if not positive)
     * @param sleepAfter additional time to wait
     */
    protected void waitForExecution(int expectedCommandCount, int failedCount, int sleepAfter) {
        waitForExecution(expectedCommandCount, failedCount, sleepAfter, null);
    }
    
    /**
     * Waits for the execution of commands. Stops after {@link #TIMEOUT}.
     * 
     * @param expectedCommandCount the number of commands to wait for (ignored if negative)
     * @param failedCount the number of failed enactments (ignored if not positive)
     * @param sleepAfter additional time to wait
     * @param filter the filter to be considered (may be <b>null</b> for all commands)
     */
    protected void waitForExecution(int expectedCommandCount, int failedCount, int sleepAfter,
        ICommandFilter filter) {
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

    
    /**
     * Fakes a checked pipeline without adaptation layer.
     * 
     * @param pipeline the pipeline name
     */
    protected static void fakeCheckedPipeline(String pipeline) {
        sleep(500);
        EventManager.send(new PipelineLifecycleEvent(pipeline, 
            PipelineLifecycleEvent.Status.CHECKED, null)); // fake as we have no adaptation layer started
    }

    /**
     * Fakes a checked pipeline without monitoring/adaptation layer.
     * 
     * @param pipeline the pipeline name
     */
    protected static void fakeStartedPipeline(String pipeline) {
        sleep(500);
        EventManager.send(new PipelineLifecycleEvent(pipeline, 
            PipelineLifecycleEvent.Status.STARTED, null)); // fake as we have no adaptation layer started
    }
    
    /**
     * The handler for pipeline lifecycle events, in particular to send {@link CoordinationCommandExecutionEvent 
     * execution events} on completed lifecycle phase.
     * 
     * @author Holger Eichelberger
     */
    protected static class PipelineLifecycleEventHandler extends EventHandler<PipelineLifecycleEvent> {
        
        private PipelineLifecycleEvent.Status[] handle;
        private Map<PipelineLifecycleEvent.Status, Runnable> handlers = new HashMap<>();
        
        /**
         * Creates an adaptation event handler.
         * 
         * @param handle the static handled by this handler
         */
        protected PipelineLifecycleEventHandler(PipelineLifecycleEvent.Status... handle) {
            super(PipelineLifecycleEvent.class);
            this.handle = handle;
        }
        
        /**
         * Adds a status specific handler.
         * 
         * @param status the status
         * @param handler the handler
         */
        protected void addHandler(PipelineLifecycleEvent.Status status, Runnable handler) {
            handlers.put(status, handler);
        }

        @Override
        protected void handle(PipelineLifecycleEvent event) {
            Runnable r = handlers.get(event.getStatus());
            if (null != r) {
                r.run();
            }
            boolean found = false;
            for (int h = 0; !found && h < handle.length; h++) {
                found = event.getStatus() == handle[h];
            }
            if (found) {
                PipelineLifecycleEvent.Status next = null;
                switch (event.getStatus()) {
                case CHECKING:
                    next = PipelineLifecycleEvent.Status.CHECKED;
                    break;
                case CHECKED:
                    next = PipelineLifecycleEvent.Status.STARTING;
                    break;
                case STARTING:
                    next = PipelineLifecycleEvent.Status.CREATED;
                    break;
                case CREATED:
                    next = PipelineLifecycleEvent.Status.INITIALIZED;
                    break;
                case INITIALIZED:
                    next = PipelineLifecycleEvent.Status.STARTED;
                    break;
                case STARTED:
                    next = PipelineLifecycleEvent.Status.STOPPING;
                    break;
                case STOPPING:
                    next = PipelineLifecycleEvent.Status.STOPPED;
                    break;
                case STOPPED:
                    next = null;
                    break;
                default:
                    next = null;
                    break;
                }
                if (null != next) {
                    EventManager.send(new PipelineLifecycleEvent(event, next));
                }
            }
        }
        
    }

    
}
