package eu.qualimaster.adaptation.events;

/**
 * Requests a specific parameter change at runtime.
 * 
 * @author Holger Eichelberger
 */
public class AlgorithmConfigurationAdaptationEvent extends AdaptationEvent implements IPipelineAdaptationEvent {

    private static final long serialVersionUID = -4280101358561806246L;
    private String pipeline;
    private String pipelineElement;
    private String algorithm;
    private boolean userTrigger;
    
    /**
     * Creates a new algorithm configuration event.
     * 
     * @param pipeline the pipeline to enact on
     * @param pipelineElement the pipeline element to enact on
     * @param algorithm the algorithm to enact
     * @param userTrigger whether this event was created due to an user trigger (<code>true</code>) or internal by the 
     * infrastructure
     */
    public AlgorithmConfigurationAdaptationEvent(String pipeline, String pipelineElement, String algorithm, 
        boolean userTrigger) {
        this.pipeline = pipeline;
        this.pipelineElement = pipelineElement;
        this.algorithm = algorithm;
        this.userTrigger = userTrigger;
    }

    @Override
    public String getPipeline() {
        return pipeline;
    }
    
    /**
     * Returns the pipeline element to enact on.
     * 
     * @return the pipeline element
     */
    public String getPipelineElement() {
        return pipelineElement;
    }
    
    /**
     * Returns the algorithm to be enacted.
     * 
     * @return the algorithm
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * Returns whether this event has been created by the infrastructure or due to an external adaptation event 
     * (user trigger).
     * 
     * @return <code>true</code> if this event was created due to an user trigger, <code>false</code> if this was 
     * created internally by the infrastructure
     * 
     * <code>true</code> for internal by the infrastructure, <code>true</code> due to an external event
     */
    public boolean isUserTrigger() {
        return userTrigger;
    }

}
