package eu.qualimaster.monitoring;

import eu.qualimaster.monitoring.systemState.SystemState;

/**
 * Realizes an abstract monitoring task on a cluster of machines.
 * 
 * @author Holger Eichelberger
 */
public abstract class AbstractClusterMonitoringTask extends AbstractMonitoringTask {

    /**
     * Creates an abstract cluster monitoring task.
     * 
     * @param state the system state to be modified during monitoring
     */
    protected AbstractClusterMonitoringTask(SystemState state) {
        super(state);
    }
    
}
