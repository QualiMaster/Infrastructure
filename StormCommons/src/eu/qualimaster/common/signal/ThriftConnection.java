package eu.qualimaster.common.signal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.thrift7.TException;
import org.apache.thrift7.protocol.TBinaryProtocol;
import org.apache.thrift7.transport.TFramedTransport;
import org.apache.thrift7.transport.TSocket;
import org.apache.thrift7.transport.TTransportException;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.json.simple.JSONValue;

import eu.qualimaster.Configuration;
import backtype.storm.ILocalCluster;
import backtype.storm.generated.ClusterSummary;
import backtype.storm.generated.ExecutorSummary;
import backtype.storm.generated.NotAliveException;
import backtype.storm.generated.StormTopology;
import backtype.storm.generated.SupervisorSummary;
import backtype.storm.generated.TopologySummary;
import backtype.storm.generated.Nimbus.Client;
import backtype.storm.generated.TopologyInfo;

/**
 * Represents a Storm/Thrift connection. Allows graceful connections. Please consider setting the system property
 * <code>storm.conf.file</code> and make sure that it is on the classpath!
 * 
 * @author Holger Eichelberger
 */
public class ThriftConnection {

    private static final Logger LOGGER = LogManager.getLogger(ThriftConnection.class);
    private static ILocalCluster localCluster;
    private Client client;
    private TFramedTransport transport;
    private TSocket socket;
    private TBinaryProtocol protocol;

    /**
     * Creates a connection from the infrastucture configuration object.
     */
    public ThriftConnection() {
        this(Configuration.getNimbus(), Configuration.getThriftPort());
    }
    
    /**
     * Creates a connection with explicit nimbus host.
     * 
     * @param nimbusHost the nimbus host name
     * @param port the nimbus port number
     */
    public ThriftConnection(String nimbusHost, int port) {
        socket = new TSocket(nimbusHost, port);
        LOGGER.info("Thrift connection info " + nimbusHost + " " + port);
        transport = new TFramedTransport(socket);
        protocol = new TBinaryProtocol(transport);
        client = new Client(protocol);
    }
    
    /**
     * Sets the local cluster for testing. If a local cluster is set,
     * information requests are delegated to the local cluster.
     * 
     * @param cluster the cluster
     */
    public static final void setLocalCluster(ILocalCluster cluster) {
        localCluster = cluster;
    }
    
    /**
     * Returns whether the connection is open.
     * 
     * @return <code>true</code> if the connection is open, <code>false</code> else (consider calling {@link #open()}.
     */
    public boolean isOpen() {
        return null != localCluster || (null != transport && transport.isOpen());
    }
    
    /**
     * Opens a connection if not already open.
     * 
     * @return <code>true</code> if the connection is open, <code>false</code> else
     */
    public boolean open() {
        boolean isOpen = false;
        if (!isOpen()) {
            try {
                transport.open();
                isOpen = true;
            } catch (TTransportException e) {
                LOGGER.info("Cannot start Thrift transport " + e.getMessage());
            } catch (NullPointerException e) {
                // if there is no storm, the library will result in a NPE
            }
        } else {
            isOpen = true;
            if (null != localCluster) {
                client = null;
                protocol = null;
                transport = null;
                socket = null;
            }
        }
        return isOpen;
    }
    
    /**
     * Closes the connection.
     */
    public void close() {
        if (null != transport) {
            if (transport.isOpen()) {
                transport.close();
            }
        }
        if (null != socket) {
            socket.close();
        }
    }
    
    /**
     * Returns the cluster summary.
     * 
     * @return the cluster summary
     * @throws TException in case of problems accessing the remote cluster summary
     */
    public ClusterSummary getClusterSummary() throws TException {
        ClusterSummary result;
        if (null != client) {
            result = client.getClusterInfo();
        } else if (null != localCluster) {
            result = localCluster.getClusterInfo();
        } else {
            throw new TException("connection not open");
        }
        return result;
    }

    /**
     * Returns a topology information by <code>id</code>.
     * 
     * @param id the topology identification
     * @return the topology information
     * @throws NotAliveException in case that the requested topology is not alive
     * @throws TException in case of problems accessing the remote topology info
     */
    public TopologyInfo getTopologyInfo(String id) throws NotAliveException, TException {
        TopologyInfo result;
        if (null == id) {
            throw new TException("illegal topology name: " + id);
        }
        if (null != client) {
            result = client.getTopologyInfo(id);
        } else if (null != localCluster) {
            result = localCluster.getTopologyInfo(id);
        } else {
            throw new TException("connection not open");
        }
        return result;
    }

    /**
     * Returns a topology information by <code>name</code>.
     * 
     * @param name the topology name
     * @return the topology information (may be <b>null</b>)
     * @throws NotAliveException in case that the requested topology is not alive
     * @throws TException in case of problems accessing the remote topology info
     */
    public TopologyInfo getTopologyInfoByName(String name) throws NotAliveException, TException {
        TopologySummary summary = getTopologySummaryByName(name);
        return null == summary ? null : getTopologyInfo(summary.get_id());
    }

    /**
     * Returns a topology summary by <code>name</code>.
     * 
     * @param name the topology name
     * @return the topology summary (may be <b>null</b>)
     * @throws TException in case of problems accessing the remote topology info
     */
    public TopologySummary getTopologySummaryByName(String name) throws TException {
        TopologySummary result = null;
        ClusterSummary summary = getClusterSummary();
        List<TopologySummary> topologies = summary.get_topologies();
        for (int t = 0; null == result && t < topologies.size(); t++) {
            TopologySummary tSummary = topologies.get(t);
            if (tSummary.get_name().equals(name)) {
                result = tSummary;
            }
        }
        return result;
    }

    /**
     * Returns the topology configuration including the connections among the executors.
     * 
     * @param topology the topology id
     * @return the topology configuration, <b>null</b> in case of errors (if not exception)
     * @throws NotAliveException in case that the requested topology is not alive
     * @throws TException in case of problems accessing the remote topology info
     */
    public Map<String, Object> getTopologyConfiguration(TopologyInfo topology) throws TException, NotAliveException {
        Map<String, Object> result = null;
        String tmp = null;
        if (null != client) {
            tmp = client.getTopologyConf(topology.get_id());
        } else if (null != localCluster) {
            tmp = localCluster.getTopologyConf(topology.get_id());
        } else {
            throw new TException("connection not open");
        }
        if (null != tmp) {
            Object obj = JSONValue.parse(tmp);
            if (obj instanceof JSONObject) {
                JSONObject json = (JSONObject) obj;
                result = new HashMap<String, Object>();
                Iterator<?> iter = json.keys();
                while (iter.hasNext()) {
                    String key = iter.next().toString();
                    try {
                        result.put(key, json.get(key));
                    } catch (JSONException e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Returns the storm topology description structure for the given topology information.
     * 
     * @param topology the topology information
     * @return the topology description structure
     * @throws NotAliveException in case that the requested topology is not alive
     * @throws TException in case of problems accessing the remote topology info
     */
    public StormTopology getTopology(TopologyInfo topology) throws TException, NotAliveException {
        return getTopology(topology.get_id());
    }

    /**
     * Returns the storm topology description structure for the given topology identification.
     * 
     * @param topologyId the topology identification
     * @return the topology description structure
     * @throws NotAliveException in case that the requested topology is not alive
     * @throws TException in case of problems accessing the remote topology info
     */
    public StormTopology getTopology(String topologyId) throws TException, NotAliveException {
        StormTopology result;
        if (null != client) {
            result = client.getTopology(topologyId);
        } else if (null != localCluster) {
            result = localCluster.getTopology(topologyId);
        } else {
            throw new TException("connection not open");
        }
        return result;
    }
    
    /**
     * Returns the ports used by a topology.
     * 
     * @param topology the topology to return the ports for
     * @return the used ports
     */
    public static Set<Integer> getUsedPorts(TopologyInfo topology) {
        Set<Integer> result = new HashSet<Integer>();
        List<ExecutorSummary> executors = topology.get_executors();
        for (int e = 0; e < executors.size(); e++) {
            result.add(executors.get(e).get_port());
        }
        return result;
    }
    
    /**
     * Returns the ports used by a topology on <code>host</code>.
     * 
     * @param topology the topology to return the ports for
     * @param host the host name
     * @return the used ports
     */
    public static Set<Integer> getUsedPort(TopologyInfo topology, String host) {
        Set<Integer> result = new HashSet<Integer>();
        List<ExecutorSummary> executors = topology.get_executors();
        for (int e = 0; e < executors.size(); e++) {
            ExecutorSummary executor = executors.get(e);
            if (executor.get_host().equals(host)) {
                result.add(executor.get_port());
            }
        }
        return result;
    }
    
    /**
     * Returns the mapping of supervisor host names to supervisor ids.
     * 
     * @param summary the cluster summary
     * @return the mapping (a host may run multiple supervisors and then we return them in the sequence given 
     *   by thrift)
     */
    public static Map<String, List<String>> getSupervisorHostIdMapping(ClusterSummary summary) {
        Map<String, List<String>> result = new HashMap<String, List<String>>();
        List<SupervisorSummary> supervisors = summary.get_supervisors();
        for (int s = 0; s < summary.get_supervisors_size(); s++) {
            SupervisorSummary supervisor = supervisors.get(s);
            String host = supervisor.get_host();
            List<String> tmp = result.get(host);
            if (null == tmp) {
                tmp = new ArrayList<String>();
                result.put(host, tmp);
            }
            tmp.add(supervisor.get_supervisor_id());
        }
        return result;
    }

    /**
     * Returns the mapping of supervisor ids to host names.
     * 
     * @param summary the cluster summary
     * @return the mapping
     */
    public static Map<String, String> getSupervisorIdHostMapping(ClusterSummary summary) {
        Map<String, String> result = new HashMap<String, String>();
        List<SupervisorSummary> supervisors = summary.get_supervisors();
        for (int s = 0; s < summary.get_supervisors_size(); s++) {
            SupervisorSummary supervisor = supervisors.get(s);
            result.put(supervisor.get_supervisor_id(), supervisor.get_host());
        }
        return result;
    }

}