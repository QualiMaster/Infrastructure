package eu.qualimaster.monitoring.events;

import java.util.Map;

import eu.qualimaster.common.QMInternal;
import eu.qualimaster.events.IResponseEvent;
import eu.qualimaster.events.IReturnableEvent;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.MonitoringFrequency;

/**
 * Causes a change in monitoring an observable.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public class ChangeMonitoringEvent extends AbstractPipelineElementMonitoringEvent implements IResponseEvent {

    private static final long serialVersionUID = 3321516452071765608L;
    private Map<MonitoringFrequency, Integer> frequencies;
    private Map<IObservable, Boolean> observables;
    private Boolean enableAlgorithmTracing; 
    private String receiverId;
    private String msgId;

    /**
     * Creates a neutral monitoring event potentially affecting algorithm tracing, i.e., no receiverId/msgId and 
     * continued enabled monitoring. 
     * 
     * @param enableAlgorithmTracing enables or disables algorithm tracing to files for obtaining 
     *     (initial/experimental/testing) profiles
     */
    public ChangeMonitoringEvent(boolean enableAlgorithmTracing) {
        this(null, null);
        this.enableAlgorithmTracing = enableAlgorithmTracing;
    }
    
    /**
     * Changes the monitoring for the entire infrastructure.
     * 
     * @param frequencies the desired monitoring frequency, <b>null</b> for unspecified, 0 or negative for 
     *     completely disabled
     * @param cause optional request message with sender and message id (may be <b>null</b>)
     */
    public ChangeMonitoringEvent(Map<MonitoringFrequency, Integer> frequencies, IReturnableEvent cause) {
        this(null, null, frequencies, null, cause);
    }

    /**
     * Changes the monitoring of a resource for the entire infrastructure.
     * 
     * @param frequencies the desired monitoring frequencies, <b>null</b> for unspecified, 0 or negative for 
     *     completely disabled
     * @param observables the enabled/disabled observables, <b>null</b> for unspecified
     * @param cause optional request message with sender and message id (may be <b>null</b>)
     */
    public ChangeMonitoringEvent(Map<MonitoringFrequency, Integer> frequencies, Map<IObservable, Boolean> observables, 
        IReturnableEvent cause) {
        this(null, null, frequencies, observables, cause);
    }

    /**
     * Changes the monitoring for a <code>pipeline</code>.
     * 
     * @param pipeline the name of the pipeline (may be <b>null</b> for all)
     * @param frequencies the desired monitoring frequencies, <b>null</b> for unspecified, 0 or negative for 
     *     completely disabled
     * @param observables the enabled/disabled observables, <b>null</b> for unspecified
     * @param cause optional request message with sender and message id (may be <b>null</b>)
     */
    public ChangeMonitoringEvent(String pipeline, Map<MonitoringFrequency, Integer> frequencies, 
        Map<IObservable, Boolean> observables, IReturnableEvent cause) {
        this(pipeline, null, frequencies, observables, cause);
    }
    
    /**
     * Changes the monitoring for a <code>pipelineElement</code>.
     * 
     * @param pipeline the name of the pipeline (may be <b>null</b> for all)
     * @param pipelineElement the pipeline element as class name (may be <b>null</b> for all)
     * @param frequencies the desired monitoring frequency, <b>null</b> for unspecified, 0 or negative for 
     *     completely disabled
     * @param observables the enabled/disabled observables, <b>null</b> for unspecified
     * @param cause optional request message with sender and message id (may be <b>null</b>)
     */
    public ChangeMonitoringEvent(String pipeline, String pipelineElement, 
        Map<MonitoringFrequency, Integer> frequencies, Map<IObservable, Boolean> observables, IReturnableEvent cause) {
        super(pipeline, pipelineElement, null);
        this.frequencies = frequencies;
        this.observables = observables;
        if (null != cause) {
            this.receiverId = cause.getSenderId();
            this.msgId = cause.getMessageId();
        }
    }

    /**
     * Returns the desired monitoring frequencies.
     * 
     * @return the desired monitoring frequencies, <b>null</b> for unspecified, 0 or negative for completely disabled
     */
    public Map<MonitoringFrequency, Integer> getFrequencies() {
        return frequencies;
    }
    
    /**
     * Returns the desired frequency for a certain frequency kind.
     * 
     * @param frequency the frequency kind
     * @return the desired monitoring frequency, <b>null</b> for unspecified, 0 or negative for completely disabled
     */
    public Integer getFrequency(MonitoringFrequency frequency) {
        return null == frequencies || null == frequency ? null : frequencies.get(frequency);
    }
    
    /**
     * Returns the enabled/disabled observables.
     * 
     * @return the enabled/disabled observables, <b>null</b> for unspecified
     */
    public Map<IObservable, Boolean> getObservables() {
        return observables;
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

    @Override
    public String getReceiverId() {
        return receiverId;
    }

    @Override
    public String getMessageId() {
        return msgId;
    }
    
}
