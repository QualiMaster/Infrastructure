package eu.qualimaster.coordination.commands;


/**
 * An abstract command addressing a pipeline.
 * Please note that abstract names are described here while the CoordinationLayer must translate
 * them to actual Execution System commands. 
 * 
 * @author Holger Eichelberger
 */
public abstract class AbstractPipelineCommand extends CoordinationCommand {
    
    private static final long serialVersionUID = 57688424041835641L;
    private String pipeline;

    /**
     * Creates a pipeline command.
     * 
     * @param pipeline the name of the pipeline
     */
    protected AbstractPipelineCommand(String pipeline) {
        this.pipeline = pipeline;
    }

    /**
     * Returns the addressed pipeline.
     * 
     * @return the pipeline name
     */
    public String getPipeline() {
        return pipeline;
    }

}
