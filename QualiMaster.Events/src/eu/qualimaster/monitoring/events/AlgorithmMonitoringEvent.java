package eu.qualimaster.monitoring.events;

import eu.qualimaster.common.QMInternal;
import eu.qualimaster.observables.IObservable;

/**
 * A monitoring event dedicated to a single algorithm.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public class AlgorithmMonitoringEvent extends MonitoringEvent {

    private static final long serialVersionUID = 245805272875086837L;
    private String algorithmId;
    private IObservable observable;
    private double value;
    private String topologyId;

    /**
     * Creates a new algorithm monitoring event.
     * 
     * @param topologyId the (optional) topology id
     * @param algorithmId the algorithm identifier (usually the class name)
     * @param observable the observable
     * @param value the observed value
     */
    public AlgorithmMonitoringEvent(String topologyId, String algorithmId, IObservable observable, double value) {
        this.algorithmId = algorithmId;
        this.observable = observable;
        this.value = value;
        this.topologyId = quoteNull(topologyId);
    }
    
    /**
     * Returns the algorithm id.
     * 
     * @return the algorithm id
     */
    public String getAlgorithmId() {
        return algorithmId;
    }
    
    /**
     * Returns the observable.
     * 
     * @return the observable
     */
    public IObservable getObservable()  {
        return observable;
    }
    
    /**
     * Returns the observed value.
     * 
     * @return the observed value
     */
    public double getValue() {
        return value;
    }
    
    /**
     * Returns the topology id.
     * 
     * @return the topology id
     */
    public String getTopologyId() {
        return unquoteNull(topologyId);
    }

}
