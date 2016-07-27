package eu.qualimaster.adaptation.external;

/**
 * Implementing a PipelineMessage for the cloud extending the PipelineMessage.
 * 
 * @author Bendix Harries
 *
 */
public class CloudPipelineMessage extends PipelineMessage {

   
    private static final long serialVersionUID = 8894329972793666151L;
    private String cloudEnvironment;

    /**
     * Constructor for a CloudPipelineMessage.
     * 
     * @param pipeline
     *            the name of the pipeline
     * @param status
     *            the status
     * @param cloudEnvironment
     *            the name of the cloudEnvironment
     */
    public CloudPipelineMessage(String pipeline, Status status, String cloudEnvironment) {
        super(pipeline, status);
        this.cloudEnvironment = cloudEnvironment;
    }
    /**
     * Returns the name of the cloudEnvironment.
     * @return the cloudEnvironment
     */
    public String getCloudEnvironment() {
        return cloudEnvironment;
    }

    @Override
    public void dispatch(IDispatcher dispatcher) {
        dispatcher.handleCloudPipelineMessage(this);
    }

    @Override
    public String toString() {
        return "CloudPipelineMessage " + super.getPipeline() + " " + getStatus() + " cloudEnvironemnt: "
                + cloudEnvironment;
    }

}
