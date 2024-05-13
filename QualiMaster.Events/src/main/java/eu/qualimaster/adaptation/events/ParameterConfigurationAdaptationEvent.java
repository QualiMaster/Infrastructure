package eu.qualimaster.adaptation.events;

import java.io.Serializable;

import eu.qualimaster.common.QMInternal;

/**
 * Requests a specific parameter change at runtime.
 * 
 * @author Holger Eichelberger
 */
public class ParameterConfigurationAdaptationEvent extends AdaptationEvent implements IPipelineAdaptationEvent {

    private static final long serialVersionUID = -466693025802063319L;
    private String pipeline;
    private String pipelineElement;
    private String parameter;
    private Serializable value;
    private boolean userTrigger;
    
    /**
     * Creates a new algorithm configuration event.
     * 
     * @param pipeline the pipeline to enact on
     * @param pipelineElement the pipeline element to enact on
     * @param parameter the parameter to change
     * @param value the new value
     * @param userTrigger whether this event was created due to an user trigger (<code>true</code>) or internal by the 
     * infrastructure
     */
    @QMInternal
    public ParameterConfigurationAdaptationEvent(String pipeline, String pipelineElement, String parameter, 
        Serializable value, boolean userTrigger) {
        this.pipeline = pipeline;
        this.pipelineElement = pipelineElement;
        this.parameter = parameter;
        this.value = value;
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
    public String getParameter() {
        return parameter;
    }
    
    /**
     * Return the value.
     * 
     * @return the value
     */
    public Serializable getValue() {
        return value;
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
