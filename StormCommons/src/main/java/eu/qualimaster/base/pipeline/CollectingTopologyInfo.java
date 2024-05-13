package eu.qualimaster.base.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.apache.thrift7.TException;

import backtype.storm.generated.ClusterSummary;
import backtype.storm.generated.ExecutorSummary;
import backtype.storm.generated.NotAliveException;
import backtype.storm.generated.TopologyInfo;
import backtype.storm.generated.TopologySummary;
import eu.qualimaster.common.signal.ThriftConnection;

/**
 * Collects the topology runtime information.
 * 
 * @author qin
 * 
 */
public class CollectingTopologyInfo {
    private static Logger logger = Logger.getLogger(CollectingTopologyInfo.class);
    private static ThriftConnection connection;
    private String pipelineName;
    private String executorName;

    /**
     * Creates a collecting topology info with pipeline name and element name.
     * 
     * @param pipelineName
     *            the pipeline to search for
     * @param executorName
     *            the executor to search for
     * @param nimbusHost the host of the nimbus
     * @param thriftPort the port of the thrift
     */
    public CollectingTopologyInfo(String pipelineName, String executorName, String nimbusHost, int thriftPort) {
        this.pipelineName = pipelineName;
        this.executorName = executorName;
        openThriftConnection(nimbusHost, thriftPort);
    }

    /**
     * Opens a thrift connection.
     * 
     * @param nimbusHost the host of the nimbus
     * @param thriftPort the port of the thrift
     */
    public static void openThriftConnection(String nimbusHost, int thriftPort) {
        connection = new ThriftConnection(nimbusHost, thriftPort);
        if (connection != null) {
            logger.info("Opening the thrift connection...");
            connection.open();
        }
    }

    /**
     * Closes the thrift connection.
     */
    public static void closeThriftConnection() {
        connection.close();
    }

    /**
     * Gets the topology info based on the pipeline name.
     * 
     * @param pipelineName
     *            the pipeline to search for
     * @return the topology info (<b>null</b> if not found)
     */
    public TopologyInfo getTopologyInfo(String pipelineName) {
        TopologyInfo result = null;
        ClusterSummary summary;
        try {
            logger.info("The thrift connection is " + connection);
            if (connection != null) {
                summary = connection.getClusterSummary();
                List<TopologySummary> topologies = summary.get_topologies();
                for (int t = 0; t < topologies.size(); t++) {
                    TopologySummary topologySummary = topologies.get(t);
                    if (pipelineName.equals(topologySummary.get_name())) {
                        try {
                            logger.info("Obtaining the TopologyInfo for the pipeine: " + pipelineName);
                            result = connection.getTopologyInfo(topologySummary
                                .get_id());
                            logger.info("the TopologyInfo is " + result);
                        } catch (NotAliveException | TException e) {
                        }
                    }
                }
            }
        } catch (TException e1) {
            e1.printStackTrace();
        }
        return result;
    }

    /**
     * Returns the list of the host of the specific executor in the pipeline.
     * 
     * @return the list of the host of the executor (or <b>null</b> if not found)
     */
    public List<String> getExecutorHostList() {
        List<String> hosts = new ArrayList<String>();
        TopologyInfo topologyInfo = getTopologyInfo(pipelineName);
        if (topologyInfo != null) {
            List<ExecutorSummary> executors = topologyInfo.get_executors();
            for (int e = 0; e < executors.size(); e++) {
                ExecutorSummary executor = executors.get(e);
                String nodeName = executor.get_component_id();
                if (nodeName.equals(executorName)) {
                    hosts.add(executor.get_host());
                }
            }
        }
        return hosts;
    }
    
    /**
     * Returns the host of the specific executor in the pipeline.
     * 
     * @return the host of the executor (or <b>null</b> if not found)
     */
    public String getExecutorHost() {
        return shuffleHost(getExecutorHostList());
    }

    /**
     * Returns a host selecting randomly from the list of hosts.
     * 
     * @param hosts
     *            the list of hosts to select
     * @return the selected host (or <b>null</b> if no hosts found)
     */
    public String shuffleHost(List<String> hosts) {
        String result = null;
        if (!hosts.isEmpty()) {
            int index = new Random().nextInt(hosts.size());
            result = hosts.get(index);
        }
        return result;
    }

}
