package eu.qualimaster.monitoring.events;

import java.util.Map;

import eu.qualimaster.common.QMInternal;
import eu.qualimaster.observables.IObservable;

/**
 * Represents a pipeline element monitoring event with multiple observations issued by the generated pipeline 
 * implementation.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public class PipelineElementMultiObservationMonitoringEvent extends AbstractPipelineElementMonitoringEvent {

    private static final long serialVersionUID = -1113011834383526806L;
    private Map<IObservable, Double> observations;
    
    /**
     * Creates a pipeline element monitoring event.
     * 
     * @param pipeline the pipeline name
     * @param pipelineElement the pipeline element name
     * @param key the aggregation component key
     * @param observations the observations made
     */
    public PipelineElementMultiObservationMonitoringEvent(String pipeline, String pipelineElement, ComponentKey key, 
        Map<IObservable, Double> observations) {
        super(pipeline, pipelineElement, key);
        this.observations = observations;
    }

    /**
     * Returns all observations.
     * 
     * @return the observations
     */
    public Map<IObservable, Double> getObservations() {
        return observations;
    }

}
