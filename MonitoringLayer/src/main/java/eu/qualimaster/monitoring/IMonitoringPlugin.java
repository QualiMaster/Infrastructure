package eu.qualimaster.monitoring;

import eu.qualimaster.monitoring.systemState.SystemState;

/**
 * A monitoring plugin to perform regular monitoring tasks via one of the interfaces
 * of the Execution Systems.
 * 
 * @author Holger Eichelberger
 */
public interface IMonitoringPlugin {

    /**
     * Starts the plugin.
     */
    public void start();

    /**
     * Stops the plugin.
     */
    public void stop();
    
    /**
     * Creates a container (e.g., pipeline) monitoring task.
     * 
     * @param componentName the name of the component to create the task for
     * @param state the system state to be modified due to monitoring
     * @return the monitoring task or <b>null</b> if no monitoring shall happen
     */
    public AbstractContainerMonitoringTask createPipelineTask(String componentName, SystemState state);

    /**
     * Creates a cluster monitoring task for monitoring on cluster level.
     * 
     * @param state the system state to be modified due to monitoring
     * @return the monitoring task or <b>null</b> if no monitoring shall happen
     */
    public AbstractClusterMonitoringTask createClusterTask(SystemState state);

}
