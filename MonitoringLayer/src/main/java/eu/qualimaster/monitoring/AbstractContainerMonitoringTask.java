package eu.qualimaster.monitoring;

import eu.qualimaster.monitoring.systemState.SystemState;

/**
 * Realizes a new abstract container (e.g., pipeline) monitoring task.
 * 
 * @author Holger Eichelberger
 */
public abstract class AbstractContainerMonitoringTask extends AbstractMonitoringTask {
    
    /**
     * Creates the container monitoring task.
     * 
     * @param state the system state to modify due to monitoring
     */
    protected AbstractContainerMonitoringTask(SystemState state) {
        super(state);
    }

}
