package eu.qualimaster.base.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;

import eu.qualimaster.Configuration;
import eu.qualimaster.common.switching.TupleSender;
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
    
    public static List<TupleSender> createTupleSenders(String topologyName, String nodeName, int port) {
		List<TupleSender> senders = new ArrayList<TupleSender>();
		LOGGER.info("Getting the host with " + topologyName + ", " + nodeName + ", " + Configuration.getNimbus());
        String nimbusHost = "192.168.0.1"; //Configuration.getNimbus() TODO: hardcode for now, need to be solved.
		List<String> hosts = new CollectingTopologyInfo(topologyName, nodeName, nimbusHost, Configuration.getThriftPort()).getExecutorHostList();
		LOGGER.info("The executor: " + nodeName + "-- host size: " + hosts.size());
		for (String host : hosts) {
			TupleSender sender =  new TupleSender(host, port);
//			while(!sender.isConnected()) {
//				sender.connect();
//			}
			senders.add(sender);
		}
		return senders;
	}	
	
	public static TupleSender shuffleSender(List<TupleSender> senders) {
		TupleSender result = null;
        if (!senders.isEmpty()) {
            int index = new Random().nextInt(senders.size());
            result = senders.get(index);
        }
        return result;
    }
    
}
