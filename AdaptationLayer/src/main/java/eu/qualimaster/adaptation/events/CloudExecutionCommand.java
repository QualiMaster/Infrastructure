package eu.qualimaster.adaptation.events;

import eu.qualimaster.adaptation.AdaptationManager;
import eu.qualimaster.adaptation.external.CloudPipelineMessage;
import eu.qualimaster.adaptation.external.PipelineMessage.Status;
import eu.qualimaster.common.QMName;
import eu.qualimaster.common.QMNoSimulation;
import eu.qualimaster.common.QMSupport;
import eu.qualimaster.coordination.commands.CoordinationCommand;
import eu.qualimaster.coordination.commands.CoordinationExecutionResult;
import eu.qualimaster.coordination.commands.ICoordinationCommandVisitor;

/**
 * Implements a pseudo command for the cloud execution.
 * 
 * @author Bendix Harries
 *
 */
@QMSupport
public class CloudExecutionCommand extends CoordinationCommand {

    private static final long serialVersionUID = 4263610900539025073L;
    private String cloudEnvironment;
    private String pipeline;
    private Status status;
    private int numVm;

    /**
     * Creates a cloud execution command.
     * 
     * @param cloudEnvironment
     *            the cloudenvironment
     * @param pipeline
     *            the pipeline
     * @param start the status
     */
    public CloudExecutionCommand(String cloudEnvironment, String pipeline, boolean start) {
        this.cloudEnvironment = cloudEnvironment;
        this.pipeline = pipeline;
        if (start) {
            status = Status.START;
        } else if (!start) {
            status = Status.STOP;
        }
    }
    /**
     * Creates a cloud execution command including the number of VMs.
     * @param cloudEnvironment the cloudenvironment
     * @param pipeline the pipeline
     * @param start the status
     * @param numVm the number of VMs
     */
    public CloudExecutionCommand(String cloudEnvironment, String pipeline, boolean start, int numVm) {
        this.cloudEnvironment = cloudEnvironment;
        this.pipeline = pipeline;
        if (start) {
            status = Status.START;
        } else if (!start) {
            status = Status.STOP;
        }
        this.numVm = numVm;
    }
    /**
     * This method sends the CloudExecutionCommand as a message to the endpoint.
     * Please note that this command is basically not a typical command but some
     * kind of an event
     */
    @QMName(name = "exec") // rename virtually for VIL
    @QMNoSimulation() // do not really execute this method during a simulation /
                      // test
    public void execute() {
        AdaptationManager.send(new CloudPipelineMessage(pipeline, status, cloudEnvironment));
    }

    /**
     * Returns the cloud environment.
     * 
     * @return the cloud environment.
     */
    public String getCloudEnvironment() {
        return cloudEnvironment;
    }

    /**
     * Returns the pipeline.
     * 
     * @return the pipeline
     */
    public String getPipeline() {
        return pipeline;
    }
    
    /**
     * Returns the status.
     * @return the status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Returns the number of VMs to be created.
     * @return the number of VMs
     */
    public int getNumVm() {
        return numVm;
    }

    @Override
    public CoordinationExecutionResult accept(ICoordinationCommandVisitor visitor) {
        return visitor.visitCloudExecutionCommand(this);
    }

}
