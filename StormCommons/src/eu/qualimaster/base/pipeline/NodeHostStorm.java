package eu.qualimaster.base.pipeline;

import org.apache.log4j.Logger;

import eu.qualimaster.Configuration;
/**
 * Provide the information of the node host in Apache Storm. 
 * @author Cui Qin
 *
 */
public class NodeHostStorm {
    private static final Logger LOGGER = Logger.getLogger(NodeHostStorm.class);
    /**
     * Return the node host based on the configuration in Storm.
     * @param topologyName the topology name
     * @param nodeName the host node
     * @return the host name
     */
    public static String getHost(String topologyName, String nodeName) {
        String host = "localhost";
        LOGGER.info("Getting the host with " + topologyName + ", " + nodeName + ", " + Configuration.getNimbus());
        String nimbusHost = "192.168.0.1"; //Configuration.getNimbus() TODO: hardcode for now, need to be solved.
        host = new CollectingTopologyInfo(topologyName, nodeName, nimbusHost,
                Configuration.getThriftPort()).getExecutorHost();
        return host;
    }
}
