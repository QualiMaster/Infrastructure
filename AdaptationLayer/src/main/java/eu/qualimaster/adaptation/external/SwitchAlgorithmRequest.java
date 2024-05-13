package eu.qualimaster.adaptation.external;

/**
 * A message to cause switching an algorithm. This message can be sent without authentication, but leads then
 * to an adaptation of lower priority, potentially a rejection. This message can be elevated, but is then processed 
 * only if authenticated. Leads to an {@link ExecutionResponseMessage}.
 * 
 * @author Holger Eichelberger
 */
public class SwitchAlgorithmRequest extends RequestMessage {

    private static final long serialVersionUID = 7860837475798514225L;
    private String pipeline;
    private String pipelineElement;
    private String newAlgorithm;
    
    /**
     * Creates a switch algorithm message (explicit demo adaptation).
     * @param pipeline the name of the pipeline
     * @param pipelineElement the name of the processing element
     * @param newAlgorithm the new algorithm to use
     */
    public SwitchAlgorithmRequest(String pipeline, String pipelineElement, String newAlgorithm) {
        this.pipeline = pipeline;
        this.pipelineElement = pipelineElement;
        this.newAlgorithm = newAlgorithm;
    }
    
    @Override
    public void dispatch(IDispatcher dispatcher) {
        dispatcher.handleSwitchAlgorithmRequest(this);
    }

    /**
     * Returns the name of the pipeline.
     * 
     * @return the name of the pipeline
     */
    public String getPipeline() {
        return pipeline;
    }

    /**
     * Returns the processing element.
     * 
     * @return the processing element
     */
    public String getPipelineElement() {
        return pipelineElement;
    }

    /**
     * Returns the new algorithm to switch to.
     * 
     * @return the new algorithm
     */
    public String getNewAlgorithm() {
        return newAlgorithm;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Utils.hashCode(getNewAlgorithm()) + Utils.hashCode(getPipeline()) 
             + Utils.hashCode(getPipelineElement()) + Utils.hashCode(requiresAuthentication());
    }
    
    @Override
    public boolean equals(Object obj) {
        boolean equals = super.equals(obj);
        if (obj instanceof SwitchAlgorithmRequest) {
            SwitchAlgorithmRequest msg = (SwitchAlgorithmRequest) obj;
            equals = Utils.equals(getNewAlgorithm(), msg.getNewAlgorithm());
            equals &= Utils.equals(getPipeline(), msg.getPipeline());
            equals &= Utils.equals(getPipelineElement(), msg.getPipelineElement());
            equals &= requiresAuthentication() == msg.requiresAuthentication(); // avoid elevated equality
        }
        return equals;
    }

    /**
     * A privileged wrapper of the same type.
     * 
     * @author Holger Eichelberger
     */
    private static class ElevatedSwitchAlgorithmRequest extends SwitchAlgorithmRequest {
        
        private static final long serialVersionUID = -3169611893077357326L;

        /**
         * Creates an elevated switch algorithm request.
         * 
         * @param request the original (unelevated) request
         */
        private ElevatedSwitchAlgorithmRequest(SwitchAlgorithmRequest request) {
            super(request.getPipeline(), request.getPipelineElement(), request.getNewAlgorithm());
            setMessageId(request.getMessageId());
            setClientId(request.getClientId());
        }
        
        @Override
        public final boolean requiresAuthentication() {
            return true; // privileged messages require always an authenticated connection, no reduction possible
        }

        @Override
        public final boolean passToUnauthenticatedClient() {
            return false; // pass never
        }
        
        @Override
        public final Message elevate() {
            return this; // we are already elevated
        }
        
    }
    
    @Override
    public Message elevate() {
        return new ElevatedSwitchAlgorithmRequest(this);
    }
    
    @Override
    public Message toInformation() {
        return new InformationMessage(pipeline, pipelineElement, "switch to algorithm '" + newAlgorithm + "'", null);
    }

    @Override
    public String toString() {
        return "SwitchAlgorithmRequest " + pipeline + " " + pipelineElement + " " + newAlgorithm;
    }

}
