package eu.qualimaster.coordination;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import eu.qualimaster.common.signal.AbstractTopologyExecutorSignal;
import eu.qualimaster.common.signal.AlgorithmChangeSignal;
import eu.qualimaster.common.signal.LoadSheddingSignal;
import eu.qualimaster.common.signal.MonitoringChangeSignal;
import eu.qualimaster.common.signal.ParameterChange;
import eu.qualimaster.common.signal.ParameterChangeSignal;
import eu.qualimaster.common.signal.ReplaySignal;
import eu.qualimaster.common.signal.ShutdownSignal;
import eu.qualimaster.common.signal.SignalException;
import eu.qualimaster.common.signal.SignalMechanism;
import eu.qualimaster.coordination.INameMapping.Algorithm;
import eu.qualimaster.coordination.INameMapping.Component;
import eu.qualimaster.coordination.INameMapping.ISubPipeline;
import eu.qualimaster.coordination.PipelineCache.PipelineElementCache;
import eu.qualimaster.coordination.RepositoryConnector.Models;
import eu.qualimaster.coordination.StormUtils.TopologyTestInfo;
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
import eu.qualimaster.coordination.commands.PipelineCommand.Status;
import eu.qualimaster.coordination.events.CoordinationCommandExecutionEvent;
import eu.qualimaster.coordination.commands.ProfileAlgorithmCommand;
import eu.qualimaster.coordination.commands.ReplayCommand;
import eu.qualimaster.coordination.commands.ScheduleWavefrontAdaptationCommand;
import eu.qualimaster.coordination.commands.ShutdownCommand;
import eu.qualimaster.coordination.commands.UpdateCommand;
import eu.qualimaster.coordination.shutdown.Shutdown;
import eu.qualimaster.dataManagement.DataManager;
import eu.qualimaster.easy.extension.internal.AlgorithmProfileHelper;
import eu.qualimaster.easy.extension.internal.AlgorithmProfileHelper.ProfileData;
import eu.qualimaster.easy.extension.internal.QmProjectDescriptor;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.infrastructure.PipelineOptions;
import eu.qualimaster.monitoring.events.ChangeMonitoringEvent;
import net.ssehub.easy.basics.modelManagement.ModelInitializer;
import net.ssehub.easy.basics.modelManagement.ModelManagementException;
import net.ssehub.easy.basics.progress.ProgressObserver;
import net.ssehub.easy.instantiation.core.model.common.VilException;

import static eu.qualimaster.coordination.CoordinationUtils.getNamespace;

/**
 * Visits the commands for execution. Please note that when starting the execution of a single command,
 * the command must be placed on the {@link #commandStack} and at the end of the execution (no exception must
 * be thrown) {@link #writeCoordinationLog(CoordinationCommand, CoordinationExecutionResult)} must be
 * called which pops from the stack and performs the logging.
 * 
 * @author Holger Eichelberger
 */
class CoordinationCommandExecutionVisitor extends AbstractCoordinationCommandExecutionVisitor {

    private static final Set<Class<? extends CoordinationCommand>> DEFER_SUCCESSFUL_EXECUTION 
        = new HashSet<Class<? extends CoordinationCommand>>();
    private long timestamp = System.currentTimeMillis();
    private ActiveCommands activeCommands = null;
    
    static {
        // later, all command shall be deferred and report about the actual completion
        DEFER_SUCCESSFUL_EXECUTION.add(PipelineCommand.class);
    }
    
    /**
     * Creates a command execution visitor.
     * 
     * @param tracer a tracer for obtaining information about the execution
     */
    CoordinationCommandExecutionVisitor(IExecutionTracer tracer) {
        super(tracer);
    }
    
    /**
     * Sets the top-level command of this execution.
     * 
     * @param command the command
     * @return the active commands object
     */
    ActiveCommands setTopLevelCommand(CoordinationCommand command) {
        activeCommands = new ActiveCommands(command);
        return activeCommands;
    }
    
    /**
     * Returns the actual message id.
     * 
     * @return the actual message id
     */
    private String getCauseMessageId() {
        return null == activeCommands ? "" : activeCommands.getCauseMessageId();
    }
    
    /**
     * Handles sending a (storm commons) signal.
     * 
     * @param command the command causing the signal
     * @param signal the signal
     * @return whether the execution failed (instance) or not (<b>null</b>)
     */
    private static CoordinationExecutionResult send(CoordinationCommand command, 
        AbstractTopologyExecutorSignal signal) {
        CoordinationExecutionResult failing = null;
        try {
            if (!CoordinationManager.isTestingMode()) {
                getLogger().info("Sending signal " + signal);
                signal.sendSignal();
            }
        } catch (SignalException e) {
            failing = new CoordinationExecutionResult(command, e.getMessage(), 
                CoordinationExecutionCode.SIGNAL_SENDING_ERROR);
        }
        return failing;
    }

    @Override
    public CoordinationExecutionResult visitAlgorithmChangeCommand(AlgorithmChangeCommand command) {
        return handleAlgorithmChange(command, null);
    }
    
    @Override
    protected CoordinationExecutionResult handleAlgorithmChange(AlgorithmChangeCommand command, 
        List<ParameterChange> parameters) {
        CoordinationExecutionResult result;
        String pipelineName = command.getPipeline();
        INameMapping mapping = CoordinationManager.getNameMapping(pipelineName);
        ISubPipeline subPip = mapping.getSubPipelineByAlgorithmName(command.getAlgorithm());
        if (null != subPip) {
            CoordinationManager.deferCommand(subPip.getName(), PipelineLifecycleEvent.Status.STARTED, 
                new AlgorithmChangeAction(command, parameters, getTracer()));
            PipelineCommand cmd = new PipelineCommand(subPip.getName(), PipelineCommand.Status.START, 
                getSubPipelineOptions(pipelineName, command.getOptions()));
            result = handlePipelineStart(cmd);
        } else {
            result = handleAlgorithmChangeImpl(command, parameters);
        }
        return result;
    }

    /**
     * Returns the sub pipeline options.
     * 
     * @param mainPipeline the main pipeline
     * @param commandOptions the options introduced by the command
     * @return the options for the sub-pipeline marked as sub-pipeline
     */
    private PipelineOptions getSubPipelineOptions(String mainPipeline, PipelineOptions commandOptions) {
        PipelineOptions opts = CoordinationManager.getPipelineOptions(mainPipeline);
        if (null == opts) {
            opts = new PipelineOptions(); // shall not occur
        } else {
            opts = new PipelineOptions(opts); // clone
        }
        if (null != commandOptions) { // take into account decisions done by the adaptation for algorithm change
            opts.merge(commandOptions);
        }
        opts.markAsSubPipeline(mainPipeline);
        return opts;
    }

    /**
     * Handles an algorithm change command with optional parameters. This method may be an entry point for deferred
     * actions.
     * 
     * @param command the command
     * @param parameters explicit parameters (may be <b>null</b> than only the cached ones will be used)
     * @return the coordination execution result
     */
    CoordinationExecutionResult handleAlgorithmChangeImpl(AlgorithmChangeCommand command, 
        List<ParameterChange> parameters) {
        CoordinationExecutionResult failing = null;
        
        startingCommand(command);
        String pipelineName = command.getPipeline();
        INameMapping mapping = CoordinationManager.getNameMapping(pipelineName);
        Component receiver = CoordinationUtils.getReceiverComponent(
            mapping.getPipelineNodeComponent(command.getPipelineElement()));
        if (null == receiver) {
            String message = "no receiver for changing the algorithm on " + command.getPipeline() + "/" 
                + command.getPipelineElement();
            failing = new CoordinationExecutionResult(command, message, CoordinationExecutionCode.SIGNAL_SENDING_ERROR);
        } else {
            PipelineElementCache cache = PipelineCache.getCache(command);
            if (null == parameters) {
                parameters = cache.parameters();
            }
            String algorithm = command.getAlgorithm();
            Algorithm alg = mapping.getAlgorithm(algorithm);
            if (null != alg) {
                algorithm = alg.getName(); // the implementation name
            }
            AlgorithmChangeSignal signal = new AlgorithmChangeSignal(getTargetNamespace(mapping), receiver.getName(), 
                algorithm, parameters, getCauseMessageId());
            signal.setParameters(command.getParameters());
            send(command, signal);
            cache.setAlgorithm(command.getAlgorithm(), parameters);
        }
        if (null != getTracer()) {
            getTracer().executedAlgorithmChangeCommand(command, failing);
        }
        return writeCoordinationLog(command, failing);        
    }
    
    @Override
    public CoordinationExecutionResult visitParameterChangeCommand(ParameterChangeCommand<?> command) {
        return handleParameterChange(command, null);
    }

    @Override
    protected CoordinationExecutionResult handleParameterChange(ParameterChangeCommand<?> command, 
        List<ParameterChange> huckup) {
        CoordinationExecutionResult failing = null;
        startingCommand(command);
        String pipelineName = command.getPipeline();
        INameMapping mapping = CoordinationManager.getNameMapping(pipelineName);
        Component receiver = CoordinationUtils.getReceiverComponent(CoordinationUtils.getParameterReceiverComponent(
            mapping, command.getPipelineElement(), command.getParameter()));
        if (null == receiver) {
            String message = "no receiver for changing the parameter on " + command.getPipeline() + "/" 
                + command.getPipelineElement();
            failing = new CoordinationExecutionResult(command, message, CoordinationExecutionCode.SIGNAL_SENDING_ERROR);
        } else {
            List<ParameterChange> changes = new ArrayList<ParameterChange>();
            changes.add(new ParameterChange(command.getParameter(), command.getValue()));
            if (null != huckup) {
                changes.addAll(huckup);
            }
            ParameterChangeSignal signal = new ParameterChangeSignal(getTargetNamespace(mapping), 
                receiver.getName(), changes, getCauseMessageId());
            send(command, signal);
            PipelineCache.getCache(command).setParameters(changes, false);
        }
        if (null != getTracer()) {
            getTracer().executedParameterChangeCommand(command, failing);
        }
        return writeCoordinationLog(command, failing);
    }
    
    /**
     * Returns the target namespace for a pipeline mapping. If <code>mapping</code> represents the namespace of a 
     * sub-pipeline, the target namespace of the main pipeline is returned. By convention, loosely integrated 
     * sub-pipelines carry the namespace of the main pipeline (as for tightly integrated sub-pipelines).
     * 
     * @param mapping the name mapping
     * @return the target namespace
     */
    private String getTargetNamespace(INameMapping mapping) {
        String result = getNamespace(mapping);
        PipelineInfo info = CoordinationManager.getPipelineInfo(mapping.getPipelineName());
        if (null != info) {
            String mainPip = info.getMainPipeline();
            if (null != mainPip) {
                INameMapping mainPipMapping = CoordinationManager.getNameMapping(mainPip);
                if (null != mainPipMapping) {
                    result = getNamespace(mainPipMapping);
                }
            }
        }
        return result;
    }
    
    @Override
    public CoordinationExecutionResult visitCommandSequence(CommandSequence command) {
        CoordinationExecutionResult failed = null;
        startingCommand(command);
        CommandSequenceGroupingVisitor gVisitor = new CommandSequenceGroupingVisitor();
        gVisitor.setExecutor(this);
        for (int c = 0; null == failed && c < command.getCommandCount(); c++) {
            failed = command.getCommand(c).accept(gVisitor);
        }
        if (null == failed) {
            failed = gVisitor.flush();
        }
        if (null != getTracer()) {
            getTracer().executedCommandSequence(command, failed);
        }
        return writeCoordinationLog(command, failed);
    }
    
    @Override
    public CoordinationExecutionResult visitCommandSet(CommandSet command) {
        CoordinationExecutionResult failed = null;
        startingCommand(command);
        CommandSetGroupingVisitor gVisitor = new CommandSetGroupingVisitor();
        for (int c = 0; c < command.getCommandCount(); c++) {
            command.getCommand(c).accept(gVisitor);
        }
        gVisitor.setExecutor(this);
        for (int c = 0; null == failed && c < command.getCommandCount(); c++) {
            failed = command.getCommand(c).accept(gVisitor);
        }
        if (null != getTracer()) {
            getTracer().executedCommandSet(command, failed);
        }
        return writeCoordinationLog(command, failed);
    }

    @Override
    public CoordinationExecutionResult visitPipelineCommand(PipelineCommand command) {
        CoordinationExecutionResult failing = null;
        boolean deferred = false;
        startingCommand(command);
        Status status = command.getStatus();
        boolean knownCommand = false;
        if (null != status) {
            knownCommand = true;
            String pipelineName = command.getPipeline();
            switch (status) {
            case START:
                if (null == pipelineName) {
                    failing = new CoordinationExecutionResult(command, "illegal pipeline name: null", 
                        CoordinationExecutionCode.STARTING_PIPELINE);
                } else {
                    if (CoordinationManager.isStartupPending(pipelineName)) {
                        CoordinationManager.removePendingStartup(pipelineName);
                        failing = handlePipelineStart(command);
                    } else {
                        getLogger().info("Deferring pipeline start command for " + pipelineName);
                        CoordinationManager.deferStartup(command);
                        EventManager.handle(new PipelineLifecycleEvent(pipelineName, 
                            PipelineLifecycleEvent.Status.CHECKING, command.getOptions(), command));
                        deferred = true;
                    }
                }
                break;
            case CONNECT:
                DataManager.connectAll(command.getPipeline());
                break;
            case DISCONNECT:
                DataManager.disconnectAll(command.getPipeline());
                break;
            case STOP:
                failing = handlePipelineStop(command, true);
                break;
            default:
                knownCommand = false;
                break;
            }
        } 
        if (!knownCommand) {
            String message = "unknown pipeline command state: " + command.getStatus();
            failing = new CoordinationExecutionResult(command, message, CoordinationExecutionCode.UNKNOWN_COMMAND);
        }
        if (!deferred) {
            if (null != getTracer()) {
                getTracer().executedPipelineCommand(command, failing);
            }
            failing = writeCoordinationLog(command, failing);
        }
        return failing;
    }
    
    /**
     * Handles starting a pipeline.
     * 
     * @param command the pipeline start command
     * @return the pipeline execution result
     */
    private CoordinationExecutionResult handlePipelineStart(PipelineCommand command) {
        CoordinationExecutionResult failing = null;
        CoordinationManager.registerPipelineOptions(command.getPipeline(), command.getOptions());
        String pipelineName = command.getPipeline();
        INameMapping mapping = CoordinationManager.getNameMapping(pipelineName);
        getLogger().info("Processing pipeline start command for " + pipelineName + " with options " 
            + command.getOptions());
        try {
            String absPath = CoordinationUtils.loadMapping(pipelineName);
            doPipelineStart(mapping, absPath, command.getOptions(), command);
        } catch (IOException e) { // implies file not found!
            String message = "while starting pipeline '" + command.getPipeline() + "': " +  e.getMessage();
            failing = new CoordinationExecutionResult(command, message, 
                CoordinationExecutionCode.STARTING_PIPELINE);
        }
        return failing;
    }

    /**
     * Actually performs the pipeline start.
     * 
     * @param mapping the name mapping instance
     * @param jarPath the absolute path to the pipeline Jar
     * @param options the pipeline options
     * @param command the causing command (may be <b>null</b>)
     * @throws IOException in case that stopping fails
     */
    static void doPipelineStart(INameMapping mapping, String jarPath, PipelineOptions options, 
        CoordinationCommand command) throws IOException {
        String pipelineName = mapping.getPipelineName();
        try {
            // just in case that the pipeline was killed manually
            SignalMechanism.getPortManager().clearPortAssignments(pipelineName); 
        } catch (SignalException e) {
            getLogger().error(e.getMessage());
        }
        PipelineCache.getCache(pipelineName); // prepare the cache
        if (!CoordinationManager.isTestingMode()) {
            StormUtils.submitTopology(CoordinationConfiguration.getNimbus(), mapping, jarPath, 
                options);
            // cache curator, takes a while, pass "virtual" namespace for namespace state
            SignalMechanism.prepareMechanism(getNamespace(mapping));
        }
        EventManager.handle(new PipelineLifecycleEvent(pipelineName, PipelineLifecycleEvent.Status.STARTING, 
            options, command));
        // if monitoring detects, that all families are initialized, it switches to INITIALIZED causing the 
        // DataManger to connect the sources
    }
    
    /**
     * Handles stopping a pipeline.
     * 
     * @param command the pipeline start command
     * @param top is this a top-level or a sub-pipeline call
     * @return the pipeline execution result
     */
    private CoordinationExecutionResult handlePipelineStop(PipelineCommand command, boolean top) {
        CoordinationExecutionResult failing = null;
        String pipelineName = command.getPipeline();
        INameMapping mapping = CoordinationManager.getNameMapping(pipelineName);
        String pipText = top ? "pipeline" : "sub-pipeline";
        getLogger().info("Processing " + pipText + " stop command for " + pipelineName 
            + " with options " + command.getOptions());
        try {
            doPipelineStop(mapping, command.getOptions(), command);
            CoordinationManager.unregisterNameMapping(mapping);
            SignalMechanism.getPortManager().clearPortAssignments(pipelineName);
        } catch (IOException e) {
            String message = "while stopping " + pipText + " '" + command.getPipeline() + "': " +  e.getMessage();
            failing = new CoordinationExecutionResult(command, message, 
                CoordinationExecutionCode.STOPPING_PIPELINE);
        } catch (SignalException e) {
            getLogger().error(e.getMessage());
        }
        for (ISubPipeline sp : mapping.getSubPipelines()) {
            handlePipelineStop(new PipelineCommand(sp.getName(), command.getStatus(), command.getOptions()), false);
        }
        return failing;
    }
    
    /**
     * Actually performs the pipeline stop. This method does not unregister the name mapping!
     * 
     * @param mapping the name mapping instance
     * @param options the pipeline options
     * @param command the causing command (may be <b>null</b>)
     * @throws IOException in case that stopping fails
     */
    static void doPipelineStop(INameMapping mapping, PipelineOptions options, 
        CoordinationCommand command) throws IOException {
        String pipelineName = mapping.getPipelineName();
        EventManager.handle(new PipelineLifecycleEvent(pipelineName, PipelineLifecycleEvent.Status.STOPPING, 
            command));
        for (Component c : mapping.getComponents()) {
            ShutdownSignal signal = new ShutdownSignal(getNamespace(mapping), c.getName());
            send(command, signal); // ignore failing
        }
        Utils.sleep(CoordinationConfiguration.getShutdownSignalWaitTime());
        SignalMechanism.releaseMechanism(getNamespace(mapping));
        if (!CoordinationManager.isTestingMode()) {
            // TODO stop multiple physical sub-topologies
            StormUtils.killTopology(CoordinationConfiguration.getNimbus(), pipelineName, 
                CoordinationConfiguration.getStormCmdWaitingTime(), options, true);
            // TODO unload MaxFiles (currently assuming exclusive DFE use)
        }
        PipelineCache.removeCache(pipelineName);
        EventManager.handle(new PipelineLifecycleEvent(pipelineName, PipelineLifecycleEvent.Status.STOPPED, 
            command));
    }

    @Override
    public CoordinationExecutionResult visitScheduleWavefrontAdaptationCommand(
        ScheduleWavefrontAdaptationCommand command) {
        startingCommand(command);
        // TODO to be done later, writeCoordinationLog
        if (null != getTracer()) {
            getTracer().executedScheduleWavefrontAdaptationCommand(command, null);
        }
        return writeCoordinationLog(command, new CoordinationExecutionResult(command, "not yet implemented", 
            CoordinationExecutionCode.NOT_IMPLEMENTED));
    }

    @Override
    public CoordinationExecutionResult visitMonitoringChangeCommand(MonitoringChangeCommand command) {
        startingCommand(command);
        // just forward to the monitoring layer

        String pipelineName = command.getPipeline();
        if (null != pipelineName && null != command.getPipelineElement()) {
            INameMapping mapping = CoordinationManager.getNameMapping(pipelineName);
            Component receiver = CoordinationUtils.getReceiverComponent(
                mapping.getPipelineNodeComponent(command.getPipelineElement()));
            if (null != receiver) {
                MonitoringChangeSignal signal = new MonitoringChangeSignal(getNamespace(mapping), receiver.getName(), 
                    command.getFrequencies(), command.getObservables(), getCauseMessageId());
                send(command, signal);
            }
        }
        
        EventManager.handle(new ChangeMonitoringEvent(command.getPipeline(), command.getPipelineElement(), 
            command.getFrequencies(), command.getObservables(), command));
        if (null != getTracer()) {
            getTracer().executedMonitoringChangeCommand(command, null);
        }
        return writeCoordinationLog(command, null);
    }

    /**
     * Pops the actual command from the stack and in case of a failure, writes the message to logging
     * and the whole information to the coordination log if on top level.
     * 
     * @param command the current command being executed
     * @param failing the failing command (may be <b>null</b> if no execution failure)
     * @return <code>failing</code>
     */
    protected CoordinationExecutionResult writeCoordinationLog(CoordinationCommand command, 
        CoordinationExecutionResult failing) {
        endingCommand(command);
        if (null != failing) {
            getLogger().error("enactment failed: " + failing.getMessage());
        }
        if (isProcessingCommands()) {
            CoordinationCommand cmd;
            String message;
            int code;
            boolean sendEvent;
            if (null == failing) {
                cmd = command;
                message = "";
                code = CoordinationExecutionCode.SUCCESSFUL;
                sendEvent = !DEFER_SUCCESSFUL_EXECUTION.contains(command.getClass());
            } else {
                cmd = failing.getCommand();
                message = failing.getMessage();
                code = failing.getCode();
                sendEvent = true; // immediate in failure case
            }
            // TODO this shall go to the data management layer
            String text = "CoordinationLog: " + timestamp + " " + cmd.getClass().getName() + " " + message + " " + code;
            System.out.println(text);
            if (null != getTracer()) {
                getTracer().logEntryWritten(text);
            }
            if (sendEvent) {
                EventManager.handle(new CoordinationCommandExecutionEvent(command, cmd, code, message));
            }
        }
        return failing;
    }

    @Override
    public CoordinationExecutionResult visitParallelismChangeCommand(ParallelismChangeCommand command) {
        CoordinationExecutionResult failing = null;
        startingCommand(command);
        String pipelineName = command.getPipeline();
        INameMapping mapping = CoordinationManager.getNameMapping(pipelineName);
        String errorMsg = null;
        if (!mapping.isIdentity()) { // testing, unknown pipeline
            if (null != command.getIncrementalChanges()) {
                Map<String, ParallelismChangeRequest> changeMapping 
                    = mapChanges(mapping, command.getIncrementalChanges());
                String cmd = "Changing parallism of " + pipelineName + ":";
                for (Map.Entry<String, ParallelismChangeRequest> entry : changeMapping.entrySet()) {
                    cmd += " " + entry.getKey() + "=" + entry.getValue();
                }
                getLogger().info(cmd);
                try {
                    StormUtils.changeParallelism(pipelineName, changeMapping);
                } catch (IOException e) {
                    errorMsg = "While changing pipeline parallelism of '" + pipelineName + "': " +  e.getMessage();
                }
            } else if (null != command.getExecutors()) {
                Map<String, Integer> execMapping = mapChanges(mapping, command.getExecutors());
                String cmd = "Rebalancing " + pipelineName + " -n " + command.getNumberOfWorkers();
                for (Map.Entry<String, Integer> entry : execMapping.entrySet()) {
                    cmd += " -e " + entry.getValue() + "=" + entry.getValue();
                }
                getLogger().info(cmd);
                try {
                    StormUtils.rebalance(CoordinationConfiguration.getNimbus(), pipelineName, 
                        command.getNumberOfWorkers(), execMapping, CoordinationConfiguration.getStormCmdWaitingTime());
                } catch (IOException e) {
                    errorMsg = "while rebalancing pipeline '" + pipelineName + "': " +  e.getMessage();
                }
            } // no changes given, no failure
        } // ignore for testing, no failure
        if (null != errorMsg) {
            failing = new CoordinationExecutionResult(command, errorMsg,
                CoordinationExecutionCode.CHANGING_PARALLELISM);
        }
        if (null != getTracer()) {
            getTracer().executedParallelismChangeCommand(command, failing);
        }
        return writeCoordinationLog(command, failing);
    }
    
    /**
     * Maps pipeline changes from pipeline names to executor / component names.
     * 
     * @param <T> the type of the change
     * @param mapping the name mapping to be applied
     * @param changes the changes assigned to pipeline names
     * @return the mapped changes assigned to executor / component names
     */
    private static <T> Map<String, T> mapChanges(INameMapping mapping, Map<String, T> changes) {
        Map<String, T> result = new HashMap<String, T>();
        for (Map.Entry<String, T> entry : changes.entrySet()) {
            Component comp = mapping.getPipelineNodeComponent(entry.getKey());
            if (null != comp) {
                result.put(comp.getName(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * Returns the logger for this class.
     * 
     * @return the logger
     */
    private static Logger getLogger() {
        return LogManager.getLogger(CoordinationCommandExecutionVisitor.class);
    }

    @Override
    public CoordinationExecutionResult visitProfileAlgorithmCommand(ProfileAlgorithmCommand command) {
        CoordinationExecutionResult failing = null;
        startingCommand(command);
        Models models = RepositoryConnector.getModels(RepositoryConnector.getPhaseWithVil());
        if (null == models) {
            failing = new CoordinationExecutionResult(command, "Configuration model is not available "
                + "- see messages above", CoordinationExecutionCode.CHANGING_PARALLELISM);
        } else {
            models.startUsing();
            //models.reloadIvml();
            models.reloadVil();
            net.ssehub.easy.varModel.confModel.Configuration cfg = models.getConfiguration();
            File tmp = null;
            try {
                String pipelineName;
                tmp = new File(FileUtils.getTempDirectory(), "qmDebugProfile");
                org.apache.commons.io.FileUtils.deleteDirectory(tmp);
                tmp.mkdirs();
                if (StormUtils.inTesting()) {
                    Set<String> testing = StormUtils.getTestingTopologyNames();
                    Object[] testingTmp = testing.toArray();
                    if (testingTmp.length > 0) {
                        pipelineName = testingTmp[0].toString();
                        TopologyTestInfo info = StormUtils.getTestInfo(pipelineName);
                        FileUtils.copyDirectory(info.getBaseFolder(), tmp);
                    } else {
                        pipelineName = "TestPip"; // fallback - fails as no VIL present
                    }
                } else {
                    pipelineName = "TestPip" + System.currentTimeMillis(); 
                    FileUtils.copyDirectory(RepositoryConnector.getCurrentModelPath().toFile(), tmp);
                }
                ModelInitializer.addLocation(tmp, ProgressObserver.NO_OBSERVER);
                ProfileData data;
                try {
                    QmProjectDescriptor source = new QmProjectDescriptor(tmp);
                    data = AlgorithmProfileHelper.createProfilePipeline(cfg, pipelineName, command.getFamily(), 
                        command.getAlgorithm(), source);
                } catch (ModelManagementException e) {
                    data = null;
                    if (StormUtils.inTesting()) {
                        TopologyTestInfo tInfo = StormUtils.getTestInfo();
                        data = null != tInfo ? tInfo.getProfileData() : null;
                    } 
                    if (null == data) {
                        throw e;
                    }
                }
                ProfileControl control = new ProfileControl(cfg, command, data);
                control.startNext();
            } catch (ModelManagementException | VilException | IOException e) {
                failing = new CoordinationExecutionResult(command, e.getMessage(),
                    CoordinationExecutionCode.PROFILING);
            }
            if (null != tmp) {
                try {
                    ModelInitializer.removeLocation(tmp, ProgressObserver.NO_OBSERVER);
                    if (CoordinationConfiguration.deleteProfilingPipelines()) {
                        FileUtils.deleteQuietly(tmp);
                    }
                } catch (ModelManagementException e) {
                    getLogger().error("While clearning up profiling creation folders: " + e.getMessage());
                }
            }
            models.reloadVil();
            models.endUsing();
        }
        return writeCoordinationLog(command, failing);
    }

    @Override
    public CoordinationExecutionResult visitShutdownCommand(ShutdownCommand command) {
        CoordinationExecutionResult failing = null;
        startingCommand(command);
        Set<String> pips;
        do { // as stopping sub-pipelines affects getRegisteredPipelines, do it rather carefully
            pips = CoordinationManager.getRegisteredPipelines();
            if (!pips.isEmpty()) {
                // ignore result for now
                handlePipelineStop(new PipelineCommand(pips.iterator().next(), PipelineCommand.Status.STOP), true);
            }
        } while (!pips.isEmpty());
        Shutdown.shutdown(command);
        return writeCoordinationLog(command, failing);
    }

    @Override
    public CoordinationExecutionResult visitUpdateCommand(UpdateCommand command) {
        CoordinationExecutionResult failing = null;
        startingCommand(command);
        RepositoryConnector.updateModels();
        return writeCoordinationLog(command, failing);
    }

    @Override
    public CoordinationExecutionResult visitReplayCommand(ReplayCommand command) {
        CoordinationExecutionResult failing = null;
        startingCommand(command);
        String pipelineName = command.getPipeline();
        INameMapping mapping = CoordinationManager.getNameMapping(pipelineName);
        Component receiver = CoordinationUtils.getReceiverComponent(
            mapping.getPipelineNodeComponent(command.getPipelineElement()));
        if (null == receiver) {
            String message = "no receiver for sending replay command on " + command.getPipeline() + "/" 
                + command.getPipelineElement();
            failing = new CoordinationExecutionResult(command, message, CoordinationExecutionCode.SIGNAL_SENDING_ERROR);
        } else {
            ReplaySignal signal = new ReplaySignal(getNamespace(mapping), receiver.getName(), 
                command.getStartReplay(), command.getTicket(), getCauseMessageId());
            if (command.getStartReplay()) {
                signal.setReplayStartInfo(command.getStart(), command.getEnd(), command.getSpeed(), command.getQuery());
            }
            send(command, signal);
        }
        if (null != getTracer()) {
            getTracer().executedReplayCommand(command, failing);
        }
        return writeCoordinationLog(command, failing);        
    }

    @Override
    public CoordinationExecutionResult visitLoadScheddingCommand(LoadSheddingCommand command) {
        CoordinationExecutionResult failing = null;
        startingCommand(command);
        String pipelineName = command.getPipeline();
        INameMapping mapping = CoordinationManager.getNameMapping(pipelineName);
        Component receiver = CoordinationUtils.getReceiverComponent(
            mapping.getPipelineNodeComponent(command.getPipelineElement()));
        if (null == receiver) {
            String message = "no receiver for sending replay command on " + command.getPipeline() + "/" 
                + command.getPipelineElement();
            failing = new CoordinationExecutionResult(command, message, CoordinationExecutionCode.SIGNAL_SENDING_ERROR);
        } else {
            LoadSheddingSignal signal = new LoadSheddingSignal(getNamespace(mapping), receiver.getName(), 
                command.getShedder(), command.parameters(), getCauseMessageId());
            send(command, signal);
        }
        if (null != getTracer()) {
            getTracer().executedLoadScheddingCommand(command, failing);
        }
        return writeCoordinationLog(command, failing);        
    }

    @Override
    public CoordinationExecutionResult visitCloudExecutionCommand(CoordinationCommand command) {
        return null;
    }

}
