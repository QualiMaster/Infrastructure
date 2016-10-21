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
    private boolean stopped;

    /**
     * Creates a startup adaptation event.
     * 
     * @param pipeline the pipeline
     * @param stopped whether the pipeline finally stopped or is in stopping
     */
    @QMInternal
    public ShutdownAdaptationEvent(String pipeline, boolean stopped) {
        this.pipeline = pipeline;
        this.stopped = stopped;
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
     * Returns whether the pipeline finally stopped.
     * 
     * @return <code>true</code> for stopped, <code>false</code> for stopping
     */
    public boolean isStopped() {
        return stopped;
    }

}
