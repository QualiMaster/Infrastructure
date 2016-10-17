package eu.qualimaster.infrastructure;

import eu.qualimaster.adaptation.events.AdaptationEvent;
import eu.qualimaster.common.QMInternal;
import eu.qualimaster.events.IReturnableEvent;

/**
 * Issues when the lifecycle of a pipeline changes.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public class PipelineLifecycleEvent extends InfrastructureEvent {

    /**
     * The actual pipeline status.
     * 
     * @author Holger Eichelberger
     */
    public enum Status {
        
        /**
         * The pipeline shall be checked for resource consumption and potential layout onto the cluster.
         * Either the adaptation layer stops it here or the next state is {@link #CHECKED}.
         */
        CHECKING(false, false),
        
        /**
         * The pipeline is checked for resource consumption. Next state is {@link STARTING}.
         */
        CHECKED(false, false),
        
        /**
         * The pipeline is about to start but disconnected from the sources. 
         * Default algorithms and parameters may be set.
         */
        STARTING(false, false),
        
        /**
         * The pipeline is created and set into force by the underlying execution system. Form a plain Storm 
         * perspective, the pipeline shall now be CONNECTED and processing data. Due to the decision for the actual 
         * algorithms through adaptation (all are decided means {@link #INITIALIZED}) during the startup of a pipeline, 
         * this requires connecting the sources / sinks before and, thus, a switch to {@link #INITIALIZED} and then 
         * {@link #STARTED}.
         */
        CREATED(false, false),
        
        /**
         * All algorithm families have received their initial members / parameters and are ready 
         * for execution.  
         */
        INITIALIZED(false, false),
        
        /**
         * The pipeline is connected to the sources and started.
         */
        STARTED(true, false),
        
        /**
         * The pipeline is about to stop and already disconnected from the sources.
         */
        STOPPING(true, true),
        
        /**
         * The pipeline is stopped.
         */
        STOPPED(true, true),
        
        /**
         * If the pipeline is not actually {@link #STOPPING}, this means that
         * monitoring lost connection to the pipeline and the pipeline may be dead or killed somehow.
         * This state occurs only if the pipeline {@link #wasStarted()}. 
         */
        DISAPPEARED(true, false), 
        
        /**
         * If the state of the pipeline is unknown. This is not a status to be used with a 
         * {@link PipelineLifecycleEvent} and may be ignored by implementations, rather than 
         * an initial state for monitoring.
         */
        UNKNOWN(false, false);
        
        private boolean wasStarted;
        private boolean isShuttingDown;
        
        /**
         * Creates a status.
         * 
         * @param wasStarted whether the pipeline must reach {@link #STARTED} to enter this state.
         * @param isShuttingDown whether a pipeline in this state is shutting down / was shut down
         */
        private Status(boolean wasStarted, boolean isShuttingDown) {
            this.wasStarted = wasStarted;
            this.isShuttingDown = isShuttingDown;
        }
        
        /**
         * Returns whether this state indicates that a pipeline entered the {@link #STARTED} state before
         * this state.
         * 
         * @return <code>true</code> if the pipeline entered {@link #STARTED} before, <code>false</code> else
         */
        public boolean wasStarted() {
            return wasStarted;
        }
        
        /**
         * Whether this state indicates that a pipeline is/was shutting down.
         * 
         * @return <code>true</code> for shutting down, <code>false</code> else
         */
        public boolean isShuttingDown() {
            return isShuttingDown;
        }
        
    }
    
    private static final long serialVersionUID = 4383552395939016071L;
    private String pipeline;
    private Status status;
    private String adaptationFilterName;
    private String causeSenderId;
    private String causeMessageId;
    private PipelineOptions options;

    /**
     * Creates a pipeline lifecycle event with no options (adaptation enabled).
     * 
     * @param pipeline the name of the pipeline
     * @param status the actual status
     * @param cause the causing returnable event (may be <b>null</b> if not known)
     */
    public PipelineLifecycleEvent(String pipeline, Status status, IReturnableEvent cause) {
        this(pipeline, status, (String) null, cause);
    }

    /**
     * Creates a pipeline lifecycle event with no options.
     * 
     * @param pipeline the name of the pipeline
     * @param status the actual status
     * @param adaptationFilter the adaptation filter class, <b>null</b> if there is no adaptation filter
     * @param cause the causing returnable event (may be <b>null</b> if not known)
     */
    public PipelineLifecycleEvent(String pipeline, Status status, Class<? extends AdaptationEvent> adaptationFilter, 
        IReturnableEvent cause) {
        this(pipeline, status, null == adaptationFilter ? null : adaptationFilter.getName(), cause);
    }
    
    /**
     * Creates a pipeline lifecycle event with no options.
     * 
     * @param pipeline the name of the pipeline
     * @param status the actual status
     * @param adaptationFilterName the name of the adaptation filter class, <b>null</b> if there is no adaptation 
     *     filter
     * @param cause the causing returnable event (may be <b>null</b> if not known)
     */
    public PipelineLifecycleEvent(String pipeline, Status status, String adaptationFilterName, 
        IReturnableEvent cause) {
        this.pipeline = pipeline;
        this.status = status;
        this.adaptationFilterName = adaptationFilterName;
        if (null != cause) {
            this.causeSenderId = cause.getSenderId();
            this.causeMessageId = cause.getMessageId();
        }
    }

    /**
     * Creates a pipeline lifecycle event with pipeline options.
     * 
     * @param pipeline the name of the pipeline
     * @param status the actual status
     * @param options the pipeline options (may be <b>null</b>)
     * @param cause the causing returnable event (may be <b>null</b> if not known)
     */
    public PipelineLifecycleEvent(String pipeline, Status status, PipelineOptions options, 
        IReturnableEvent cause) {
        this(pipeline, status, options.getAdaptationFilterName(), cause);
        this.options = options;
    }

    /**
     * Creates a pipeline lifecycle event based on an existing event. Takes over all information
     * from <code>event</code> and sets <code>status</code> as given.
     * 
     * @param event takes over the information from the given event
     * @param status the new status
     * @param options the pipeline options (may be <b>null</b>)
     */
    public PipelineLifecycleEvent(PipelineLifecycleEvent event, Status status, PipelineOptions options) {
        this.pipeline = event.pipeline;
        this.adaptationFilterName = event.adaptationFilterName;
        this.options = options;
        this.causeSenderId = event.causeSenderId;
        this.causeMessageId = event.causeMessageId;
        this.status = status;
    }
    
    /**
     * Creates a pipeline lifecycle event based on an existing event. Takes over all information
     * from <code>event</code> and sets <code>status</code> as given. Passes on the pipeline options.
     * 
     * @param event takes over the information from the given event
     * @param status the new status
     * @see #PipelineLifecycleEvent(PipelineLifecycleEvent, Status, PipelineOptions)
     */
    public PipelineLifecycleEvent(PipelineLifecycleEvent event, Status status) {
        this(event, status, event.options);
    }

    /**
     * Returns the causing sender id for sending return events at a certain lifecycle status.
     * 
     * @return the causing sender id, may be <b>null</b> if not known
     */
    public String getCauseSenderId() {
        return causeSenderId;
    }

    /**
     * Returns the causing message id for sending return events at a certain lifecycle status.
     * 
     * @return the causing message id, may be <b>null</b> if not known
     */
    public String getCauseMessageId() {
        return causeMessageId;
    }
    
    /**
     * Returns the name of the adaptation filter class.
     * 
     * @return the name of the adaptation filter class, <b>null</b> if there is no adaptation filter
     */
    public String getAdaptationFilterName() {
        return adaptationFilterName;
    }

    /**
     * Returns the adaptation filter class.
     * 
     * @return the adaptation filter class, <b>null</b> if there is no adaptation filter
     */
    public Class<? extends AdaptationEvent> getAdaptationFilter() {
        return PipelineOptions.getAdaptationFilter(getAdaptationFilterName());
    }

    /**
     * The name of the pipeline being affected.
     * 
     * @return the pipeline name
     */
    public String getPipeline() {
        return pipeline;
    }

    /**
     * Returns the status of the specified pipeline.
     * 
     * @return the status
     */
    public Status getStatus() {
        return status;
    }
    
    /**
     * The actual pipeline options.
     * 
     * @return the pipeline options (may be <b>null</b>)
     */
    public PipelineOptions getOptions() {
        return options;
    }
    
}
