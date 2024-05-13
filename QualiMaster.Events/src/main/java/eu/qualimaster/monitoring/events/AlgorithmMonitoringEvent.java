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
    private String pipeline;
    private ComponentKey componentKey;

    /**
     * Creates a new algorithm monitoring event.
     * 
     * @param pipeline the pipeline name
     * @param algorithmId the algorithm identifier (usually the class name)
     * @param observable the observable
     * @param value the observed value
     */
    public AlgorithmMonitoringEvent(String pipeline, String algorithmId, IObservable observable, double value) {
        this(pipeline, algorithmId, null, observable, value);
    }
    
    /**
     * Creates a new algorithm monitoring event.
     * 
     * @param pipeline the pipeline name
     * @param algorithmId the algorithm identifier (usually the class name)
     * @param key the component key if known (may be <b>null</b>)
     * @param observable the observable
     * @param value the observed value
     */
    public AlgorithmMonitoringEvent(String pipeline, String algorithmId, ComponentKey key, IObservable observable, 
        double value) {
        this.algorithmId = algorithmId;
        this.observable = observable;
        this.value = value;
        this.pipeline = pipeline;
        this.componentKey = key;
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
     * Returns the pipeline name.
     * 
     * @return the pipeline name
     */
    public String getPipeline() {
        return pipeline;
    }
    
    /**
     * Returns the component key (if known).
     * 
     * @return the component key (may be <b>null</b>)
     */
    public ComponentKey getComponentKey() {
        return componentKey;
    }

}
