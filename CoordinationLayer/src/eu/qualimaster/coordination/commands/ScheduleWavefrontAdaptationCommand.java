package eu.qualimaster.coordination.commands;

import eu.qualimaster.common.QMInternal;


/**
 * A command to schedule adaptation on an algorithm family in order to cause a wavefront adaptation.
 * Please note that abstract names are described here while the CoordinationLayer must translate
 * them to actual Execution System commands. 
 * 
 * @author Holger Eichelberger
 */
public class ScheduleWavefrontAdaptationCommand extends AbstractPipelineElementCommand {
    
    private static final long serialVersionUID = -581806295711137229L;

    /**
     * Creates a wavefront adaptation on the specified pipeline element.
     * 
     * @param pipeline the name of the pipeline
     * @param pipelineElement the name of the pipeline element
     */
    public ScheduleWavefrontAdaptationCommand(String pipeline, String pipelineElement) {
        super(pipeline, pipelineElement);
    }

    @QMInternal
    @Override
    public CoordinationExecutionResult accept(ICoordinationCommandVisitor visitor) {
        return visitor.visitScheduleWavefrontAdaptationCommand(this);
    }

}
