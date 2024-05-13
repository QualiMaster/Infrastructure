package eu.qualimaster.coordination.commands;

import java.util.Map;

import eu.qualimaster.common.QMInternal;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.MonitoringFrequency;

/**
 * Causes a change of monitoring.
 * 
 * @author Holger Eichelberger
 */
public class MonitoringChangeCommand extends AbstractPipelineElementCommand {

    private static final long serialVersionUID = 7305954829313124837L;
    private Map<MonitoringFrequency, Integer> frequencies;
    private Map<IObservable, Boolean> observables;
    
    /**
     * Changes the monitoring for the entire infrastructure.
     * 
     * @param frequencies the desired monitoring frequencies, <b>null</b> for unspecified, 0 or negative for 
     *     completely disabled
     */
    public MonitoringChangeCommand(Map<MonitoringFrequency, Integer> frequencies) {
        this(null, null, frequencies, null);
    }

    /**
     * Changes the monitoring of a resource for the entire infrastructure.
     * 
     * @param frequencies the desired monitoring frequencies, <b>null</b> for unspecified, 0 or negative for 
     *     completely disabled
     * @param observables the enabled/disabled observables, <b>null</b> for unspecified
     */
    public MonitoringChangeCommand(Map<MonitoringFrequency, Integer> frequencies, 
        Map<IObservable, Boolean> observables) {
        this(null, null, frequencies, observables);
    }

    /**
     * Changes the monitoring for a <code>pipeline</code>.
     * 
     * @param pipeline the name of the pipeline (may be <b>null</b> for all)
     * @param frequencies the desired monitoring frequencies, <b>null</b> for unspecified, 0 or negative for 
     *     completely disabled
     */
    public MonitoringChangeCommand(String pipeline, Map<MonitoringFrequency, Integer> frequencies) {
        this(pipeline, null, frequencies, null);
    }

    /**
     * Changes the monitoring for a <code>pipeline</code>.
     * 
     * @param pipeline the name of the pipeline (may be <b>null</b> for all)
     * @param frequencies the desired monitoring frequencies, <b>null</b> for unspecified, 0 or negative for 
     *     completely disabled
     * @param observables the enabled/disabled observables, <b>null</b> for unspecified
     */
    public MonitoringChangeCommand(String pipeline, Map<MonitoringFrequency, Integer> frequencies, 
        Map<IObservable, Boolean> observables) {
        this(pipeline, null, frequencies, observables);
    }

    /**
     * Changes the monitoring for a <code>pipelineElement</code>.
     * 
     * @param pipeline the name of the pipeline (may be <b>null</b> for all)
     * @param pipelineElement the pipeline element as class name (may be <b>null</b> for all)
     * @param frequencies the desired monitoring frequencies, <b>null</b> for unspecified, 0 or negative for 
     *     completely disabled
     */
    public MonitoringChangeCommand(String pipeline, String pipelineElement, 
        Map<MonitoringFrequency, Integer> frequencies) {
        this(pipeline, pipelineElement, frequencies, null);
    }

    /**
     * Changes the monitoring for a <code>pipelineElement</code>.
     * 
     * @param pipeline the name of the pipeline (may be <b>null</b> for all)
     * @param pipelineElement the pipeline element as class name (may be <b>null</b> for all)
     * @param frequencies the desired monitoring frequencies, <b>null</b> for unspecified, 0 or negative for 
     *     completely disabled
     * @param observables the enabled/disabled observables, <b>null</b> for unspecified
     */
    public MonitoringChangeCommand(String pipeline, String pipelineElement, 
        Map<MonitoringFrequency, Integer> frequencies, Map<IObservable, Boolean> observables) {
        super(pipeline, pipelineElement);
        this.frequencies = frequencies;
        this.observables = observables;
    }

    @QMInternal
    @Override
    public CoordinationExecutionResult accept(ICoordinationCommandVisitor visitor) {
        return visitor.visitMonitoringChangeCommand(this);
    }
    
    /**
     * Returns the desired monitoring frequencies.
     * 
     * @return the desired monitoring frequencies, <b>null</b> for unspecified, 0 or negative for completely disabled
     */
    public Map<MonitoringFrequency, Integer> getFrequencies() {
        return frequencies;
    }
    
    /**
     * Returns the enabled/disabled observables.
     * 
     * @return the enabled/disabled observables, <b>null</b> for unspecified
     */
    public Map<IObservable, Boolean> getObservables() {
        return observables;
    }

}
