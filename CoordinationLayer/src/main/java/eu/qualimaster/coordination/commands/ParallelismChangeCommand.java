package eu.qualimaster.coordination.commands;

import java.util.Map;

import eu.qualimaster.common.QMGenerics;
import eu.qualimaster.common.QMInternal;
import eu.qualimaster.coordination.ParallelismChangeRequest;

/**
 * Causes a change of parallelism in the execution.
 * 
 * @author Holger Eichelberger
 */
public class ParallelismChangeCommand extends AbstractPipelineCommand {

    private static final long serialVersionUID = 6246383503984740375L;
    private int numberOfWorkers;
    private Map<String, Integer> executors;
    private Map<String, ParallelismChangeRequest> changes;
    
    /**
     * Changes the parallelism of a single pipeline.
     * 
     * @param pipeline the name of the pipeline
     * @param numberOfWorkers the desired number of workers
     * @param executors the pipeline node name - executors mapping to enact
     */
    public ParallelismChangeCommand(String pipeline, int numberOfWorkers, 
        @QMGenerics(types = {String.class, Integer.class }) Map<String, Integer> executors) {
        super(pipeline);
        this.numberOfWorkers = numberOfWorkers;
        this.executors = executors;
    }
    
    /**
     * Changes the parallelism of a single pipeline in incremental fashion. Requires the extended QM Storm version.
     * 
     * @param pipeline the name of the pipeline
     * @param changes the pipeline node name - changes mapping to enact
     */
    public ParallelismChangeCommand(String pipeline, 
        @QMGenerics(types = {String.class, ParallelismChangeRequest.class }) 
        Map<String, ParallelismChangeRequest> changes) {
        super(pipeline);
        this.changes = changes;
    }

    @QMInternal
    @Override
    public CoordinationExecutionResult accept(ICoordinationCommandVisitor visitor) {
        return visitor.visitParallelismChangeCommand(this);
    }

    /**
     * Returns the desired number of workers.
     * 
     * @return the desired number of workers
     */
    public int getNumberOfWorkers() {
        return numberOfWorkers;
    }

    /**
     * Returns the desired changes on the executors.
     * 
     * @return the desired changes on the executors (may be <b>null</b> if {@link #getIncrementalChanges()} provides
     *   the changes)
     */
    @QMGenerics(types = {String.class, Integer.class })
    public Map<String, Integer> getExecutors() {
        return executors;
    }

    /**
     * Returns the desired.
     * 
     * @return the desired changes (may be <b>null</b> if {@link #getExecutor()} provides the changes)
     */
    @QMGenerics(types = {String.class, ParallelismChangeRequest.class })
    public Map<String, ParallelismChangeRequest> getIncrementalChanges() {
        return changes;
    }

}
