package eu.qualimaster.base.algorithm;

/**
 * Define the interface for creating a main topology.
 * @author qin
 *
 */
public interface IMainTopologyCreate {

    /**
     * Create the main topology for the pipeline.
     * @return a TopologyOutput carring the storm config and TopologyBuilder information
     */
    public TopologyOutput createMainTopology();
}
