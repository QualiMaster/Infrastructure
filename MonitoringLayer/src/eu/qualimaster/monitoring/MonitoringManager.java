package eu.qualimaster.monitoring;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import eu.qualimaster.adaptation.events.AdaptationEvent;
import eu.qualimaster.common.monitoring.MonitoringPluginRegistry;
import eu.qualimaster.coordination.CoordinationManager;
import eu.qualimaster.coordination.INameMapping;
import eu.qualimaster.coordination.RepositoryConnector;
import eu.qualimaster.coordination.RepositoryConnector.Models;
import eu.qualimaster.coordination.RepositoryConnector.Phase;
import eu.qualimaster.coordination.commands.CoordinationCommand;
import eu.qualimaster.coordination.events.AlgorithmProfilingEvent;
import eu.qualimaster.coordination.events.CoordinationCommandExecutionEvent;
import eu.qualimaster.dataManagement.events.ShutdownEvent;
import eu.qualimaster.easy.extension.QmConstants;
import eu.qualimaster.easy.extension.internal.VariableHelper;
import eu.qualimaster.events.EventHandler;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent.Status;
import eu.qualimaster.monitoring.AbstractMonitoringTask.IPiggybackTask;
import eu.qualimaster.monitoring.ReasoningTask.IReasoningModelProvider;
import eu.qualimaster.monitoring.ReasoningTask.PhaseReasoningModelProvider;
import eu.qualimaster.monitoring.events.ChangeMonitoringEvent;
import eu.qualimaster.monitoring.events.MonitoringEvent;
import eu.qualimaster.monitoring.handlers.AlgorithmChangedMonitoringEventHandler;
import eu.qualimaster.monitoring.handlers.AlgorithmMonitoringEventHandler;
import eu.qualimaster.monitoring.handlers.CloudResourceMonitoringEventHandler;
import eu.qualimaster.monitoring.handlers.ParameterChangedMonitoringEventHandler;
import eu.qualimaster.monitoring.handlers.PipelineElementMultiObservationMonitoringEventHandler;
import eu.qualimaster.monitoring.handlers.PipelineElementObservationMonitoringEventHandler;
import eu.qualimaster.monitoring.handlers.PlatformMonitoringEventHandler;
import eu.qualimaster.monitoring.handlers.PlatformMultiMonitoringHostEventHandler;
import eu.qualimaster.monitoring.handlers.SourceVolumeMonitoringEventHandler;
import eu.qualimaster.monitoring.handlers.SubTopologyMonitoringEventHandler;
import eu.qualimaster.monitoring.profiling.AlgorithmProfilePredictor;
import eu.qualimaster.monitoring.storm.StormMonitoringPlugin;
import eu.qualimaster.monitoring.systemState.PipelineSystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.monitoring.tracing.Tracing;
import eu.qualimaster.monitoring.tracing.TracingTask;
import eu.qualimaster.monitoring.utils.IScheduler;
import eu.qualimaster.monitoring.volumePrediction.VolumePredictionManager;
import eu.qualimaster.observables.MonitoringFrequency;
import net.ssehub.easy.varModel.confModel.Configuration;
import net.ssehub.easy.varModel.confModel.IDecisionVariable;
import net.ssehub.easy.varModel.model.AbstractVariable;
import net.ssehub.easy.varModel.model.ModelQuery;
import net.ssehub.easy.varModel.model.ModelQueryException;

/**
 * The external interface to monitor execution.
 * 
 * @author Nick Pavlakis
 * @author Holger Eichelberger
 */
public class MonitoringManager {

    public static final int DEMO_MSG_INFRASTRUCTURE = 0x00000001;
    public static final int DEMO_MSG_PIPELINE = 0x00000002;
    public static final int DEMO_MSG_PROCESSING_ELEMENT = 0x00000004;
    public static final int DEMO_MSG_PROCESSING_ALGORITHM = 0x00000008;
    
    public static final int MINIMUM_MONITORING_FREQUENCY = 200; // ms
    public static final int REASONING_FREQUENCY = 1000; // ms
    private static final String CLUSTER_TASK_NAME = "";

    private static final Logger LOGGER = LogManager.getLogger(MonitoringManager.class);
    private static Timer timer;
    private static List<IMonitoringPlugin> plugins = new ArrayList<IMonitoringPlugin>();
    private static Map<IMonitoringPlugin, Map<String, AbstractMonitoringTask>> tasks = 
        Collections.synchronizedMap(new HashMap<IMonitoringPlugin, Map<String, AbstractMonitoringTask>>());
    private static Map<Class<? extends MonitoringEvent>, MonitoringEventHandler<?>> handlers = 
        new HashMap<Class<? extends MonitoringEvent>, MonitoringEventHandler<?>>();
    private static Map<String, List<MonitoringEvent>> deferred 
        = Collections.synchronizedMap(new HashMap<String, List<MonitoringEvent>>());
    private static SystemState state = new SystemState();
    private static int demoMessages = DEMO_MSG_INFRASTRUCTURE | DEMO_MSG_PIPELINE 
        | DEMO_MSG_PROCESSING_ALGORITHM // currently the default
        | DEMO_MSG_PROCESSING_ELEMENT; // for init testing
    private static List<URLClassLoader> loaders = new ArrayList<URLClassLoader>();

    private static ReasoningTask reasoningTask;
    private static int runningPipelines = 0;
    
    private static IScheduler scheduler = new IScheduler() {
        
        @Override
        public void schedule(TimerTask task, Date firstTime, long period) {
            timer.schedule(task, firstTime, period);
        }

    };

    /**
     * Prevents external creation.
     */
    private MonitoringManager() {
    }
    
    /**
     * Implements the handling of {@link ChangeMonitoringEvent}.
     * 
     * @author Holger Eichelberger
     */
    private static class ChangeMonitoringEventHandler extends MonitoringEventHandler<ChangeMonitoringEvent> {

        public static final ChangeMonitoringEventHandler INSTANCE = new ChangeMonitoringEventHandler();

        /**
         * Creates an instance.
         */
        private ChangeMonitoringEventHandler() {
            super(ChangeMonitoringEvent.class);
        }

        @Override
        protected void handle(ChangeMonitoringEvent event, SystemState state) {
            if (null == event.getMessageId() && null != event.enableAlgorithmTracing()) {
                // shall be internal
                state.enableAlgorithmTracing(event.enableAlgorithmTracing());
            } else {
                for (IMonitoringPlugin plugin : plugins) {
                    Map<String, AbstractMonitoringTask> pluginTasks = tasks.get(plugin);
                    if (null != pluginTasks) {
                        reschedule(pluginTasks, CLUSTER_TASK_NAME, event, MonitoringFrequency.CLUSTER_MONITORING);
                        reschedule(pluginTasks, event.getPipeline(), event, MonitoringFrequency.PIPELINE_MONITORING);
                    }
                }
                // TODO if failed send ChangeMonitoringFailedAdaptationEvent
            }
        }

        /**
         * Reschedules the task <code>taskName</code> in <code>pluginTask</code> for <code>frequency</code> stated by
         * <code>event</code>.
         * 
         * @param pluginTasks the tasks
         * @param taskName the task name (may be <b>null</b>, ignored)
         * @param event the change event
         * @param frequency the frequency kind (may be <b>null</b>, ignored)
         */
        private void reschedule(Map<String, AbstractMonitoringTask> pluginTasks, String taskName, 
            ChangeMonitoringEvent event, MonitoringFrequency frequency) {
            if (null != taskName && null != frequency) {
                Integer freq = event.getFrequency(frequency);
                if (null != freq) {
                    AbstractMonitoringTask task = pluginTasks.get(taskName);
                    task.reschedule(freq);
                }
            }
        }

    }
    
    /**
     * A handler for monitoring events.
     * 
     * @author Holger Eichelberger
     */
    private static class TopMonitoringEventHandler extends EventHandler<MonitoringEvent> {

        /**
         * Creates an event handler instance.
         */
        protected TopMonitoringEventHandler() {
            super(MonitoringEvent.class);
        }

        @Override
        protected void handle(MonitoringEvent event) {
            MonitoringManager.handleEvent(event);
        }
        
    }

    /**
     * A handler for coordination command execution events.
     * 
     * @author Holger Eichelberger
     */
    private static class CoordinationCommandExecutionEventHandler 
        extends EventHandler<CoordinationCommandExecutionEvent> {

        /**
         * Creates an event handler instance.
         */
        protected CoordinationCommandExecutionEventHandler() {
            super(CoordinationCommandExecutionEvent.class);
        }

        @Override
        protected void handle(CoordinationCommandExecutionEvent event) {
            EnactingPipelineElements.INSTANCE.handle(event.getCommand(), false);
        }
        
    }
    
    /**
     * The handler for coordination command events (if not passed in directly as commands).
     * 
     * @author Holger Eichelberger
     */
    private static class CoordinationCommandEventHandler extends EventHandler<CoordinationCommand> {

        /**
         * Creates an coordination command event handler.
         */
        protected CoordinationCommandEventHandler() {
            super(CoordinationCommand.class);
        }

        @Override
        protected void handle(CoordinationCommand command) {
            EnactingPipelineElements.INSTANCE.handle(command, true);
        }
        
    }

    /**
     * Handles starting the given pipeline.
     * 
     * @param event the causing event
     */
    private static void handleStarting(PipelineLifecycleEvent event) {
        String pipelineName = event.getPipeline();
        if (null != pipelineName) {
            for (IMonitoringPlugin plugin : plugins) {
                handleStarting(event, plugin);
            }
            PipelineSystemPart pipeline = state.obtainPipeline(pipelineName); // cache pipeline
            pipeline.changeStatus(PipelineLifecycleEvent.Status.STARTING, false, null, event);
            List<MonitoringEvent> evt = deferred.remove(pipelineName);
            if (null != evt) {
                for (int e = 0; e < evt.size(); e++) {
                    handleEvent(evt.get(e));
                }
            }
        } else {
            System.out.println("Illegal event lifecycle event [STARTING]. Pipeline null!");
        }
    }

    /**
     * Handles starting the pipeline in <code>event</code> for the monitoring plugin <code>plugin</code>. If a 
     * pipeline still exists but is not being monitored as it disappeared, it will be cleaned up here.
     * 
     * @param event the event causing the start
     * @param plugin the plugin to handle the starting for
     */
    private static void handleStarting(PipelineLifecycleEvent event, IMonitoringPlugin plugin) {
        Map<String, AbstractMonitoringTask> pluginTasks = tasks.get(plugin);
        String pipelineName = event.getPipeline();
        Class<? extends AdaptationEvent> adaptationFilter = event.getAdaptationFilter();
        boolean createTask = true;
        if (null != pluginTasks) {
            AbstractMonitoringTask mTask = pluginTasks.get(pipelineName);
            if (null != mTask) {
                // pipeline seems to exist
                createTask = false;
                PipelineSystemPart pPart = getSystemState().getPipeline(pipelineName);
                if (null != pPart && (pPart.isShuttingDown() || Status.DISAPPEARED == pPart.getStatus())) {
                    // pipeline was there already, state is outdated, delete and start over again
                    // uhh - pipeline is disappeared but shall be started - stop monitoring
                    createTask = true;
                    mTask.cancel();
                    pluginTasks.remove(pipelineName);
                    state.removePipeline(pipelineName);
                }
            } // pipeline monitoring not enabled - do it now
        }
        if (createTask) {
            AbstractContainerMonitoringTask task = plugin.createPipelineTask(pipelineName, state, 
                adaptationFilter);
            if (null != task) {
                task.add(new TracingTask(task));
                if (null == pluginTasks) {
                    pluginTasks = new HashMap<String, AbstractMonitoringTask>();
                    tasks.put(plugin, pluginTasks);
                } 
                pluginTasks.put(pipelineName, task);
                timer.schedule(task, 0, Math.max(MINIMUM_MONITORING_FREQUENCY, task.getFrequency())); 
            } // else no task created, ignore
        }
    }
    
    /**
     * Returns a specific piggyback task for a pipeline.
     * 
     * @param <T> the type of piggyback task
     * @param pipelineName the pipeline name
     * @param cls the type of piggyback tasks to return
     * @return the (first) specified piggyback task or <b>null</b> if there is none
     */
    private static <T extends IPiggybackTask> T getPiggybackTask(String pipelineName, Class<T> cls) {
        T result = null;
        if (null != cls) {
            for (IMonitoringPlugin plugin : plugins) {
                Map<String, AbstractMonitoringTask> pluginTasks = tasks.get(plugin);
                AbstractMonitoringTask task = pluginTasks.get(pipelineName);
                if (null != task) {
                    for (int p = 0; null == result && p < task.getPiggybackTaskCount(); p++) {
                        IPiggybackTask pTask = task.getPiggybackTask(p);
                        if (cls.isInstance(pTask)) {
                            result = cls.cast(pTask);
                        }
                    }
                    if (null != result) {
                        break;
                    }
                }
            }
        }
        return result;
    }
    
    /**
     * Returns the first monitoring task dedicated to <code>pipelineName</code>.
     * 
     * @param pipelineName the pipeline name
     * @return the monitoring task or <b>null</b> if there is none
     */
    private static AbstractMonitoringTask getFirstMonitoringTask(String pipelineName) {
        AbstractMonitoringTask result = null;
        for (IMonitoringPlugin plugin : plugins) {
            Map<String, AbstractMonitoringTask> pluginTasks = tasks.get(plugin);
            if (null != pluginTasks) {
                AbstractMonitoringTask task = pluginTasks.get(pipelineName);
                if (null != task) {
                    result = task;
                    break;
                }
            }
        }
        return result;
    }
    
    /**
     * Handles stopping the given pipeline.
     * 
     * @param event the causing event
     */
    private static void handleStopping(PipelineLifecycleEvent event) {
        String pipelineName = event.getPipeline();
        if (null != pipelineName) {
            // don't remove the pipeline - keep state
            PipelineSystemPart pipeline = state.getPipeline(pipelineName);
            if (null != pipeline) {
                pipeline.changeStatus(PipelineLifecycleEvent.Status.STOPPING, false, null, event);
            }
            for (IMonitoringPlugin plugin : plugins) {
                Map<String, AbstractMonitoringTask> pluginTasks = tasks.get(plugin);
                if (null != pluginTasks) {
                    AbstractMonitoringTask task = pluginTasks.get(pipelineName);
                    if (null != task) {
                        task.cancel();
                        pluginTasks.remove(pipelineName);
                    }
                } // else ignore
            }
        } else {
            LOGGER.info("Illegal event lifecycle event [STOPPING]. Pipeline null!");
        }
    }

    /**
     * The handler for monitoring events.
     * 
     * @author Holger Eichelberger
     */
    private static class PipelineLifecycleEventEventHandler extends EventHandler<PipelineLifecycleEvent> {

        /**
         * Creates an adaptation event handler.
         */
        protected PipelineLifecycleEventEventHandler() {
            super(PipelineLifecycleEvent.class);
        }

        @Override
        protected void handle(PipelineLifecycleEvent event) {
            if (null == timer) {
                LOGGER.error("Monitoring Manager not started properly! Call start before!"); 
            } else {
                switch (event.getStatus()) {
                case STARTING:
                    handleStarting(event);
                    break;
                case STOPPING:
                    Tracing.logMonitoringData(state, event.getPipeline());
                    handleStopping(event);
                    runningPipelines--;
                    break;
                case STARTED:
                    runningPipelines++;
                    // fallthrough
                case STOPPED:
                    PipelineSystemPart pipeline = state.obtainPipeline(event.getPipeline());
                    pipeline.changeStatus(event.getStatus(), false, null);
                    break;
                default:
                    // UNKNOWN is the default and shall not be send through a signal
                    // DISAPPEARED, CREATED, INITIALIZED are assigned during monitoring 
                    break;
                }
            }
            
            if (MonitoringConfiguration.isReasoningEnabled()) {
                if (0 == runningPipelines && null != reasoningTask) {
                    reasoningTask.cancel();
                } else if (runningPipelines > 0 && null == reasoningTask) {
                    IReasoningModelProvider modelProvider = new PhaseReasoningModelProvider(Phase.MONITORING);
                    if (null != modelProvider.getConfiguration() && null != modelProvider.getScript()) {
                        reasoningTask = new ReasoningTask(modelProvider);     
                        timer.schedule(reasoningTask, 0, REASONING_FREQUENCY);
                    } else {
                        LOGGER.error("Monitoring model not loaded - cannot monitor pipelines");
                    }
                }
            }
            AlgorithmProfilePredictor.notifyPipelineLifecycleChange(event);
            VolumePredictionManager.notifyPipelineLifecycleChange(event);
        }
        
    }
    
    /**
     * Handles a shutdown event.
     * 
     * @author Holger Eichelberger
     */
    private static class ShutdownEventHandler extends EventHandler<ShutdownEvent> {

        /**
         * Creates a handler instance.
         */
        protected ShutdownEventHandler() {
            super(ShutdownEvent.class);
        }

        @Override
        protected void handle(ShutdownEvent event) {
            MonitoringManager.stop();
        }
        
    }

    /**
     * Handles an algorithm profiling event.
     * 
     * @author Holger Eichelberger
     */
    private static class AlgorithmProfilingEventHandler extends EventHandler<AlgorithmProfilingEvent> {

        /**
         * Creates a handler instance.
         */
        protected AlgorithmProfilingEventHandler() {
            super(AlgorithmProfilingEvent.class);
        }

        @Override
        protected void handle(AlgorithmProfilingEvent event) {
            final Logger logger = LogManager.getLogger(MonitoringManager.class);
            final String pipelineName = event.getPipeline();
            final String msgPrefix = "Handling algorithm profiling event: ";
            Tracing.handleEvent(event);
            TracingTask tTask = getPiggybackTask(pipelineName, TracingTask.class);
            switch (event.getStatus()) {
            case START:
                if (null == tTask) {
                    AbstractMonitoringTask mTask = getFirstMonitoringTask(pipelineName);
                    if (null != mTask) {
                        mTask.add(new TracingTask(mTask));
                        logger.info(msgPrefix + " Added tracing task for pipeline " + pipelineName);
                    } else {
                        logger.error(msgPrefix + "No monitoring task for pipeline " + pipelineName);
                    }
                } else {
                    tTask.incrementUsageCount();
                }
                break;
            case NEXT:
                // nothing to do
                break;
            case END:
                if (null != tTask) {
                    tTask.decrementUsageCount();
                    if (0 == tTask.getUsageCount()) {
                        tTask.getParent().remove(tTask);
                        logger.info(msgPrefix + " Removed tracing task for pipeline " + pipelineName);
                    }
                }
                break;
            default:
                break;
            }
            logger.info(msgPrefix + " done " + event);
            AlgorithmProfilePredictor.notifyAlgorithmProfilingEvent(event);
        }
        
    }
    
    /**
     * Registers an internal monitoring event handler.
     * 
     * @param <E> the event type
     * @param handler the handler instance
     */
    private static <E extends MonitoringEvent> void register(MonitoringEventHandler<E> handler) {
        handlers.put(handler.handles(), handler);
    }
    
    /**
     * Register the event handlers statically.
     */
    static {
        register(AlgorithmChangedMonitoringEventHandler.INSTANCE);
        register(AlgorithmMonitoringEventHandler.INSTANCE);
        register(ChangeMonitoringEventHandler.INSTANCE);
        register(PipelineElementMultiObservationMonitoringEventHandler.INSTANCE);
        register(PipelineElementObservationMonitoringEventHandler.INSTANCE);
        register(PlatformMonitoringEventHandler.INSTANCE);
        register(PlatformMultiMonitoringHostEventHandler.INSTANCE);
        register(SubTopologyMonitoringEventHandler.INSTANCE);
        register(CloudResourceMonitoringEventHandler.INSTANCE);
        register(ParameterChangedMonitoringEventHandler.INSTANCE);
        register(SourceVolumeMonitoringEventHandler.INSTANCE);
        
        EventManager.register(new TopMonitoringEventHandler());
        EventManager.register(new PipelineLifecycleEventEventHandler());
        EventManager.register(new CoordinationCommandExecutionEventHandler());
        EventManager.register(new CoordinationCommandEventHandler());
        EventManager.register(new ShutdownEventHandler());
        EventManager.register(new AlgorithmProfilingEventHandler());
    }

    /**
     * Changes the monitoring according to the event.
     * 
     * @param event the monitoring event (<b>null</b> is ignored)
     */
    public static void handleEvent(MonitoringEvent event) {
        if (null != event) {
            MonitoringEventHandler<?> handler = handlers.get(event.getClass());
            if (null != handler) {
                handler.doHandle(event, state);
            } else {
                LOGGER.error("no monitoring event handler for " + event.getClass().getName() + " registered");
            }
        }
    }

    /**
     * Start the layer.
     * 
     * @see #start(boolean)
     */
    public static void start() {
        start(true);
    }
    
    /**
     * Start the layer (public for testing).
     * 
     * @param registerDefaultPlugins if the default monitoring plugins shall be registered
     * 
     * @see #registerDefaultPlugins()
     */
    public static void start(boolean registerDefaultPlugins) {
        if (registerDefaultPlugins) {
            registerDefaultPlugins();
        }
        loadMonitoringPlugins();
        timer = new Timer();
        for (IMonitoringPlugin plugin : plugins) {
            startPlugin(plugin);
        }
        AlgorithmProfilePredictor.start();
        VolumePredictionManager.start(scheduler);
    }
    
    /**
     * Loads configured monitoring plugins.
     */
    private static void loadMonitoringPlugins() {
        String error = null;
        Models models = RepositoryConnector.getModels(Phase.MONITORING);
        if (null != models) {
            Configuration config = models.getConfiguration();
            try {
                AbstractVariable var = ModelQuery.findVariable(config.getProject(), 
                    QmConstants.VAR_OBSERVABLES_CONFIGUREDPARAMS, null);
                if (null != var) {
                    IDecisionVariable dec = config.getDecision(var);
                    if (null != dec) {
                        error = loadMonitoringPlugins(dec);
                    }
                } else {
                    error = "Cannot find configuration variable " + QmConstants.VAR_OBSERVABLES_CONFIGUREDPARAMS;
                }
            } catch (ModelQueryException e) {
                error = e.getMessage();
            }
        } else {
            error = "No configuration available";
        }
        if (null != error) {
            LOGGER.error("While loading monitoring plugins: " + error);
        }
    }
    
    /**
     * Adds <code>text</code> to <code>error</code>.
     * 
     * @param error the error text so far (may be <b>null</b>)
     * @param text the text to append
     * @return the new error text
     */
    private static String addError(String error, String text) {
        String result;
        if (null == error) {
            result = text;
        } else {
            result = error + "; " + text;
        }
        return result;
    }
    
    /**
     * Returns whether a setting is valid.
     * 
     * @param setting the setting
     * @return <code>true</code> if valid (not <b>null</b>, not empty), <code>false</code> else
     */
    private static boolean isValid(String setting) {
        return null != setting && setting.length() > 0;
    }
    
    /**
     * Loads and registers the monitoring plugins in <code>var</code>.
     * 
     * @param var the decision variable containing the configured monitoring plugins
     * @return error texts inc ase of errors
     */
    private static String loadMonitoringPlugins(IDecisionVariable var) {
        String error = null;
        Set<URL> jars = new HashSet<URL>();
        Set<String> classes = new HashSet<String>();
        for (int n = 0; n < var.getNestedElementsCount(); n++) {
            IDecisionVariable nested = Configuration.dereference(var.getNestedElement(n));
            String type = VariableHelper.getString(nested, QmConstants.SLOT_OBSERVABLE_TYPE);
            String cls = VariableHelper.getString(nested, QmConstants.SLOT_CONFIGUREDQPARAM_MONITORCLS);
            String artifact = VariableHelper.getString(nested, QmConstants.SLOT_CONFIGUREDQPARAM_ARTIFACT);
            if (isValid(type) && isValid(artifact) && isValid(cls)) {
                File artifactF = RepositoryConnector.obtainArtifact(artifact, type, ".jar");
                if (null != artifactF) {
                    try {
                        jars.add(artifactF.toURI().toURL());
                        classes.add(cls);
                    } catch (MalformedURLException e) {
                        error = addError(error, e.getMessage());
                    }
                } else {
                    error = addError(error, "Artifact for observable " + type + " not found: " + artifact);
                }
            }  else {
                error = addError(error, "Configuration of observable incomplete: " + type + " " + artifact + " " + cls);
            }
        }
        if (jars.size() > 0) {
            URL[] urls = new URL[jars.size()];
            jars.toArray(urls);
            URLClassLoader loader = new URLClassLoader(urls);
            loaders.add(loader);
            for (String cls : classes) {
                try {
                    Class<?> c = loader.loadClass(cls);
                    if (eu.qualimaster.common.monitoring.IMonitoringPlugin.class.isAssignableFrom(c)) {
                        MonitoringPluginRegistry.register(
                            (eu.qualimaster.common.monitoring.IMonitoringPlugin) c.newInstance());
                    }
                } catch (ClassNotFoundException e) {
                    error = addError(error, e.getMessage());
                } catch (InstantiationException e) {
                    error = addError(error, e.getMessage());
                } catch (IllegalAccessException e) {
                    error = addError(error, e.getMessage());
                }
            }
        }
        return error;
    }

    /**
     * Starts a monitoring plugin and schedules its cluster monitoring task (if provided).
     * 
     * @param plugin the plugin to be started
     */
    private static void startPlugin(IMonitoringPlugin plugin) {
        plugin.start();
        AbstractClusterMonitoringTask task = plugin.createClusterTask(state);
        if (null != task) {
            Map<String, AbstractMonitoringTask> pluginTasks = tasks.get(plugin);
            if (null == pluginTasks) {
                pluginTasks = new HashMap<String, AbstractMonitoringTask>();
                tasks.put(plugin, pluginTasks);
            }
            pluginTasks.put(CLUSTER_TASK_NAME, task);
            timer.schedule(task, 0, Math.max(MINIMUM_MONITORING_FREQUENCY, task.getFrequency()));
        }
    }

    /**
     * Stops a monitoring plugin and stops its cluster monitoring task (if provided).
     * 
     * @param plugin the plugin to be started
     */
    private static void stopPlugin(IMonitoringPlugin plugin) {
        Map<String, AbstractMonitoringTask> pluginTasks = tasks.get(plugin);
        if (null != pluginTasks) {
            AbstractMonitoringTask task = pluginTasks.get(CLUSTER_TASK_NAME);
            if (null != task) {
                task.cancel();
                pluginTasks.remove(CLUSTER_TASK_NAME);
            }
        }        
        plugin.stop();
    }
    
    /**
     * Clears the whole system state (for testing).
     */
    public static void clearState() {
        state.clear();
    }
    
    /**
     * Stop the layer.
     */
    public static void stop() {
        VolumePredictionManager.stop();
        AlgorithmProfilePredictor.stop();
        if (null != reasoningTask) {
            reasoningTask.cancel();
        }
        for (int p = plugins.size() - 1; p >= 0; p--) {
            stopPlugin(plugins.get(p));
        }
        if (null != timer)  {
            timer.cancel();
            timer = null;
        }
        Tracing.close();
        state.clear();
        state.closePlatformTrace();
        for (URLClassLoader loader : loaders) {
            try {
                loader.close();
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
            }
        }
    }
    
    /**
     * Register the default plugins.
     */
    private static void registerDefaultPlugins() {
        registerPlugin(new StormMonitoringPlugin());
    }

    /**
     * Registers a plugin.
     * 
     * @param plugin the plugin to be registered
     */
    public static void registerPlugin(IMonitoringPlugin plugin) {
        if (null != plugin && !plugins.contains(plugin)) {
            plugins.add(plugin);
            if (null != timer) {
                startPlugin(plugin);
            }
        }
    }

    /**
     * Unregisters a plugin.
     * 
     * @param plugin the plugin to be unregistered
     */
    public static void unregisterPlugin(IMonitoringPlugin plugin) {
        if (null != plugins && plugins.contains(plugin)) {
            plugins.remove(plugin);
            Map<String, AbstractMonitoringTask> pluginTasks = tasks.get(plugin);
            if (null != pluginTasks) {
                for (AbstractMonitoringTask task : pluginTasks.values()) {
                    task.cancel();
                }
                pluginTasks.clear();
            }
        }
    }
    
    /**
     * Returns the actual system state determined by monitoring.
     * 
     * @return the system state
     */
    public static SystemState getSystemState() {
        return state;
    }
    
    /**
     * Defines how demo messages shall be handled.
     * 
     * @param state the handling state
     * @return the old state before setting
     */
    public static int setDemoMessageState(int state) {
        int old = demoMessages;
        demoMessages = state;
        return old;
    }

    /**
     * Returns how demo messages shall be handled.
     * 
     * @return the handling state
     */
    public static int getDemoMessagesState() {
        return demoMessages;
    }
    
    /**
     * Returns whether the monitoring layer has access to the adaptation model.
     * 
     * @return <code>true</code> if it has access to the adaptation model, <code>false</code> else
     */
    public static boolean hasAdaptationModel() {
        return null != reasoningTask;
    }
    
    /**
     * Returns the name mapping for the given <code>pipelineName</code>.
     * 
     * @param pipelineName the name of the pipeline
     * @return the name mapping (an identity mapping in case of no registered mapping)
     */
    public static INameMapping getNameMapping(String pipelineName) {
        return CoordinationManager.getNameMapping(pipelineName);
    }
    
    /**
     * Returns the name mapping for the given <code>className</code>.
     * 
     * @param className the class name of the algorithm or pipeline element
     * @return the name mapping (or <b>null</b> if there is none)
     */
    public static INameMapping getNameMappingForClass(String className) {
        return CoordinationManager.getNameMappingForClass(className);
    }
    
    /**
     * Adds a deferred event.
     * 
     * @param pipeline the pipeline to register the event for
     * @param event the deferred event
     */
    public static void addDeferredEvent(String pipeline, MonitoringEvent event) {
        List<MonitoringEvent> evts = deferred.get(pipeline);
        if (null == evts) {
            evts = Collections.synchronizedList(new ArrayList<MonitoringEvent>());
            deferred.put(pipeline, evts);
        }
        evts.add(event);
    }

}
