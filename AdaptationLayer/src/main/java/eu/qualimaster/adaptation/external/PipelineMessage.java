package eu.qualimaster.adaptation.external;

/**
 * Implements a pipeline message, indicating a change to a pipeline. This message requires an authenticated client
 * connection. Leads to a {@link ExecutionResponseMessage}.
 * 
 * @author Holger Eichelberger
 */
public class PipelineMessage extends PrivilegedMessage {

    /**
     * Defines the target status.
     * 
     * @author Holger Eichelberger
     */
    public enum Status {
        START,
        STOP;
    }
    
    private static final long serialVersionUID = 6699640225130649321L;
    private String pipeline;
    private Status status;

    /**
     * Creates a pipeline message.
     * 
     * @param pipeline the pipeline name
     * @param status the target status
     */
    public PipelineMessage(String pipeline, Status status) {
        this.pipeline = pipeline;
        this.status = status;
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
     * Returns the intended status.
     * 
     * @return the status
     */
    public Status getStatus() {
        return status;
    }

    @Override
    public void dispatch(IDispatcher dispatcher) {
        dispatcher.handlePipelineMessage(this);
    }

    @Override
    public int hashCode() {
        return Utils.hashCode(getPipeline()) + Utils.hashCode(getStatus());
    }
    
    @Override
    public boolean equals(Object obj) {
        boolean equals = false;
        if (obj instanceof PipelineMessage) {
            PipelineMessage msg = (PipelineMessage) obj;
            equals = Utils.equals(getPipeline(), msg.getPipeline());
            equals &= Utils.equals(getStatus(), msg.getStatus());
        }
        return equals;
    }

    @Override
    public Message toInformation() {
        return new InformationMessage(pipeline, null, 
            status == Status.START ? "start pipeline" : "stop pipeline", null);
    }

    @Override
    public String toString() {
        return "PipelineMessage " + pipeline + " " + status;
    }

}
