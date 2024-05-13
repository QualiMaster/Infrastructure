package eu.qualimaster.coordination.commands;

/**
 * An abstract command addressing a pipeline element in a pipeline.
 * Please note that abstract names are described here while the CoordinationLayer must translate
 * them to actual Execution System commands. 
 * 
 * @author Holger Eichelberger
 */
public abstract class AbstractPipelineElementCommand extends AbstractPipelineCommand {

    private static final long serialVersionUID = -8058420253330316131L;
    private String pipelineElement;

    /**
     * Creates a pipeline element command.
     * 
     * @param pipeline the name of the pipeline
     * @param pipelineElement the name of the element
     */
    public AbstractPipelineElementCommand(String pipeline, String pipelineElement) {
        super(pipeline);
        this.pipelineElement = pipelineElement;
    }
    
    /**
     * Returns the addressed pipeline element.
     * 
     * @return the name of the pipeline element
     */
    public String getPipelineElement() {
        return pipelineElement;
    }

}
