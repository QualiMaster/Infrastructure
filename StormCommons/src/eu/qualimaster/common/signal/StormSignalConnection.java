package eu.qualimaster.common.signal;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.storm.curator.framework.CuratorFramework;
import org.apache.storm.curator.framework.CuratorFrameworkFactory;
import org.apache.storm.curator.framework.state.ConnectionStateListener;
import org.apache.storm.curator.retry.RetryNTimes;

import backtype.storm.utils.Utils;
import eu.qualimaster.Configuration;
import eu.qualimaster.common.logging.QmLogging;
import eu.qualimaster.monitoring.events.AlgorithmChangedMonitoringEvent;

/**
 * Represents a signal connection for Storm.
 * 
 * @author Cui Qin
 */
public class StormSignalConnection extends AbstractSignalConnection {

    private String pipeline;
    private String connectString;
    
    /**
     * Creates a storm signal connection.
     * 
     * @param name the name of this element
     * @param listener the signal listener
     * @param pipeline the name of the pipeline
     * @param conf the storm configuration 
     */
    @SuppressWarnings("rawtypes")
    public StormSignalConnection(String name, SignalListener listener, String pipeline, Map conf) {
        super(name, listener);
        this.pipeline = pipeline;
        this.connectString = zkHosts(conf);
    }

    // checkstyle: stop exception type check
    
    /**
     * Initializes the connection.
     * 
     * @param listener an optional connection state listener (may be <b>null</b>)
     * @throws Exception in case of execution problems
     */
    public void init(ConnectionStateListener listener) throws Exception {
        if (Configuration.getPipelineSignalsCurator()) {
            SignalMechanism.setConnectString(pipeline, connectString);
            int retryCount = Configuration.getZookeeperRetryTimes();
            int retryInterval = Configuration.getZookeeperRetryInterval();
            // use global namespace here - create individual connections for watchers
            CuratorFramework client = CuratorFrameworkFactory.builder().namespace(SignalMechanism.GLOBAL_NAMESPACE).
                connectString(connectString).retryPolicy(new RetryNTimes(retryCount, retryInterval)).build();
            // unsure, so far reuse did sometimes not allow passing all signals
            //CuratorFramework client = SignalMechanism.obtainFramework(SignalMechanism.GLOBAL_NAMESPACE);
            super.setClient(client);
            if (null != listener) {
                client.getConnectionStateListenable().addListener(listener);
            }
            client.start();
            initWatcher(); // failing
        }
        SignalMechanism.initEnabledSignalNamespaceState(pipeline);
    }
    
    /**
     * Returns the watched path.
     * 
     * @return the watched path
     */
    protected String getWatchedPath() {
        return SignalMechanism.getTopologyExecutorPath(getTopology(), getElementName());
    }
    
    /**
     * Returns the Curator zookeeper connect string.
     * 
     * @param conf storm configuration
     * @return the zookeeper connect string
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static String zkHosts(Map conf) {
        int zkPort = Utils.getInt(conf.get(Constants.CONFIG_KEY_STORM_ZOOKEEPER_PORT));
        List<String> zkServers = (List<String>) conf.get(Constants.CONFIG_KEY_STORM_ZOOKEEPER_SERVERS);

        Iterator<String> it = zkServers.iterator();
        StringBuffer sb = new StringBuffer();
        while (it.hasNext()) {
            sb.append(it.next());
            sb.append(":");
            sb.append(zkPort);
            if (it.hasNext()) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    /**
     * Configures the QM event bus from the Storm configuration if possible and
     * installs the QM specific logging ({@link QmLogging#install()}). Call before 
     * {@link Monitor#Monitor(String, String, boolean, backtype.storm.task.TopologyContext)} so that 
     * {@link Configuration#enableVolumeMonitoring()} receives the correct value from <code>conf</code>.
     * 
     * @param conf the storm configuration
     */
    @SuppressWarnings({ "rawtypes" })
    public static void configureEventBus(Map conf) {
        Configuration.transferConfigurationFrom(conf);
        Object tmp = conf.get(QmLogging.ENABLING_PROPERTY);
        boolean enableLogging = false;
        if (tmp instanceof Boolean) {
            enableLogging = ((Boolean) tmp).booleanValue();
        } else if (null != tmp ) {
            enableLogging = Boolean.valueOf(tmp.toString()).booleanValue();
        }
        if (enableLogging) {
            QmLogging.install();
        }
    }
    
    // checkstyle: resume exception type check
    
    /**
     * Returns the thrift namespace.
     * 
     * @return the thrift namespace
     */
    public String getNamespace() {
        return pipeline; // keep as "virtual" namespace for namespace state
    }
    
    /**
     * Returns the pipeline name.
     * 
     * @return the pipeline name 
     */
    public String getTopology() {
        return pipeline;
    }
    
    /**
     * Sends an algorithm changed event.
     * 
     * @param algorithm the name of the new algorithm
     */
    public void sendAlgorithmChangedEvent(String algorithm) {
        sendEvent(new AlgorithmChangedMonitoringEvent(pipeline, getElementName(), algorithm));
    }

    
}