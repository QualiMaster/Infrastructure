package eu.qualimaster.observables;

import eu.qualimaster.common.QMInternal;

/**
 * Resource observables (see D4.1, D4.2).
 * 
 * @author Holger Eichelberger
 */
public enum ResourceUsage implements IObservable {
    
    MEMORY_USE, // TODO USED_MEMORY
    AVAILABLE_MEMORY,
    AVAILABLE_FREQUENCY,
    
    /**
     * Denotes the system load of a machine.
     */
    LOAD,
    
    /**
     * Denotes the number of actually available machines (&gt;=0).
     */
    AVAILABLE_MACHINES,
    
    /**
     * Denotes the number of actually used macines (&gt;=0).
     */
    USED_MACHINES,

    /**
     * Denotes the number of actually available DFEs (reconfigurable hardware, &gt;=0).
     */
    AVAILABLE_DFES,
    
    /**
     * Denotes the number of actually used CPUs (reconfigurable hardware, &gt;=0).
     */
    USED_DFES,
    BANDWIDTH,

    // new
    /**
     * Denotes the capacity of a processing element (0;1).
     */
    CAPACITY,
    
    /**
     * Denotes the number of actually running executors (&gt;=0).
     */
    EXECUTORS,

    /**
     * Denotes the number of actually running tasks (&gt;=0). This may differ from the configuration
     * due to {@link eu.qualimaster.infrastructure.PipelineOptions}. 
     */
    TASKS,
    
    /**
     * Denotes the number of hosts an element is actually running at (&gt;=0).
     */
    HOSTS,

    /**
     * Denotes the number of actually available CPUs (reconfigurable hardware, &gt;=0).
     */
    AVAILABLE_CPUS,
    
    /**
     * Denotes the number of actually used CPUs (reconfigurable hardware, &gt;=0).
     */
    USED_CPUS,
    
    /**
     * Denotes the state whether a machine is available at all (0;1).
     */
    AVAILABLE;
    
    @QMInternal
    @Override
    public boolean isInternal() {
        return false;
    }
    
}
