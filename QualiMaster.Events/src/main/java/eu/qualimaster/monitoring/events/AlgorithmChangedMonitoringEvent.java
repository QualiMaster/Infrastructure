package eu.qualimaster.monitoring.events;

import eu.qualimaster.common.QMInternal;

/**
 * Informs monitoring about a successful change of an algorithm in the pipeline realization.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public class AlgorithmChangedMonitoringEvent extends AbstractPipelineElementEnactmentCompletedMonitoringEvent {

    private static final long serialVersionUID = -6801990405481431021L;
    private String algorithm;

    /**
     * Creates a new algorithm changed monitoring event (without causing message id).
     * 
     * @param pipeline the pipeline name
     * @param pipelineElement the pipeline element name
     * @param algorithm the new algorithm
     */
    public AlgorithmChangedMonitoringEvent(String pipeline, String pipelineElement, String algorithm) {
        this(pipeline, pipelineElement, algorithm, null);
    }

    /**
     * Creates a new algorithm changed monitoring event.
     * 
     * @param pipeline the pipeline name
     * @param pipelineElement the pipeline element name
     * @param algorithm the new algorithm
     * @param causeMsgId the causing message id
     */
    public AlgorithmChangedMonitoringEvent(String pipeline, String pipelineElement, String algorithm, 
        String causeMsgId) {
        super(pipeline, pipelineElement, null, causeMsgId);
        this.algorithm = algorithm;
    }
    
    /**
     * Returns the new algorithm.
     * 
     * @return the new algorithm name
     */
    public String getAlgorithm() {
        return algorithm;
    }

}
