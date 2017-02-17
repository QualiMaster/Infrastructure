package eu.qualimaster.adaptation;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import net.ssehub.easy.instantiation.rt.core.model.rtVil.RtVILMemoryStorage;
import net.ssehub.easy.instantiation.rt.core.model.rtVil.RtVilStorage;
import eu.qualimaster.adaptation.TestHandler.ITestHandler;
import eu.qualimaster.adaptation.events.AdaptationEvent;
import eu.qualimaster.adaptation.events.AlgorithmConfigurationAdaptationEvent;
import eu.qualimaster.adaptation.events.CheckBeforeStartupAdaptationEvent;
import eu.qualimaster.adaptation.events.HandlerAdaptationEvent;
import eu.qualimaster.adaptation.events.ParameterConfigurationAdaptationEvent;
import eu.qualimaster.adaptation.events.ReplayAdaptationEvent;
import eu.qualimaster.adaptation.events.ResourceChangeAdaptationEvent;
import eu.qualimaster.adaptation.events.ShutdownAdaptationEvent;
import eu.qualimaster.adaptation.events.StartupAdaptationEvent;
import eu.qualimaster.adaptation.events.WrappingRequestMessageAdaptationEvent;
import eu.qualimaster.adaptation.external.AlgorithmChangedMessage;
import eu.qualimaster.adaptation.external.ChangeParameterRequest;
import eu.qualimaster.adaptation.external.DispatcherAdapter;
import eu.qualimaster.adaptation.external.ExecutionResponseMessage;
import eu.qualimaster.adaptation.external.HardwareAliveMessage;
import eu.qualimaster.adaptation.external.InformationMessage;
import eu.qualimaster.adaptation.external.Logging;
import eu.qualimaster.adaptation.external.LoggingFilterRequest;
import eu.qualimaster.adaptation.external.LoggingMessage;
import eu.qualimaster.adaptation.external.Message;
import eu.qualimaster.adaptation.external.MonitoringDataMessage;
import eu.qualimaster.adaptation.external.PipelineMessage;
import eu.qualimaster.adaptation.external.PipelineStatusRequest;
import eu.qualimaster.adaptation.external.PipelineStatusResponse;
import eu.qualimaster.adaptation.external.ReplayMessage;
import eu.qualimaster.adaptation.external.ResourceChangeRequest;
import eu.qualimaster.adaptation.external.SwitchAlgorithmRequest;
import eu.qualimaster.adaptation.external.UpdateCloudResourceMessage;
import eu.qualimaster.adaptation.internal.AdaptationLoggerFactory;
import eu.qualimaster.adaptation.internal.HilariousAuthenticationProvider;
import eu.qualimaster.adaptation.internal.IAuthenticationProvider;
import eu.qualimaster.adaptation.internal.ServerEndpoint;
import eu.qualimaster.adaptation.reflective.ReflectiveAdaptationManager;
import eu.qualimaster.coordination.CoordinationManager;
import eu.qualimaster.coordination.INameMapping;
import eu.qualimaster.coordination.commands.CoordinationCommand;
import eu.qualimaster.coordination.commands.PipelineCommand;
import eu.qualimaster.coordination.events.CoordinationCommandExecutionEvent;
import eu.qualimaster.dataManagement.events.IShutdownListener;
import eu.qualimaster.dataManagement.events.ShutdownEvent;
import eu.qualimaster.events.EventHandler;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.logging.events.LoggingEvent;
import eu.qualimaster.logging.events.LoggingFilterEvent;
import eu.qualimaster.monitoring.events.AlgorithmChangedMonitoringEvent;
import eu.qualimaster.monitoring.events.CloudResourceMonitoringEvent;
import eu.qualimaster.monitoring.events.HardwareAliveEvent;
import eu.qualimaster.monitoring.events.MonitoringEvent;
import eu.qualimaster.monitoring.events.MonitoringInformationEvent;
import eu.qualimaster.monitoring.events.ParameterChangedMonitoringEvent;
import eu.qualimaster.monitoring.utils.IScheduler;

/**
 * Realizes the external interface of the adaptation manager.
 * 
 * @author Holger Eichelberger
 */
public class AdaptationManager {

    private static ServerEndpoint endpoint;
    private static IAuthenticationProvider authProvider = HilariousAuthenticationProvider.INSTANCE;
    private static CoordinationCommandHandler coordinationCommandHandler = new CoordinationCommandHandler();
    private static CoordinationCommandExecutionHandler coordinationCommandExecutionHandler 
        = new CoordinationCommandExecutionHandler();
    private static Set<String> activePipelines = new HashSet<String>();
    private static IShutdownListener shutdownListener = null;
    private static Timer timer;
   
    private static IScheduler scheduler = new IScheduler() {
        
        @Override
        public void schedule(TimerTask task, Date firstTime, long period) {
            if (null != timer) {
                timer.schedule(task, firstTime, period);
            }
        }

    };

    /**
     * The handler for adaptation events.
     * 
     * @author Holger Eichelberger
     */
    private static class AdaptationEventHandler extends EventHandler<AdaptationEvent> {

        /**
         * Creates an adaptation event handler.
         */
        protected AdaptationEventHandler() {
            super(AdaptationEvent.class);
        }

        @Override
        protected void handle(AdaptationEvent event) {
            if (null != endpoint) {
                InformationMessage msg = AdaptationEventInformationMessageConverter.toMessage(event);
                if (null != msg) {
                    endpoint.schedule(msg);
                }
            }
            AdaptationManager.handleEvent(event);
        }
        
    }
    
    /**
     * The handler for pipeline lifecycle events.
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
            switch (event.getStatus()) {
            case STARTING:
                // clean up left-over
                synchronized (activePipelines) {
                    activePipelines.remove(event.getPipeline());
                }
                AdaptationEventQueue.notifyStopping(event.getPipeline());
                break;
            case CHECKING:
                handleEvent(new CheckBeforeStartupAdaptationEvent(event));
                break;
            case CREATED:
                AdaptationEventQueue.setAdaptationFilter(event.getPipeline(), event.getAdaptationFilter());
                // individually initialized algorithms will cause the switch to INITIALIZED in Thrift Monitoring
                handleEvent(new StartupAdaptationEvent(event.getPipeline(), event.getMainPipeline()));
                break;
            case STARTED:
                synchronized (activePipelines) {
                    activePipelines.add(event.getPipeline());
                }
                break;
            case STOPPING:
                handleEvent(new ShutdownAdaptationEvent(event.getPipeline(), false));
                synchronized (activePipelines) {
                    activePipelines.remove(event.getPipeline());
                }
                AdaptationEventQueue.notifyStopping(event.getPipeline());
                break;
            case STOPPED:
                handleEvent(new ShutdownAdaptationEvent(event.getPipeline(), true));
                break;
            default:
                break;
            }
            send(new InformationMessage(event.getPipeline(), null, "Lifecycle phase " + event.getStatus() 
                + " entered."));
        }
        
    }
    
    /**
     * Handles monitoring adaptation events by sending them directly to the clients.
     * 
     * @author Holger Eichelberger
     */
    private static class MonitoringInformationEventHandler extends EventHandler<MonitoringInformationEvent> {

        /**
         * Creates an event handler instance.
         */
        protected MonitoringInformationEventHandler() {
            super(MonitoringInformationEvent.class);
        }

        @Override
        protected void handle(MonitoringInformationEvent event) {
            // receive and directly send to configuration tool
            MonitoringDataMessage data = new MonitoringDataMessage(event.getPart(), event.getObservations());
            send(data);
        }
        
    }

    /**
     * The handler for monitoring events. This class is preliminary for the demo.
     * 
     * @author Holger Eichelberger
     */
    @SuppressWarnings("unused")
    private static class MonitoringEventHandler extends EventHandler<MonitoringEvent> {
        
        // TODO preliminary, remove later - inefficient!
        
        /**
         * Creates a monitoring event handler.
         */
        protected MonitoringEventHandler() {
            super(MonitoringEvent.class);
        }

        @Override
        protected void handle(MonitoringEvent event) {
            if (event instanceof AlgorithmChangedMonitoringEvent) {
                AlgorithmChangedMonitoringEvent evt = (AlgorithmChangedMonitoringEvent) event;
                send(new AlgorithmChangedMessage(evt.getPipeline(), evt.getPipelineElement(), evt.getAlgorithm()));
            } else if (event instanceof HardwareAliveEvent) {
                HardwareAliveEvent evt = (HardwareAliveEvent) event;
                send(new HardwareAliveMessage(evt.getIdentifier()));
            } else if (event instanceof ParameterChangedMonitoringEvent) {
                ParameterChangedMonitoringEvent evt = (ParameterChangedMonitoringEvent) event;
                send(new InformationMessage(evt.getPipeline(), evt.getPipelineElement(), "parameter " 
                    + evt.getParameter() + " changed to " + evt.getValue()));
            }
        }
    }

    /**
     * The handler for logging events.
     * 
     * @author Holger Eichelberger
     */
    @SuppressWarnings("unused")
    private static class LoggingEventHandler extends EventHandler<LoggingEvent> {

        /**
         * Creates a logging event handler.
         */
        protected LoggingEventHandler() {
            super(LoggingEvent.class);
        }

        @Override
        protected void handle(LoggingEvent event) {
            LoggingMessage msg = new LoggingMessage(event.getTimeStamp(), event.getLevel(), event.getMessage(), 
                event.getThreadName(), event.getHostAddress());
            send(msg);
        }
        
    }
    
    /**
     * Implements a handler for coordination command execution events.
     * 
     * @author Holger Eichelberger
     */
    private static class CoordinationCommandExecutionEventHandler 
        extends EventHandler<CoordinationCommandExecutionEvent> {

        /**
         * Creates a coordination command execution event handler.
         */
        protected CoordinationCommandExecutionEventHandler() {
            super(CoordinationCommandExecutionEvent.class);
        }

        @Override
        protected void handle(CoordinationCommandExecutionEvent event) {
            AdaptationFiltering.modifyPipelineElementFilters(event.getCommand(), false);
            handleEvent(new HandlerAdaptationEvent<CoordinationCommandExecutionEvent>(
                event, coordinationCommandExecutionHandler));
        }
        
    }
   
    /**
     * Observes all commands to infer those from CLI.
     * 
     * @author Holger Eichelberger
     */
    private static class CoordinationCommandEventHandler extends EventHandler<CoordinationCommand> {

        /**
         * Creates an event handler.
         */
        protected CoordinationCommandEventHandler() {
            super(CoordinationCommand.class);
        }

        @Override
        protected void handle(CoordinationCommand command) {
            handleEvent(new HandlerAdaptationEvent<CoordinationCommand>(command, coordinationCommandHandler));
        }
        
    }
    
    /**
     * Implements the handling of {@link AlgorithmChangedMonitoringEvent}.
     * 
     * @author Holger Eichelberger
     */
    private static class AlgorithmChangedMonitoringEventHandler extends EventHandler<AlgorithmChangedMonitoringEvent> {

        /**
         * Creates an instance.
         */
        private AlgorithmChangedMonitoringEventHandler() {
            super(AlgorithmChangedMonitoringEvent.class);
        }

        @Override
        protected void handle(AlgorithmChangedMonitoringEvent event) {
            AdaptationEventQueue.notifyStartupAlgorithmChangedEvent(event);
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
            if (null != shutdownListener) {
                shutdownListener.notifyShutdown(event);
            }
            AdaptationManager.stop();
        }
        
    }
    
    /**
     * Defines or undefines the shutdown listener.
     * 
     * @param listener the listener (resets listening if <b>null</b>)
     */
    public static void setShutdownListener(IShutdownListener listener) {
        shutdownListener = listener;
    }
    
    /**
     * Register the event handler statically.
     */
    static {
        EventManager.register(new AdaptationEventHandler());
        EventManager.register(new PipelineLifecycleEventEventHandler());
        //EventManager.register(new MonitoringEventHandler());
        //EventManager.register(new LoggingEventHandler()); // TODO disable in production ??

        EventManager.register(new CoordinationCommandExecutionEventHandler());
        EventManager.register(new CoordinationCommandEventHandler());
        EventManager.register(new MonitoringInformationEventHandler());
        EventManager.register(new ShutdownEventHandler());
        EventManager.register(new AlgorithmChangedMonitoringEventHandler());
        // not AlgorithmChangedMonitoringEventHandler.INSTANCE as dynamic
    }

    /**
     * Handles an adaptation event.
     * 
     * @param event the event to be handled
     */
    public static void handleEvent(AdaptationEvent event) {
        if (null != event) {
            AdaptationEventQueue.add(event);
        }
    }
    
    /**
     * A simple dispatcher for external messages sent from the configuration tool. (public for testing)
     * 
     * @author Holger Eichelberger
     */
    public static class AdaptationDispatcher extends DispatcherAdapter {

        private IAuthenticationCallback callback = IAuthenticationCallback.DEFAULT;
        
        /**
         * Defines the authentication callback.
         * 
         * @param callback the callback (<b>null</b> is ignored)
         */
        public void setAuthenticationCallback(IAuthenticationCallback callback) {
            if (null != callback) {
                this.callback = callback;
            }
        }
        
        @Override
        public void handleSwitchAlgorithmRequest(SwitchAlgorithmRequest msg) {
            // message type indicates user trigger
            AlgorithmConfigurationAdaptationEvent evt = new AlgorithmConfigurationAdaptationEvent(msg.getPipeline(), 
                msg.getPipelineElement(), msg.getNewAlgorithm(), !callback.isAuthenticated(msg)); 
            handleEvent(new WrappingRequestMessageAdaptationEvent(msg, evt));
        }

        @Override
        public void handleChangeParameterRequest(ChangeParameterRequest<?> msg) {
            // message type indicates user trigger
            ParameterConfigurationAdaptationEvent evt = new ParameterConfigurationAdaptationEvent(msg.getPipeline(), 
                msg.getPipelineElement(), msg.getParameter(), msg.getValue(), !callback.isAuthenticated(msg)); 
            handleEvent(new WrappingRequestMessageAdaptationEvent(msg, evt));
        }
        
        @Override
        public void handleReplayMessage(ReplayMessage msg) {
            handleEvent(new WrappingRequestMessageAdaptationEvent(msg, new ReplayAdaptationEvent(msg)));
        }

        @Override
        public void handlePipelineMessage(PipelineMessage msg) {
            PipelineCommand.Status pStatus = null;
            if (null != msg.getStatus()) {
                switch (msg.getStatus()) {
                case START:
                    pStatus = PipelineCommand.Status.START;
                    break;
                case STOP:
                    pStatus = PipelineCommand.Status.STOP;
                    break;
                default:
                    pStatus = null;
                    break;
                }
                EventManager.send(new PipelineCommand(msg.getPipeline(), pStatus));
            }
        }

        @Override
        public void handleLoggingFilterRequest(LoggingFilterRequest msg) {
            EventManager.send(new LoggingFilterEvent(msg.getFilterAdditions(), msg.getFilterRemovals()));
        }
        
        @Override
        public void handlePipelineStatusRequest(PipelineStatusRequest msg) {
            String[] pipelines;
            synchronized (activePipelines) {
                pipelines = new String[activePipelines.size()];
                activePipelines.toArray(pipelines);    
            }
            send(new PipelineStatusResponse(msg, pipelines));
        }
        
        @Override
        public void handleUpdateCloudResourceMessage(UpdateCloudResourceMessage msg) {                         
            EventManager.send(new CloudResourceMonitoringEvent(msg.getName(), msg.getObservations()));     
        }
        
        @Override
        public void handleResourceChangeMessage(ResourceChangeRequest msg) {
            ResourceChangeAdaptationEvent.Status status;
            switch (msg.getStatus()) {
            case ADDED:
                status = ResourceChangeAdaptationEvent.Status.ADDED;
                break;
            case ENABLED:
                status = ResourceChangeAdaptationEvent.Status.ENABLED;
                break;
            case DISABLED:
                status = ResourceChangeAdaptationEvent.Status.DISABLED;
                break;
            case REMOVED:
                status = ResourceChangeAdaptationEvent.Status.REMOVED;
                break;
            default:
                status = null;
                break;
            }
            if (null != status) {
                handleEvent(new WrappingRequestMessageAdaptationEvent(msg, 
                    new ResourceChangeAdaptationEvent(msg.getResource(), status)));
            }
        }
        
    }

    /**
     * May be an event.
     */
    public static void start() {
        Logging.setBack(Log4jLoggingBack.INSTANCE);
        RtVilStorage.setInstance(new RtVILMemoryStorage()); // TODO switch to QmRtVILStorageProvider
        try {
            AdaptationDispatcher dispatcher = new AdaptationDispatcher();
            endpoint = new ServerEndpoint(dispatcher, AdaptationConfiguration.getAdaptationPort(), authProvider);
            dispatcher.setAuthenticationCallback(endpoint);
            endpoint.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        timer = new Timer();
        AdaptationEventQueue.start();
        ReflectiveAdaptationManager.start(scheduler);
    }
    
    /**
     * May be an event.
     */
    public static void stop() {
        ReflectiveAdaptationManager.stop();
        AdaptationEventQueue.stop();
        AdaptationLoggerFactory.closeLogger();
        if (null != endpoint) {
            endpoint.stop();
        }
        if (null != timer)  {
            timer.cancel();
            timer = null;
        }
    }
    
    /**
     * Returns the server endpoint.
     * 
     * @return the server endpoint
     */
    static ServerEndpoint getEndpoint() {
        return endpoint;
    }
    
    /**
     * Sends a message.
     * 
     * @param message the message
     */
    public static void send(Message message) {
        if (null != endpoint) {
            endpoint.schedule(message);
        }
    }
    
    /**
     * Sends an explicit execution response message.
     * 
     * @param message the message
     */
    static void send(ExecutionResponseMessage message) {
        send((Message) message);
        ITestHandler handler = TestHandler.getHandler();
        if (null != handler) {
            handler.handle(message);
        }
    }
    
    /**
     * Defines the authentication provider.
     *  
     * @param provider the new provider (only effective before {@link #start()} and if <code>provider</code> 
     *     is not <b>null</b>)
     */
    public static void setAuthenticationProvider(IAuthenticationProvider provider) {
        if (null != provider && null == endpoint) {
            authProvider = provider;
        }
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

}
