package eu.qualimaster.coordination;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;

import eu.qualimaster.common.signal.SignalMechanism;
import eu.qualimaster.common.signal.SignalMechanism.NamespaceState;
import eu.qualimaster.coordination.INameMapping.Algorithm;
import eu.qualimaster.coordination.INameMapping.ISubPipeline;
import eu.qualimaster.coordination.commands.CoordinationCommand;
import eu.qualimaster.coordination.commands.PipelineCommand;
import eu.qualimaster.coordination.events.AlgorithmProfilingEvent;
import eu.qualimaster.coordination.events.CoordinationCommandExecutionEvent;
import eu.qualimaster.coordination.events.PipelineResourceUnpackingPluginRegistrationEvent;
import eu.qualimaster.dataManagement.events.ShutdownEvent;
import eu.qualimaster.events.EventHandler;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.EndOfDataEvent;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent.Status;
import eu.qualimaster.infrastructure.PipelineOptions;
import eu.qualimaster.monitoring.events.AlgorithmChangedMonitoringEvent;
import eu.qualimaster.monitoring.events.IEnactmentCompletedMonitoringEvent;
import eu.qualimaster.plugins.ILayerDescriptor;

/**
 * The external interface to execute coordination commands.
 * 
 * @author Holger Eichelberger
 */
public class CoordinationManager {

    /**
     * Defines the coordination layer plugin descriptor.
     * 
     * @author Holger Eichelberger
     */
    public enum Layer implements ILayerDescriptor {
        COORDINATION
    }

    private static final Map<String, INameMapping> NAME_MAPPING = new HashMap<String, INameMapping>();
    private static IExecutionTracer executionTracer;
    private static boolean testingMode = false;
    private static CommandStore store = new CommandStore(CoordinationConfiguration.getEventResponseTimeout());
    private static Map<String, PipelineCommand> pendingStartups = new HashMap<String, PipelineCommand>();
    private static Map<String, AlgorithmProfilingEvent> pendingProfiling 
        = new HashMap<String, AlgorithmProfilingEvent>();
    private static Map<String, PipelineInfo> pipelines = new HashMap<String, PipelineInfo>();

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
            CoordinationManager.execute(command);
        }
        
    }

    /**
     * The handler for pipeline resource unpacking plugin events.
     * 
     * @author Holger Eichelberger
     */
    private static class PipelineResourceUnpackingPluginRegistrationEventHandler 
        extends EventHandler<PipelineResourceUnpackingPluginRegistrationEvent> {

        /**
         * Creates a resource unpacking event handler.
         */
        protected PipelineResourceUnpackingPluginRegistrationEventHandler() {
            super(PipelineResourceUnpackingPluginRegistrationEvent.class);
        }

        @Override
        protected void handle(PipelineResourceUnpackingPluginRegistrationEvent command) {
            PluginRegistry.registerPipelineResourceUnpackingPlugin(command.getPlugin());
        }
        
    }

    /**
     * The handler for pipeline lifecycle events, in particular to send {@link CoordinationCommandExecutionEvent 
     * execution events} on completed lifecycle phase.
     * 
     * @author Holger Eichelberger
     */
    private static class PipelineLifecycleEventHandler extends EventHandler<PipelineLifecycleEvent> {

        /**
         * Creates an adaptation event handler.
         */
        protected PipelineLifecycleEventHandler() {
            super(PipelineLifecycleEvent.class);
        }

        @Override
        protected void handle(PipelineLifecycleEvent event) {
            String pipelineName = event.getPipeline();
            PipelineInfo info = pipelines.get(pipelineName);
            switch (event.getStatus()) {
            // case STARTING: do this when handling the pipeline start command 
            case CHECKED:
                // do not remove here! otherwise endless pending cycle
                PipelineCommand cmd = pendingStartups.get(pipelineName);
                if (null != cmd) {
                    PipelineOptions opts = cmd.getOptions();
                    opts.merge(event.getOptions());
                    execute(cmd);
                } else {
                    LogManager.getLogger(CoordinationManager.class).error(
                        "No deferred startup command for " + pipelineName);
                }
                break;
            case STARTED:
                if (null == info) { // be careful, don't override
                    info = obtainPipelineInfo(pipelineName);
                }
                EventManager.handle(new CoordinationCommandExecutionEvent(event));
                // now monitoring is ready to also handle the profiling event
                AlgorithmProfilingEvent evt = pendingProfiling.remove(pipelineName);
                if (null != evt) {
                    EventManager.handle(evt);
                }
                break;
            case STOPPING:
                break;
            case STOPPED:
                handleSignalNamespace(pipelineName, NamespaceState.CLEAR);
                EventManager.handle(new CoordinationCommandExecutionEvent(event));
                pendingStartups.remove(pipelineName);
                pendingProfiling.remove(pipelineName);
                pipelines.remove(pipelineName);
                break;
            case CREATED:
                // to enable the adaptation layer sending signals, see also signal handler in storm commons
                handleSignalNamespace(pipelineName, NamespaceState.ENABLE);
                ProfileControl.created(pipelineName);
                break;
            default:
                break;
            }
            if (null != info) {
                info.changeStatus(event.getStatus());
            }
        }
        
    }
    
    /**
     * Returns whether the given pipeline was started.
     * 
     * @param pipeline the pipeline name
     * @return <code>true</code> for started, <code>false</code> else
     */
    public static boolean isStarted(String pipeline) {
        boolean result = false;
        PipelineInfo info = pipelines.get(pipeline);
        if (null != info) {
            PipelineLifecycleEvent.Status status = info.getStatus();
            result = status.wasStarted() && status != PipelineLifecycleEvent.Status.STOPPING 
                && status != PipelineLifecycleEvent.Status.STOPPED;
        }
        return result;
    }
    
    /**
     * Enables or disables the signal namespace of <code>pipelineName</code>. The signal namespace will start
     * disabled and upon enabling it it will send out all cached messages.
     * 
     * @param pipelineName the pipeline name leading to the namespace
     * @param state the target state of the specified signal namespace
     */
    protected static void handleSignalNamespace(String pipelineName, NamespaceState state) {
        INameMapping mapping = CoordinationManager.getNameMapping(pipelineName);
        SignalMechanism.changeSignalNamespaceState(CoordinationUtils.getNamespace(mapping), state);
    }
    
    /**
     * Handles end-of-data events sent out by (profiling) sources.
     * 
     * @author Holger Eichelberger
     */
    private static class EndOfDataEventHandler extends EventHandler<EndOfDataEvent> {

        /**
         * Creates a handler instance.
         */
        protected EndOfDataEventHandler() {
            super(EndOfDataEvent.class);
        }

        @Override
        protected void handle(EndOfDataEvent event) {
            ProfileControl control = ProfileControl.getInstance(event.getPipeline());
            if (null != control) {
                try {
                    control.killActual();
                    if (control.hasNext()) {
                        control.startNext();
                    } else {
                        ProfileControl.releaseInstance(control);
                        // may send message
                    }
                } catch (IOException e) {
                    LogManager.getLogger(CoordinationManager.class).error("While profiling - EOD: " + e.getMessage());
                }
            } else {
                LogManager.getLogger(CoordinationManager.class).error("No profile control for: " + event.getPipeline());
            }
        }
        
    }

    /**
     * Handles enactment completed messages from pipelines.
     * 
     * @author Holger Eichelberger
     */
    private static class EnactmentEventHandler extends EventHandler<IEnactmentCompletedMonitoringEvent> {
        
        /**
         * Creates a handler instance.
         */
        protected EnactmentEventHandler() {
            super(IEnactmentCompletedMonitoringEvent.class);
        }

        @Override
        protected void handle(IEnactmentCompletedMonitoringEvent event) {
            store.received(event);
            if (event instanceof AlgorithmChangedMonitoringEvent) {
                AlgorithmChangedMonitoringEvent evt = (AlgorithmChangedMonitoringEvent) event;
                PipelineInfo info = getPipelineInfo(evt.getPipeline());
                if (null != info) {
                    INameMapping mapping = CoordinationManager.getNameMapping(evt.getPipeline());
                    String pipelineElement = evt.getPipelineElement(); // possibly the implementation name
                    String tmp = mapping.getPipelineNodeByImplName(pipelineElement);
                    if (null != tmp) {
                        pipelineElement = tmp;
                    }
                    String algorithm = evt.getAlgorithm(); // possibly the implementation name
                    Algorithm alg = mapping.getAlgorithmByImplName(algorithm);
                    if (null != alg) { // map it back to the configured name
                        algorithm = alg.getName();
                    }
                    LogManager.getLogger(CoordinationManager.class).info("Handling AlgChange "
                        + pipelineElement + " " + algorithm + " " + evt);
                    String prevAlgorithm = info.getEnactedAlgorithm(pipelineElement);
                    if (null != prevAlgorithm) {
                        ISubPipeline subPip = mapping.getSubPipelineByAlgorithmName(prevAlgorithm);
                        if (null != subPip) {
                            PipelineInfo subInfo = getPipelineInfo(subPip.getName());
                            PipelineOptions opts = subInfo == null ? null : subInfo.getOptions();
                            // stop the sub-pipeline, send to all for cleanup
                            new PipelineCommand(subPip.getName(), PipelineCommand.Status.STOP, opts).execute(); 
                        }
                    }
                    LogManager.getLogger(CoordinationManager.class).info("Enacting algorithm "
                        + pipelineElement + " " + algorithm + " " + evt);
                    info.setEnactedAlgorithm(pipelineElement, algorithm);
                }
            }
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
            CoordinationManager.stop();
        }
        
    }
    
    /**
     * Register the event handlers statically.
     */
    static {
        EventManager.register(new CoordinationCommandEventHandler());
        EventManager.register(new PipelineLifecycleEventHandler());
        EventManager.register(new EndOfDataEventHandler());
        EventManager.register(new EnactmentEventHandler());
        EventManager.register(new ShutdownEventHandler());
        EventManager.register(new PipelineResourceUnpackingPluginRegistrationEventHandler());
    }
    
    /**
     * Executes a command.
     * 
     * @param command the command to be executed.
     */
    public static void execute(CoordinationCommand command) {
        if (null != command) {
            CoordinationCommandExecutionVisitor visitor 
                = new CoordinationCommandExecutionVisitor(executionTracer);
            ActiveCommands cmds = visitor.setTopLevelCommand(command);
            if (null != cmds && !cmds.isEmpty()) {
                store.sent(cmds);
            }
            command.accept(visitor);
        }
    }
    
    /**
     * Returns the name mapping for the given <code>pipelineName</code>.
     * 
     * @param pipelineName the name of the pipeline
     * @return the name mapping (an identity mapping in case of no registered mapping)
     */
    public static INameMapping getNameMapping(String pipelineName) {
        INameMapping mapping = NAME_MAPPING.get(pipelineName);
        if (null == mapping) {
            try {
                File topologyJar = CoordinationUtils.obtainPipelineJar(pipelineName);
                mapping = CoordinationUtils.createMapping(pipelineName, topologyJar);
            } catch (IOException e) {
                LogManager.getLogger(CoordinationManager.class).error("While reading name mapping for " + pipelineName 
                    + " (using identity mapping):" + e.getMessage());
            }                
            if (null == mapping) {
                mapping = new IdentityMapping(pipelineName);
            }
            registerNameMapping(mapping);
        }
        return mapping;
    }
    
    /**
     * Returns the name mapping for the given <code>className</code>.
     * 
     * @param className the class name of the algorithm or pipeline element
     * @return the name mapping (or <b>null</b> if there is none)
     */
    public static INameMapping getNameMappingForClass(String className) {
        INameMapping result = null;
        for (INameMapping mapping : NAME_MAPPING.values()) {
            if (null != mapping.getAlgorithmByClassName(className) 
                 || null != mapping.getComponentByClassName(className) 
                 || mapping.getContainerNames().contains(className)) {
                result = mapping;
                break;
            }
        }
        return result;
    }
        
    /**
     * Registers a name mapping for the respective pipeline. This method is
     * for internal use only. Does not override existing full name mappings by identity mappings.
     * 
     * @param mapping the name mapping to be registered
     */
    static void registerNameMapping(INameMapping mapping) {
        INameMapping existing = NAME_MAPPING.get(mapping.getPipelineName());
        if (null == existing || existing.isIdentity()) { // don't overwrite more detailed mapping
            NAME_MAPPING.put(mapping.getPipelineName(), mapping);
        }
    }

    /**
     * Unregisters a name mapping for the respective pipeline. This method is
     * for internal use only.
     * 
     * @param mapping the name mapping to be unregistered
     */
    public static void unregisterNameMapping(INameMapping mapping) {
        NAME_MAPPING.remove(mapping.getPipelineName());
    }
    
    /**
     * Returns the registered pipelines.
     * 
     * @return the registered pipelines
     */
    static Set<String> getRegisteredPipelines() {
        return NAME_MAPPING.keySet();
    }
    
    /**
     * Registers a name mapping but just for testing. Do not use this method
     * in operational code.
     * 
     * @param mapping the mapping to be registered
     */
    public static void registerTestMapping(INameMapping mapping) {
        if (!NAME_MAPPING.containsKey(mapping.getPipelineName())) {
            registerNameMapping(mapping);
        }
    }

    /**
     * Starts this layer.
     */
    public static void start() {
        eu.qualimaster.plugins.PluginRegistry.registerLayer(Layer.COORDINATION);
        RepositoryConnector.initialize();
        eu.qualimaster.plugins.PluginRegistry.startPlugins(Layer.COORDINATION);
    }

    /**
     * Stop this layer.
     */
    public static void stop() {
        eu.qualimaster.plugins.PluginRegistry.shutdownPlugins(Layer.COORDINATION);
        NAME_MAPPING.clear();
        SignalMechanism.clear();
    }
    
    /**
     * Defines the execution tracer.
     * 
     * @param tracer the execution tracer (may be <b>null</b>)
     */
    public static void setTracer(IExecutionTracer tracer) {
        executionTracer = tracer;
    }
    
    /**
     * Runs this layer in testing mode, i.e., pretend that execution systems are working properly without
     * doing real execution.
     * 
     * @param testing testing or not testing
     */
    public static void setTestingMode(boolean testing) {
        testingMode = testing;
    }
    
    /**
     * Returns whether this layer is in testing mode.
     * 
     * @return <code>true</code> for testing, <code>false</code> else
     */
    public static boolean isTestingMode() {
        return testingMode;
    }
    
    /**
     * Returns whether the startup of <code>pipeline</code> is pending.
     * 
     * @param pipeline the pipeline to ask for
     * @return <code>true</code> for pending, <code>false</code> else
     */
    static boolean isStartupPending(String pipeline) {
        return pendingStartups.containsKey(pipeline);
    }
    
    /**
     * Defers the startup of <code>command</code>.
     * 
     * @param command the command to be deferred
     */
    static void deferStartup(PipelineCommand command) {
        pendingStartups.put(command.getPipeline(), command);
    }
    
    /**
     * Defers the start of profiling for a given pipeline to ensure that it happens after the monitoring layer
     * started the monitoring tasks for pipeline in the starting lifecycle phase.
     * 
     * @param pipeline the pipeline name
     * @param family the family name
     * @param algorithm the algorithm name
     * @param settings the actual profiling settings for information, may be <b>null</b>
     * @return the scheduled profiling event 
     */
    static AlgorithmProfilingEvent deferProfilingStart(String pipeline, String family, String algorithm, 
        Map<String, Serializable> settings) {
        AlgorithmProfilingEvent result = new AlgorithmProfilingEvent(pipeline, family, algorithm, 
            AlgorithmProfilingEvent.Status.START, settings);
        pendingProfiling.put(pipeline, result);
        return result;
    }
    
    /**
     * Removes a pending startup.
     * 
     * @param pipeline the pipeline to remove the pending startup for
     */
    static void removePendingStartup(String pipeline) {
        pendingStartups.remove(pipeline);
    }
    
    /**
     * Defer a certain action (may be a command).
     * 
     * @param pipeline the pipeline to defer the pipeline for (may be <b>null</b>, then the call is without effect)
     * @param status the status when the <code>action</code> shall be executed (may be <b>null</b>, then the call is 
     *     without effect)
     * @param action the action to execute (may be <b>null</b>, then the call is without effect)
     */
    static void deferCommand(String pipeline, PipelineLifecycleEvent.Status status, IAction action) {
        if (null != pipeline && null != status && null != action) {
            PipelineInfo info = obtainPipelineInfo(pipeline);
            info.addAction(status, action);
        }
    }
    
    /**
     * Obtains a pipeline information object ensuring that there is an instance although there was none before.
     * 
     * @param pipeline the pipeline name
     * @return the pipeline information instance
     */
    private static PipelineInfo obtainPipelineInfo(String pipeline) {
        PipelineInfo info = pipelines.get(pipeline);
        if (null == info) {
            info = new PipelineInfo();
            pipelines.put(pipeline, info);
        }
        return info;
    }
    
    /**
     * Returns the pipeline information object for <code>pipeline</code>.
     * 
     * @param pipeline the name of the pipeline
     * @return the information object, may be <b>null</b> if <code>pipeline</code> is not known
     */
    static PipelineInfo getPipelineInfo(String pipeline) {
        return pipelines.get(pipeline);
    }
    
    /**
     * Returns the registered pipeline options for a registered pipeline.
     * 
     * @param pipeline the pipeline name
     * @return the options (may be <b>null</b>)
     */
    static PipelineOptions getPipelineOptions(String pipeline) {
        PipelineOptions result = null;
        PipelineInfo info = pipelines.get(pipeline);
        if (null != info) {
            result = info.getOptions();
        }
        return result;
    }
    
    /**
     * Registers the pipeline options for a given pipeline.
     * 
     * @param pipeline the pipeline name
     * @param options the pipeline options (may be <b>null</b>)
     */
    static void registerPipelineOptions(String pipeline, PipelineOptions options) {
        obtainPipelineInfo(pipeline).setOptions(options);
    }
    
    /**
     * Clears cached information about a pipeline. Handle with care!
     * 
     * @param pipeline the pipeline to be cleared
     * @param exclude exclude the state when not to clear
     */
    static void clearPipeline(String pipeline, Status exclude) {
        boolean clear = true;
        PipelineInfo info = getPipelineInfo(pipeline);
        if (null != info && exclude == info.getStatus()) {
            clear = false;
        }
        if (clear) {
            pipelines.remove(pipeline);
            pendingStartups.remove(pipeline);
        }
    }
    
}
