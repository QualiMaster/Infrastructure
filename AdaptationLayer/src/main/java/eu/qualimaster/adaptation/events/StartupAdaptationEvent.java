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
    private String mainPipeline;

    /**
     * Creates a startup adaptation event.
     * 
     * @param pipeline the pipeline
     */
    @QMInternal
    public StartupAdaptationEvent(String pipeline) {
        this(pipeline, null);
    }

    /**
     * Creates a startup adaptation event.
     * 
     * @param pipeline the pipeline
     * @param mainPipeline the main pipeline in case that <code>oipeline</code> is a sub-pipeline(null or empty 
     *     for none)
     */
    @QMInternal
    public StartupAdaptationEvent(String pipeline, String mainPipeline) {
        this.pipeline = pipeline;
        this.mainPipeline = mainPipeline;
    }

    /**
     * The name of the pipeline being affected.
     * 
     * @return the pipeline name
     */
    public String getPipeline() {
        return pipeline;
    }
    
    /**
     * Returns the name of the main pipeline.
     * 
     * @return the name of the main pipeline if this pipeline is a sub-pipeline, empty (!) if there is none
     */
    public String getMainPipeline() {
        return null == mainPipeline ? "" : mainPipeline;
    }

}
