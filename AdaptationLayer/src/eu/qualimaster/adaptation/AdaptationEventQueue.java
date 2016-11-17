package eu.qualimaster.adaptation;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import net.ssehub.easy.instantiation.core.model.buildlangModel.ITracer;
import net.ssehub.easy.instantiation.core.model.common.VilException;
import net.ssehub.easy.instantiation.core.model.execution.IInstantiatorTracer;
import net.ssehub.easy.instantiation.core.model.execution.TracerFactory;
import net.ssehub.easy.instantiation.rt.core.model.rtVil.Executor;
import net.ssehub.easy.instantiation.rt.core.model.rtVil.RtVilExecution;
import net.ssehub.easy.instantiation.rt.core.model.rtVil.Script;
import net.ssehub.easy.reasoning.core.reasoner.ReasonerConfiguration.IAdditionalInformationLogger;
import net.ssehub.easy.varModel.confModel.Configuration;
import net.ssehub.easy.instantiation.core.model.tracing.ConsoleTracerFactory;
import net.ssehub.easy.instantiation.core.model.vilTypes.configuration.VariableValueMapping;
import eu.qualimaster.adaptation.events.AdaptationEvent;
import eu.qualimaster.adaptation.events.CheckBeforeStartupAdaptationEvent;
import eu.qualimaster.adaptation.events.HandlerAdaptationEvent;
import eu.qualimaster.adaptation.events.IPipelineAdaptationEvent;
import eu.qualimaster.adaptation.events.StartupAdaptationEvent;
import eu.qualimaster.adaptation.events.WrappingRequestMessageAdaptationEvent;
import eu.qualimaster.adaptation.external.RequestMessage;
import eu.qualimaster.adaptation.internal.AdaptationLoggerFactory;
import eu.qualimaster.adaptation.internal.IAdaptationLogger;
import eu.qualimaster.adaptation.internal.ReasoningHook;
import eu.qualimaster.adaptation.internal.RtVilValueMapping;
import eu.qualimaster.coordination.InitializationMode;
import eu.qualimaster.coordination.RepositoryConnector;
import eu.qualimaster.coordination.RepositoryConnector.Models;
import eu.qualimaster.coordination.RepositoryConnector.Phase;
import eu.qualimaster.coordination.RepositoryHelper;
import eu.qualimaster.coordination.commands.CoordinationCommand;
import eu.qualimaster.coordination.events.CoordinationCommandExecutionEvent;
import eu.qualimaster.easy.extension.internal.PipelineHelper;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.monitoring.MonitoringManager;
import eu.qualimaster.monitoring.events.AlgorithmChangedMonitoringEvent;
import eu.qualimaster.monitoring.events.ConstraintViolationAdaptationEvent;
import eu.qualimaster.monitoring.events.FrozenSystemState;

/**
 * Implements the adaptation event queue and the main event consumer.
 * 
 * @author Holger Eichelberger
 */
public class AdaptationEventQueue {

    private static final boolean WITH_REASONING 
        = Boolean.valueOf(System.getProperty("qm.adaptation.reasoning", "true"));
    private static final boolean WITH_DEBUG = Boolean.valueOf(System.getProperty("qm.adaptation.debug", "false"));
    private static final Logger LOGGER = LogManager.getLogger(AdaptationEventQueue.class);
    private static BlockingDeque<AdaptationEvent> adaptationEventQueue = new LinkedBlockingDeque<>();
    private static Map<String, Class<? extends AdaptationEvent>> adaptationFilters 
        = Collections.synchronizedMap(new HashMap<String, Class<? extends AdaptationEvent>>());
    private static Map<String, Map<String, AlgorithmChangedMonitoringEvent>> startupAlgorithmChangedEvents 
        = Collections.synchronizedMap(new HashMap<String, Map<String, AlgorithmChangedMonitoringEvent>>());

    private static final int RESPONSE_TIMEOUT = AdaptationConfiguration.getEventResponseTimeout();
    private static MessageResponseStore messageStore = new MessageResponseStore(RESPONSE_TIMEOUT);
    private static AdaptationEventResponseStore eventStore = new AdaptationEventResponseStore(RESPONSE_TIMEOUT);
    private static CommandResponseStore commandStore = new CommandResponseStore(RESPONSE_TIMEOUT);
    private static EventConsumer consumer;
    private static int debugFileCount = 0;
    private static InformationMessageVisitor cmdVisitor = new InformationMessageVisitor(null);
    private static RtVilValueMapping rtVilMapping = new RtVilValueMapping();

    private static final TracerFactory ADAPTATION_TRACER_FACTORY = new TracerFactory() {

        private final TracerFactory current = TracerFactory.getInstance();
        
        @Override
        public ITracer createBuildLanguageTracerImpl() {
            return AdaptationLoggerFactory.createTracer(current.createBuildLanguageTracerImpl());
        }

        @Override
        public IInstantiatorTracer createInstantiatorTracerImpl() {
            return current.createInstantiatorTracerImpl();
        }

        @Override
        public net.ssehub.easy.instantiation.core.model.templateModel.ITracer 
            createTemplateLanguageTracerImpl() {
            return current.createTemplateLanguageTracerImpl();
        }

    };

    static {
        VariableValueMapping.setInstance(rtVilMapping);
        RtVilExecution.REASONER_CONFIGURATION.setAdditionalInformationLogger(new IAdditionalInformationLogger() {
            
            @Override
            public void info(String text) {
                // no output, print for debugging
            }
        });
    }
    
    /**
     * Notifies that a pipeline is stopping.
     * 
     * @param pipelineName the name of the pipeline
     */
    static void notifyStopping(String pipelineName) {
        adaptationFilters.remove(pipelineName);
    }
    
    /**
     * Sets the adaptation filter for a certain pipeline. If no filter is set, a pipeline is enabled for adaptation
     * by default.
     * 
     * @param pipelineName the name of the pipeline
     * @param adaptationFilter the adaptation filter, may be <b>null</b> if there is none
     */
    static void setAdaptationFilter(String pipelineName, Class<? extends AdaptationEvent> adaptationFilter) {
        adaptationFilters.put(pipelineName, adaptationFilter);
    }
    
    /**
     * Adds an adaptation event for processing. Filters out {@link IPipelineAdaptationEvent pipeline events} that are
     * not enabled for adaptation.
     * 
     * @param event the event to be added
     */
    static void add(AdaptationEvent event) {
        boolean add = true;
        AdaptationEvent tmp = event;
        if (null != tmp) {
            tmp = tmp.unpack();
        }
        if (tmp instanceof IPipelineAdaptationEvent) {
            String pipName = ((IPipelineAdaptationEvent) tmp).getPipeline();
            if (null != pipName) {
                Class<? extends AdaptationEvent> filter = adaptationFilters.get(pipName);
                add = (null == filter || !filter.isInstance(tmp));
            }
        }
        if (add) {
            adaptationEventQueue.addLast(event);
        }
    }
    
    /**
     * Starts the event queue. (public for testing)
     */
    public static void start() {
        consumer = new EventConsumer();
        Thread t = new Thread(consumer);
        t.start();
    }
    
    /**
     * Returns the actual event queue size. (public for testing)
     * 
     * @return the event queue size
     */
    public static int getEventQueueSize() {
        return adaptationEventQueue.size();
    }
    
    /**
     * Stops the event queue. (public for testing)
     */
    public static void stop() {
        if (null != consumer) {
            consumer.stop();
        }
    }
    
    /**
     * Performs the event queue handling, i.e., removing events from the queue
     * and executing them via rt-VIL.
     * 
     * @author Holger Eichelberger
     */
    private static class EventConsumer implements Runnable {

        private boolean isRunning = true;
        private boolean errorMessageDone = false;
        private File tmp;
        
        /**
         * Creates and initializes an event consumer.
         */
        private EventConsumer() {
            tmp = RepositoryConnector.createTmpFolder();
        }
        
        @Override
        public void run() {
            while (isRunning) {
                try {
                    if (!adaptationEventQueue.isEmpty()) {
                        boolean adapt = true;
                        AdaptationEvent event = adaptationEventQueue.removeFirst();
                        if (event instanceof WrappingRequestMessageAdaptationEvent) {
                            WrappingRequestMessageAdaptationEvent wrapper 
                                = (WrappingRequestMessageAdaptationEvent) event;
                            messageStore.setCurrentRequest(wrapper.getMessage());
                            event = wrapper.getAdaptationEvent();
                        } else if (event instanceof HandlerAdaptationEvent) {
                            ((HandlerAdaptationEvent<?>) event).handle();
                            adapt = false;
                        } else {
                            eventStore.setCurrentRequest(event);
                        }
                        Models models = RepositoryConnector.getModels(Phase.ADAPTATION);
                        if (null == models) {
                            if (!errorMessageDone) {
                                LOGGER.error("Cannot load adaptation model - skipping adaptation");
                                errorMessageDone = true;
                            }
                            if (event instanceof CheckBeforeStartupAdaptationEvent) {
                                event.adjustLifecycle(null, null); // don't stop here
                            }
                        } else if (adapt) {
                            models.startUsing();
                            Configuration config = models.getConfiguration();
                            Script rtVilModel = models.getAdaptationScript();
                            if (AdaptationFiltering.isEnabled(config, event)) {
                                adapt(event, config, rtVilModel, tmp);
                                messageStore.setCurrentRequest(null);
                                eventStore.setCurrentRequest(null);
                                messageStore.clear();
                                eventStore.clear();
                                commandStore.clear();
                            }
                            models.endUsing();
                        }
                    }
                    Thread.sleep(20); // TODO possibly adjust sleep to adaptation frequency?
                } catch (InterruptedException e) {
                }
            }
        }
        
        /**
         * Stops the event consumer.
         */
        private void stop() {
            isRunning = false;
        }
        
    }
    
    // checkstyle: stop exception type check

    /**
     * Performs the adaptation. [public for testing]
     * 
     * @param event the causing adaptation event
     * @param config the runtime configuration
     * @param rtVilModel the adaptation model
     * @param tmp the temporary folder for file-based instantiation
     */
    public static void adapt(AdaptationEvent event, Configuration config, Script rtVilModel, File tmp) {
        if (EventManager.shallBeLogged(event)) {
            LOGGER.info("handling " + event.getClass().getName());
        }
        adaptImpl(event, config, rtVilModel, tmp);
        if (EventManager.shallBeLogged(event)) {
            LOGGER.info("handling done for " + event.getClass().getName());
        }
    }
    
    
    /**
     * Performs the adaptation.
     * 
     * @param event the causing adaptation event
     * @param config the runtime configuration
     * @param rtVilModel the adaptation model
     * @param tmp the temporary folder for file-based instantiation
     */
    private static void adaptImpl(AdaptationEvent event, Configuration config, Script rtVilModel, File tmp) {
        if (null != config && null != rtVilModel) {
            FrozenSystemState state = null;
            if (event instanceof StartupAdaptationEvent) {
                initializePipeline(((StartupAdaptationEvent) event).getPipeline());
            }
            if (event instanceof ConstraintViolationAdaptationEvent) {
                ConstraintViolationAdaptationEvent evt = (ConstraintViolationAdaptationEvent) event;
                state = evt.getState();
            }
            if (null == state) {
                state = MonitoringManager.getSystemState().freeze();
            }
            rtVilMapping.setSystemState(state);
            FileUtils.deleteQuietly(tmp);
            tmp.mkdirs();
            boolean log = setTracerFactory();
            if (WITH_DEBUG) {
                String logLocation = AdaptationConfiguration.getMonitoringLogInfraLocation();
                if (!AdaptationConfiguration.isEmpty(logLocation)) { // TODO remove
                    File f = new File(logLocation, "adaptation_" + debugFileCount++);
                    try {
                        state.store(f);
                    } catch (IOException e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }
            }
            Executor exec = RepositoryHelper.createExecutor(rtVilModel, tmp, config, event, state);
            exec.setReasoningHook(ReasoningHook.INSTANCE);
            if (!AdaptationConfiguration.isReasoningEnabled() || !WITH_REASONING) {
                exec.disableReasoner();
            }
            try {
                exec.execute();
                String failReason = exec.getFailReason();
                Integer failCode = exec.getFailCode();
                if (!event.adjustLifecycle(failReason, failCode)) {
                    if (null != failReason || null != failCode) {
                        String msg = "";
                        if (null != failReason) {
                            msg += failReason;
                        }
                        if (null != failCode && msg.length() == 0) {
                            msg += "code " + failCode;
                        }
                        // TODO adaptation log
                        messageStore.sendResponse(msg); // nothing happens if no request
                        eventStore.sendResponse(msg); // nothing happens if no request
                    }
                }
            } catch (Throwable e) { // be extremely careful 
                // TODO adaptation log
                LOGGER.error("During adaptation: " + e.getMessage(), e);
            }
            if (log) {
                TracerFactory.setInstance(null); // thread-based reset
            }
            FileUtils.deleteQuietly(tmp);
        } else {
            LOGGER.info("Ignored event as neither infrastructure configuration nor rt-VIL model is available "
                + "(access disabled?): " + event);
        }
    }
    
    /**
     * Sets the tracer factory.
     * 
     * @return <code>true</code> if logging/tracing is enabled, <code>false</code> else
     */
    private static boolean setTracerFactory() {
        boolean log = AdaptationConfiguration.enableAdaptationRtVilLogging();
        if (log) {
            if (!setConfiguredTracerFactory()) {
                TracerFactory.setInstance(ConsoleTracerFactory.INSTANCE); // thread-based setting
            }
        } else {
            setConfiguredTracerFactory();
        }
        return log;
    }
    
    /**
     * Sets the configured tracer factory if one was configured.
     * 
     * @return the configured tracer factory
     */
    private static boolean setConfiguredTracerFactory() {
        String factory = AdaptationConfiguration.getAdaptationRtVilTracerFactory();
        boolean done = false;
        if (null != factory && !AdaptationConfiguration.isEmpty(factory)) {
            try {
                Class<?> cls = Class.forName(factory);
                Object inst = cls.newInstance();
                if (inst instanceof TracerFactory) {
                    TracerFactory.setInstance((TracerFactory) inst); // thread-based setting
                    done = true;
                } else {
                    LOGGER.info("Loading tracer factory: not instance of " + TracerFactory.class.getName());
                }
            } catch (ClassNotFoundException e) {
                LOGGER.info("Loading tracer factory " + factory + ":" + e.getMessage());
            } catch (InstantiationException e) {
                LOGGER.info("Loading tracer factory " + factory + ":" + e.getMessage());
            } catch (IllegalAccessException e) {
                LOGGER.info("Loading tracer factory " + factory + ":" + e.getMessage());
            }
        }
        if (!done) {
            TracerFactory.setInstance(ADAPTATION_TRACER_FACTORY);
        }
        return done;
    }

    // checkstyle: resume exception type check

    /**
     * Handles the reception of a coordination command execution event, i.e., retrieves it from the 
     * response tracking store, clears that entry and returns the related message.
     *  
     * @param event the received event
     * @return the related request message (may be <b>null</b>)
     */
    static RequestMessage getRequest(CoordinationCommandExecutionEvent event) {
        LOGGER.info("Processing infrastructure event " + event);
        return messageStore.received(event);
    }

    /**
     * Handles the reception of a coordination command execution event, i.e., retrieves it from the 
     * response tracking store, clears that entry and returns the related event.
     *  
     * @param event the received event
     * @return the related adaptation event (may be <b>null</b>)
     */
    static AdaptationEvent getEvent(CoordinationCommandExecutionEvent event) {
        LOGGER.info("Processing infrastructure event " + event);
        return eventStore.received(event);
    }
    
    /**
     * Checks for external commands.
     * 
     * @param command the coordination command to check for
     */
    static void checkForExternalCommand(CoordinationCommand command) {
        String msgId = command.getMessageId();
        boolean known = messageStore.registered(msgId) || eventStore.registered(msgId);
        LOGGER.info("External command known " + known + " " + command);
        //if (!known) { // either already removed or from CLI
        commandStore.sent(command);
        command.accept(cmdVisitor);
        //}
    }

    /**
     * Handles the reception of a coordination command execution event, i.e., retrieves it from the 
     * response tracking store, clears that entry and returns the related event.
     *  
     * @param event the received event
     * @return the related coordination command (may be <b>null</b>)
     */
    static CoordinationCommand getCommand(CoordinationCommandExecutionEvent event) {
        CoordinationCommand command = commandStore.received(event);
        LOGGER.info("Received " + event + " -> " + command);
        if (null != command) {
            cmdVisitor.setResponse(event);
            command.accept(cmdVisitor);
        }
        IAdaptationLogger logger = AdaptationLoggerFactory.getLogger();
        if (null != logger) {
            logger.enacted(command, event);
        }
        return command;
    }
    
    /**
     * Notifies about the reception for a start algorithm monitoring event. For registering the event, first 
     * {@link #notifyChecked(String)} must have been called.
     * 
     * @param event the algorithm changed monitoring event
     */
    static void notifyStartupAlgorithmChangedEvent(AlgorithmChangedMonitoringEvent event) {
        if (InitializationMode.DYNAMIC == AdaptationConfiguration.getInitializationMode()) {
            Map<String, AlgorithmChangedMonitoringEvent> evts = startupAlgorithmChangedEvents.get(event.getPipeline());
            String pipelineElement = event.getPipelineElement();
            if (null != evts && !evts.containsKey(pipelineElement)) { 
                evts.put(pipelineElement, event);
            }
        }
    }

    /**
     * Notifies that a pipeline reached the checked lifecycle. This method shall be called before the related lifecycle
     * event is issued.
     * 
     * @param pipelineName the pipeline name
     */
    public static void notifyChecked(String pipelineName) {
        if (InitializationMode.DYNAMIC == AdaptationConfiguration.getInitializationMode()) {
            Map<String, AlgorithmChangedMonitoringEvent> evts = startupAlgorithmChangedEvents.get(pipelineName);
            if (null == evts) { // no event registered so far
                evts = Collections.synchronizedMap(new HashMap<String, AlgorithmChangedMonitoringEvent>(10));
                startupAlgorithmChangedEvents.put(pipelineName, evts);
            }
        }
    }
    
    /**
     * Initializes the model for the given pipeline based on registered algorithm changed events.
     * 
     * @param pipelineName the pipeline name
     */
    private static void initializePipeline(String pipelineName) {
        Models models = RepositoryConnector.getModels(Phase.ADAPTATION);
        Map<String, AlgorithmChangedMonitoringEvent> evts = startupAlgorithmChangedEvents.remove(pipelineName);
        if (null != evts && null != models) { // may be the case if InitializationMode != DYNAMIC
            Configuration config = models.getConfiguration();
            for (AlgorithmChangedMonitoringEvent event : evts.values()) {
                String pipeline = event.getPipeline();
                String pipelineElement = event.getPipelineElement();
                String algorithm = event.getAlgorithm();
                try {
                    PipelineHelper.setActual(config, pipeline, pipelineElement, algorithm);
                } catch (VilException e) {
                    LOGGER.error("While setting initial actual algorithm " + algorithm + " on " 
                        + pipelineElement + " in " + pipeline + ": " + e.getMessage());
                }
            }
        }
    }
    
}
