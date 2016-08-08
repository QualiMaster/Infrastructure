package eu.qualimaster.adaptation.events;

import eu.qualimaster.common.QMInternal;

/**
 * Notifies the adaptation about shutting down a pipeline.
 * 
 * @author Holger Eichelberger
 */
public class ShutdownAdaptationEvent extends AdaptationEvent {

    private static final long serialVersionUID = 7508306583054608337L;
    private String pipeline;

    /**
     * Creates a startup adaptation event.
     * 
     * @param pipeline the pipeline
     */
    @QMInternal
    public ShutdownAdaptationEvent(String pipeline) {
        this.pipeline = pipeline;
    }
    
    /**
     * The name of the pipeline being affected.
     * 
     * @return the pipeline name
     */
    public String getPipeline() {
        return pipeline;
    }

}
