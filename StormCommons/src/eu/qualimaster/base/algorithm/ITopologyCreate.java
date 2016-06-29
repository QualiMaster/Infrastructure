package eu.qualimaster.base.algorithm;

import backtype.storm.Config;
import backtype.storm.topology.TopologyBuilder;

/**
 * Non-generation Define the interface for creating topology-based algorithm.
 **/
public interface ITopologyCreate {

    /**
     * Create the algorithm topology.
     * 
     * @param builder the topology builder to define the topology on
     * @param config the Storm cluster configuration
     * @param prefix the prefix to be added before each name used by sub-topology
     *     (may be empty)
     * @param input the spout/bolt name to connect to
     * @param streamId the streamId to connect to
     * @return the output Spout to go on
     **/
    public SubTopologyOutput createSubTopology(TopologyBuilder builder, Config config, String prefix, String input,
        String streamId);

}
