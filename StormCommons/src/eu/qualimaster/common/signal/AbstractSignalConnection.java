package eu.qualimaster.common.signal;

import org.apache.log4j.Logger;
import org.apache.storm.curator.framework.CuratorFramework;
import org.apache.storm.zookeeper.WatchedEvent;
import org.apache.storm.zookeeper.Watcher;
import org.apache.storm.zookeeper.data.Stat;

import eu.qualimaster.Configuration;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.events.IEvent;

/**
 * An abstract signal connection implementing a zookeeper watcher.
 * 
 * @author Cui Qin
 */
public abstract class AbstractSignalConnection implements Watcher {

    private static final Logger LOGGER = Logger.getLogger(AbstractSignalConnection.class);
    private String name;
    private CuratorFramework client;
    private SignalListener listener;
    private EventManager eventManager;

    /**
     * Creates a signal connection.
     * 
     * @param name the name of this element
     * @param listener the signal listener
     */
    public AbstractSignalConnection(String name, SignalListener listener) {
        this.name = name;
        this.listener = listener;
    }
    
    /**
     * Defines the client.
     * 
     * @param client the client instance
     */
    protected void setClient(CuratorFramework client) {
        this.client = client;
    }
    
    /**
     * Returns the client curator framework.
     * 
     * @return the client framework instance
     */
    protected CuratorFramework getClient() {
        return client;
    }
    
    // checkstyle: stop exception type check
    
    /**
     * Initializes the watcher.
     * 
     * @throws Exception in case of execution problems
     */
    protected void initWatcher() throws Exception {
        if (Configuration.getPipelineSignalsCurator()) {
            String path = getWatchedPath();
            Stat stat = client.checkExists().forPath(path);
            if (stat == null) {
                client.create().creatingParentsIfNeeded().forPath(path);
            }
            stat = this.client.checkExists().usingWatcher(this).forPath(path);
        }
    }
    
    /**
     * Returns the watched path.
     * 
     * @return the watched path
     */
    protected String getWatchedPath() {
        //         return SignalMechanism.getTopologyExecutorPath(getTopology(), getExecutor());
        return SignalMechanism.PATH_SEPARATOR + name;
    }

    /**
     * Processes a watched event.
     * 
     * @param we the watched event
     */
    public void process(WatchedEvent we) {
        if (Configuration.getPipelineSignalsCurator()) {
            String path = getWatchedPath();
            try {
                this.client.checkExists().usingWatcher(this).forPath(path);
            } catch (Exception ex) {
                LOGGER.error("Error renewing watch." + ex);
            }
    
            switch (we.getType()) {
            case NodeCreated:
                LOGGER.info("Node created.");
                break;
            case NodeDataChanged:
                LOGGER.info("Received signal.");
                try {
                    String wePath = we.getPath();
                    LOGGER.info("The path is " + wePath);
                    byte[] payload = this.client.getData().forPath(wePath);
                    this.listener.onSignal(payload);
                } catch (Exception e) {
                    LOGGER.error("Warning: Unable to process signal." + e, e);
                }
                break;
            case NodeDeleted:
                LOGGER.info("NodeDeleted");
                break;
            case None:
                break;
            case NodeChildrenChanged:
                break;
            default:
                break;
            }
        }
    }
    
    /**
     * Sends information.
     * 
     * @param toPath the target topology element / path
     * @param signal the signal payload
     * @throws Exception in case of execution problems
     */
    public void send(String toPath, byte[] signal) throws Exception {
        if (Configuration.getPipelineSignalsCurator()) {
            Stat stat = this.client.checkExists().forPath(toPath);
            if (stat == null) {
                this.client.create().creatingParentsIfNeeded().forPath(toPath);
            }
            this.client.setData().forPath(toPath, signal);
        } else {
            LOGGER.warn("attempt to send signal while curator signalling is disabled in this infrastructure: " 
                + toPath + " " + signal);
        }
    }

    // checkstyle: resume exception type check

    /**
     * Closes the connection.
     */
    public void close() {
        if (Configuration.getPipelineSignalsCurator()) {
            this.client.close();
        }
        if (null != eventManager) {
            eventManager.doCleanup();
            eventManager.doStop(); // not fully sure
        }
    }
    
    /**
     * The element name to do the signalling for.
     * 
     * @return the element name
     */
    public String getElementName() {
        return name;
    }
    
    /**
     * Sends an event to the QualiMaster infrastructure.
     * 
     * @param event the event
     */
    protected void sendEvent(IEvent event) {
        if (null == eventManager) {
            eventManager = new EventManager();
        }
        // currently using the event manager, may go via thrift in the future
        eventManager.doSend(event);
    }
    
    /**
     * Sends a topology signal.
     * 
     * @param signal the signal to be sent
     * @throws SignalException in case that the execution / signal sending fails
     */
    public void sendSignal(AbstractTopologyExecutorSignal signal) throws SignalException {
        signal.sendSignal(client);
    }
    
}
