package eu.qualimaster.base.algorithm;

import backtype.storm.*;
import backtype.storm.topology.TopologyBuilder;

/**
 * Define a class for the topology carring the config, TopologyBuilder and numWorkers information.
 * 
 * @author Cui Qin
 */

public class TopologyOutput {
    private Config config;
    private TopologyBuilder builder;
    private int numWorkers;
    
    /**
     * Constructuor of TopologyOutput class.
     * 
     * @param config the storm topology config 
     * @param builder the topology builder
     * @param numWorkers the number of the workers
     */
    public TopologyOutput(Config config, TopologyBuilder builder, int numWorkers) {
        this.config = config;
        this.builder = builder;
        this.numWorkers = numWorkers;
    }

    /**
     * Returns the storm config.
     * @return the storm config
     */
    public Config getConfig() {
        return config;
    }

    /**
     * Sets the storm config.
     * @param config the storm config
     */
    public void setConfig(Config config) {
        this.config = config;
    }

    /**
     * Returns the topology builder.
     * @return the topology builder
     */
    public TopologyBuilder getBuilder() {
        return builder;
    }

    /**
     * Sets the topology builder.
     * @param builder the topology builder
     */
    public void setBuilder(TopologyBuilder builder) {
        this.builder = builder;
    }

    /**
     * Returns the number of the workers in the topology.
     * @return the number of the workers
     */
    public int getNumWorkers() {
        return numWorkers;
    }

    /**
     * Sets the number of the workers.
     * @param numWorkers the number of the workers
     */
    public void setNumWorkers(int numWorkers) {
        this.numWorkers = numWorkers;
    }
        
    
}
