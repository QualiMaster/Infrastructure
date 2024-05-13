package eu.qualimaster.adaptation.external;

/**
 * Represents an algorithm change, i.e., the change was done in the implemented pipeline.
 * 
 * @author Holger Eichelberger
 */
public class AlgorithmChangedMessage extends UsualMessage {

    private static final long serialVersionUID = -7818179438438277176L;
    private String pipeline;
    private String pipelineElement;
    private String algorithm;
    
    /**
     * Creates an algorithm changed message.
     * 
     * @param pipeline the pipeline name
     * @param pipelineElement the pipeline element name
     * @param algorithm the algorithm name (this was the result of the change)
     */
    public AlgorithmChangedMessage(String pipeline, String pipelineElement, String algorithm) {
        this.pipeline = pipeline;
        this.pipelineElement = pipelineElement;
        this.algorithm = algorithm;
    }
    
    @Override
    public void dispatch(IDispatcher dispatcher) {
        dispatcher.handleAlgorithmChangedMessage(this);
    }

    /**
     * Returns the pipeline name.
     * 
     * @return the pipeline name
     */
    public String getPipeline() {
        return pipeline;
    }

    /**
     * Returns the affected pipeline element.
     * 
     * @return the pipeline element name
     */
    public String getPipelineElement() {
        return pipelineElement;
    }

    /**
     * Returns the new algorithm.
     * 
     * @return the algorithm name
     */
    public String getAlgorithm() {
        return algorithm;
    }

    @Override
    public int hashCode() {
        return Utils.hashCode(getAlgorithm()) + Utils.hashCode(getPipeline()) + Utils.hashCode(getPipelineElement());
    }
    
    @Override
    public boolean equals(Object obj) {
        boolean equals = false;
        if (obj instanceof AlgorithmChangedMessage) {
            AlgorithmChangedMessage msg = (AlgorithmChangedMessage) obj;
            equals = Utils.equals(getAlgorithm(), msg.getAlgorithm());
            equals &= Utils.equals(getPipeline(), msg.getPipeline());
            equals &= Utils.equals(getPipelineElement(), msg.getPipelineElement());
        }
        return equals;
    }

    @Override
    public Message toInformation() {
        return new InformationMessage(pipeline, pipelineElement, "switched to '" + algorithm + "'", null);
    }

    @Override
    public String toString() {
        return "AlgorithmChangeMessage " + pipeline + " " + pipelineElement + " " + algorithm;
    }
    
}
