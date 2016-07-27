package eu.qualimaster.adaptation.events;

import eu.qualimaster.common.QMInternal;

/**
 * Notifies the adaptation about starting a pipeline.
 * 
 * @author Holger Eichelberger
 */
public class StartupAdaptationEvent extends AdaptationEvent {

    private static final long serialVersionUID = -8521954805657613415L;
    private String pipeline;

    /**
     * Creates a startup adaptation event.
     * 
     * @param pipeline the pipeline
     */
    @QMInternal
    public StartupAdaptationEvent(String pipeline) {
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
