package eu.qualimaster.monitoring.events;

import eu.qualimaster.common.QMInternal;

/**
 * A monitoring event dedicated to a pipeline.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public class AbstractPipelineMonitoringEvent extends MonitoringEvent {

    private static final long serialVersionUID = -4568996824357199616L;
    private String pipeline;

    /**
     * Creates an abstract pipeline monitoring event.
     * 
     * @param pipeline pipeline the pipeline name
     */
    protected AbstractPipelineMonitoringEvent(String pipeline) {
        this.pipeline = pipeline;
    }
    
    /**
     * Returns the pipeline to be affected.
     * 
     * @return the pipeline to be affected (may be <b>null</b> for all)
     */
    public String getPipeline() {
        return pipeline;
    }

}
