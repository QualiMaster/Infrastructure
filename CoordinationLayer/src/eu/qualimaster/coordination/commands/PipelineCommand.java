package eu.qualimaster.coordination.commands;

import eu.qualimaster.common.QMInternal;
import eu.qualimaster.infrastructure.PipelineOptions;

/**
 * Defines a pipeline command, i.e., a command on an entire pipeline.
 * Please note that abstract names are described here while the CoordinationLayer must translate
 * them to actual Execution System commands.
 * 
 * @author Holger Eichelberger
 */
public class PipelineCommand extends AbstractPipelineCommand {

    private static final long serialVersionUID = -2324993487797063739L;

    /**
     * The desired pipeline status after execution.
     * 
     * @author Holger Eichelberger
     */
    public enum Status {
        
        /**
         * Start the pipeline, i.e. after executing the command it is started.
         * This may include deployment.
         */
        START,
        
        /**
         * Connect the pipeline.
         */
        CONNECT,

        /**
         * Disconnect the pipeline.
         */
        DISCONNECT,

        /**
         * Stop the pipeline, i.e. after executing the command it is started.
         */
        STOP;
    }
    
    private Status status;
    private PipelineOptions options;

    /**
     * Creates a pipeline command with no options.
     *  
     * @param pipeline the pipeline (name) to address
     * @param status the intended status
     */
    public PipelineCommand(String pipeline, Status status) {
        this(pipeline, status, null);
    }
    
    /**
     * Creates a pipeline command.
     *  
     * @param pipeline the pipeline (name) to address
     * @param status the intended status
     * @param options to be considered while starting up a pipeline, intended for experiments so that
     *   arguments and parallelization can be changed without recompiling a pipeline
     */
    @QMInternal
    public PipelineCommand(String pipeline, Status status, PipelineOptions options) {
        super(pipeline);
        this.status = status;
        this.options = null == options ? new PipelineOptions() : options;
    }
    
    /**
     * Returns the intended status.
     * 
     * @return the intended status
     */
    public Status getStatus() {
        return status;
    }

    @QMInternal
    @Override
    public CoordinationExecutionResult accept(ICoordinationCommandVisitor visitor) {
        return visitor.visitPipelineCommand(this);
    }
    
    /**
     * Returns the pipeline options.
     * 
     * @return the pipeline options, always an object
     */
    public PipelineOptions getOptions() {
        return options;
    }

}
