package eu.qualimaster.monitoring.events;

import eu.qualimaster.common.QMInternal;
import eu.qualimaster.observables.IForwardedCoordinationCommand;
import eu.qualimaster.observables.IObservable;

/**
 * Causes a change in monitoring an observable.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public class ChangeMonitoringEvent extends AbstractPipelineElementMonitoringEvent 
    implements IForwardedCoordinationCommand {

    private static final long serialVersionUID = 3321516452071765608L;
    private IObservable observable;
    private boolean enabled;
    private long timestamp;
    private Boolean enableAlgorithmTracing; 

    /**
     * Creates a neutral monitoring event, i.e., a disabled timestamp and
     * continued enabled monitoring. 
     * 
     * @param enableAlgorithmTracing enables or disables algorithm tracing to files for obtaining 
     *     (initial/experimental/testing) profiles
     */
    public ChangeMonitoringEvent(boolean enableAlgorithmTracing) {
        this(true, 0);
        this.enableAlgorithmTracing = enableAlgorithmTracing;
    }
    
    /**
     * Changes the monitoring for the entire infrastructure.
     * 
     * @param enabled whether the observable shall be enabled or disabled
     * @param timestamp the coordination / enactment timestamp pointing to the respective coordination log entry
     */
    public ChangeMonitoringEvent(boolean enabled, long timestamp) {
        this(null, null, null, enabled, timestamp);
    }

    /**
     * Changes the monitoring of a resource for the entire infrastructure.
     * 
     * @param observable the observable to be changed (may be <b>null</b> for all)
     * @param enabled whether the observable shall be enabled or disabled
     * @param timestamp the coordination / enactment timestamp pointing to the respective coordination log entry
     */
    public ChangeMonitoringEvent(IObservable observable, boolean enabled, long timestamp) {
        this(null, null, observable, enabled, timestamp);
    }

    /**
     * Changes the monitoring for a <code>pipeline</code>.
     * 
     * @param pipeline the name of the pipeline (may be <b>null</b> for all)
     * @param observable the observable to be changed (may be <b>null</b> for all)
     * @param enabled whether the observable shall be enabled or disabled
     * @param timestamp the coordination / enactment timestamp pointing to the respective coordination log entry
     */
    public ChangeMonitoringEvent(String pipeline, IObservable observable, boolean enabled, long timestamp) {
        this(pipeline, null, observable, enabled, timestamp);
    }
    
    /**
     * Changes the monitoring for a <code>pipelineElement</code>.
     * 
     * @param pipeline the name of the pipeline (may be <b>null</b> for all)
     * @param pipelineElement the pipeline element as class name (may be <b>null</b> for all)
     * @param observable the observable to be changed (may be <b>null</b> for all)
     * @param enabled whether the observable shall be enabled or disabled
     * @param timestamp the coordination / enactment timestamp pointing to the respective coordination log entry
     */
    public ChangeMonitoringEvent(String pipeline, String pipelineElement, IObservable observable, 
        boolean enabled, long timestamp) {
        super(pipeline, pipelineElement, null);
        this.observable = observable;
        this.enabled = enabled;
        this.timestamp = timestamp;
    }

    /**
     * Returns the observable to be affected.
     * 
     * @return the observable to be affected (may be <b>null</b> for all)
     */
    public IObservable getObservable() {
        return observable;
    }

    /**
     * Returns whether this event enables or disables something..
     * 
     * @return <code>true</code> if enable, <code>false</code> if disable
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Returns whether algorithm tracing shall be enabled. This becomes effective
     * only for pipelines started after this event.
     * 
     * @return <code>true</code> if enabled, <code>false</code> if disabled, <b>null</b> if no change is needed
     */
    public Boolean enableAlgorithmTracing() {
        return enableAlgorithmTracing;
    }
    
}
