package eu.qualimaster.monitoring.events;

import eu.qualimaster.common.QMInternal;
import eu.qualimaster.observables.IObservable;

/**
 * Represents a pipeline elemenent monitoring event issued by the generated pipeline implementation.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public class PipelineElementObservationMonitoringEvent extends AbstractPipelineElementMonitoringEvent {

    private static final long serialVersionUID = 1647581745427624957L;
    private IObservable observable;
    private Double observation;

    /**
     * Creates a pipeline element monitoring event.
     * 
     * @param pipeline the pipeline name
     * @param pipelineElement the pipeline element name
     * @param key the aggregation component key
     * @param observable the observable
     * @param observation the observation made
     */
    public PipelineElementObservationMonitoringEvent(String pipeline, String pipelineElement, 
        ComponentKey key, IObservable observable, int observation) {
        this(pipeline, pipelineElement, key, observable, Double.valueOf(observation));
    }
    
    /**
     * Creates a pipeline element monitoring event.
     * 
     * @param pipeline the pipeline name
     * @param pipelineElement the pipeline element name
     * @param key the aggregation component key
     * @param observable the observable
     * @param observation the observation made
     */
    public PipelineElementObservationMonitoringEvent(String pipeline, String pipelineElement, ComponentKey key, 
        IObservable observable, Double observation) {
        super(pipeline, pipelineElement, key);
        this.observable = observable;
        this.observation = observation;
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

}
