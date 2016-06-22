package eu.qualimaster.monitoring.events;

import java.io.Serializable;

import eu.qualimaster.common.QMInternal;

/**
 * A monitoring event for a pipeline element.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public class AbstractPipelineElementMonitoringEvent extends AbstractPipelineMonitoringEvent {

    private static final long serialVersionUID = -3205402550485551062L;
    private String pipelineElement;
    private Serializable key;

    /**
     * Creates an abstract pipeline monitoring event.
     * 
     * @param pipeline pipeline the pipeline name
     * @param pipelineElement pipeline element the name of the pipeline element
     * @param key the aggregation component key (may be <b>null</b>)
     */
    protected AbstractPipelineElementMonitoringEvent(String pipeline, String pipelineElement, Serializable key) {
        super(pipeline);
        this.pipelineElement = pipelineElement;
        this.key = key;
    }
    
    /**
     * Returns the pipeline element to be affected.
     * 
     * @return the pipeline element to be affected (may be <b>null</b> for all)
     */
    public String getPipelineElement() {
        return pipelineElement;
    }
    
    /**
     * The aggregation component key.
     * 
     * @return the component key
     */
    public Serializable getKey() {
        return key;
    }

}
