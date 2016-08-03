package eu.qualimaster.common.signal;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.storm.curator.framework.CuratorFramework;
import org.apache.storm.curator.framework.CuratorFrameworkFactory;
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
    
    /**
     * Creates a storm signal connection.
     * 
     * @param name the name of this element
     * @param listener the signal listener
     * @param pipeline the name of the pipeline 
     */
    public StormSignalConnection(String name, SignalListener listener, String pipeline) {
        super(name, listener);
        this.pipeline = pipeline;
    }

    // checkstyle: stop exception type check
    
    /**
     * Initializes the connection.
     * 
     * @param conf the storm configuration
     * @throws Exception in case of execution problems
     */
    @SuppressWarnings("rawtypes")
    public void init(Map conf) throws Exception {
        if (Configuration.getPipelineSignalsCurator()) {
            String connectString = zkHosts(conf);
            SignalMechanism.setConnectString(pipeline, connectString);
            int retryCount = Utils.getInt(conf.get("storm.zookeeper.retry.times"));
            int retryInterval = Utils.getInt(conf.get("storm.zookeeper.retry.interval"));
            // use global namespace here
            CuratorFramework client = CuratorFrameworkFactory.builder().namespace(SignalMechanism.GLOBAL_NAMESPACE).
                connectString(connectString).retryPolicy(new RetryNTimes(retryCount, retryInterval)).build();
            super.setClient(client);
            client.start();
    
            initWatcher(); // failing
        }
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
        int zkPort = Utils.getInt(conf.get("storm.zookeeper.port"));
        List<String> zkServers = (List<String>) conf.get("storm.zookeeper.servers");

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
     * installs the QM specific logging ({@link QmLogging#install()}).
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