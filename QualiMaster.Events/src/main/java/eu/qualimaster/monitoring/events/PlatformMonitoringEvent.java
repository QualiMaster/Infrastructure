package eu.qualimaster.monitoring.events;

import eu.qualimaster.common.QMInternal;
import eu.qualimaster.observables.IObservable;

/**
 * Event notifying an observation on platform level.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public class PlatformMonitoringEvent extends MonitoringEvent {
    
    private static final long serialVersionUID = -1867122651037833787L;
    private IObservable observable;
    private Double observation;
    private String key;
    private String topologyId;

    /**
     * Creates a platform monitoring event.
     * 
     * @param observable the observable
     * @param observation the observation
     * @param key the aggregation key for composite aggregations (may be <b>null</b> if not required, 
     *   depends on the <code>observable</code>)
     */
    public PlatformMonitoringEvent(IObservable observable, int observation, String key) {
        this(observable, (double) observation, key);
    }
    
    /**
     * Creates a platform monitoring event.
     * 
     * @param observable the observable
     * @param observation the observation
     * @param key the aggregation key for composite aggregations (may be <b>null</b> if not required, 
     *   depends on the <code>observable</code>)
     */
    public PlatformMonitoringEvent(IObservable observable, Double observation, String key) {
        this(null, observable, observation, key);
    }

    /**
     * Creates a platform monitoring event.
     * 
     * @param topologyId the (optional) topology id
     * @param observable the observable
     * @param observation the observation
     * @param key the aggregation key for composite aggregations (may be <b>null</b> if not required, 
     *   depends on the <code>observable</code>)
     */
    public PlatformMonitoringEvent(String topologyId, IObservable observable, Double observation, String key) {
        this.observable = observable;
        this.observation = observation;
        this.key = quoteNull(key);
        this.topologyId = quoteNull(topologyId);
    }
    
    /**
     * Returns the observable.
     * 
     * @return the observable
     */
    public IObservable getObservable() {
        return observable;
    }

    /**
     * Returns the observation.
     * 
     * @return the observation
     */
    public Double getObservation() {
        return observation;
    }
    
    /**
     * Returns the topology id.
     * 
     * @return the topology id
     */
    public String getTopologyId() {
        return unquoteNull(topologyId);
    }

    /**
     * Returns the aggregation key.
     * 
     * @return the aggregation key for composite aggregations (may be <b>null</b> if not required, 
     *   depends on the <code>observable</code>)
     */
    public String getKey() {
        return unquoteNull(key);
    }

}
