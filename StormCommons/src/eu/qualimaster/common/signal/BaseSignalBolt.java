package eu.qualimaster.common.signal;

import java.util.Map;

import org.apache.log4j.Logger;

import eu.qualimaster.Configuration;
import eu.qualimaster.common.shedding.LoadShedder;
import eu.qualimaster.common.shedding.LoadShedderFactory;
import eu.qualimaster.common.shedding.NoShedder;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.monitoring.events.LoadSheddingChangedMonitoringEvent;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.base.BaseRichBolt;

/**
 * Extends the basic Storm Bolt by signalling capabilities.
 * 
 * @author Cui Qin
 */
@SuppressWarnings("serial")
public abstract class BaseSignalBolt extends BaseRichBolt implements SignalListener, IAlgorithmChangeListener, 
    IParameterChangeListener, IShutdownListener, ILoadSheddingListener {

    private static final Logger LOGGER = Logger.getLogger(BaseSignalBolt.class);
    private String name;
    private String namespace;
    private boolean sendRegular;
    private LoadShedder<?> shedder = NoShedder.INSTANCE;
    private transient StormSignalConnection signalConnection;
    private transient AlgorithmChangeEventHandler algorithmEventHandler;
    private transient ParameterChangeEventHandler parameterEventHandler;
    private transient ShutdownEventHandler shutdownEventHandler;
    private transient Monitor monitor;

    /**
     * Creates a base signal Bolt with no regular event sending.
     * 
     * @param name the name of the bolt
     * @param namespace the namespace of the bolt
     */
    public BaseSignalBolt(String name, String namespace) {
        this(name, namespace, false);
    }
    
    /**
     * Creates a base signal Bolt.
     * 
     * @param name the name of the bolt
     * @param namespace the namespace of the bolt
     * @param sendRegular whether this monitor shall care for sending regular events (<code>true</code>) or 
     *     not (<code>false</code>, for thrift-based monitoring)
     */
    public BaseSignalBolt(String name, String namespace, boolean sendRegular) {
        this.name = name;
        this.namespace = namespace;
        this.sendRegular = sendRegular;
    }

    // checkstyle: stop exception type check

    @Override
    @SuppressWarnings("rawtypes")
    public void prepare(Map conf, TopologyContext context, OutputCollector collector) {
        if (conf.containsKey(Constants.CONFIG_KEY_SUBPIPELINE_NAME)) {
            namespace = (String) conf.get(Constants.CONFIG_KEY_SUBPIPELINE_NAME);
        }
        StormSignalConnection.configureEventBus(conf);
        monitor = new Monitor(namespace, name, true, context, sendRegular);
        try {
            LOGGER.info("Prepare--basesignalbolt....");
            signalConnection = new StormSignalConnection(this.name, this, namespace);
            signalConnection.init(conf);
            if (Configuration.getPipelineSignalsQmEvents()) {
                algorithmEventHandler = AlgorithmChangeEventHandler.createAndRegister(this, namespace, name);
                parameterEventHandler = ParameterChangeEventHandler.createAndRegister(this, namespace, name);
                shutdownEventHandler = ShutdownEventHandler.createAndRegister(this, namespace, name);
            }
        } catch (Exception e) {
            LOGGER.error("Error SignalConnection:" + e.getMessage(), e);
        }
    }
    
    /**
     * Aggregate the execution time and send the recorded value to the
     * monitoring layer. Shall be used only in combination with a corresponding
     * start time measurement. Assumes one item processed. [convenience]
     * 
     * @param start the start execution time
     */
    protected void aggregateExecutionTime(long start) {
        monitor.aggregateExecutionTime(start);
    }
    
    /**
     * Aggregate the execution time and send the recorded value to the
     * monitoring layer. Shall be used only in combination with a corresponding
     * start time measurement. [convenience]
     * 
     * @param start the start execution time
     * @param itemsCount the number of items processed
     */
    protected void aggregateExecutionTime(long start, int itemsCount) {
        monitor.aggregateExecutionTime(start, itemsCount);
    }
    
    /**
     * Returns the monitoring support class.
     * 
     * @return the monitoring support instance
     */
    protected Monitor getMonitor() {
        return monitor;
    }

    /**
     * Sends a signal.
     * 
     * @param toPath the target element (path)
     * @param signal the signal to be sent
     * @throws Exception in case of execution problems
     */
    @Deprecated
    public void sendSignal(String toPath, byte[] signal) throws Exception {
        this.signalConnection.send(toPath, signal);
    }

    /**
     * Sends a signal via the signal connection of this bolt.
     * 
     * @param signal the signal to be sent
     * @throws SignalException in case that the execution / signal sending fails
     */
    protected void sendSignal(TopologySignal signal) throws SignalException {
        signal.sendSignal(this.signalConnection);
    }
    
    // checkstyle: resume exception type check

    /**
     * Sends an algorithm changed event.
     * 
     * @param algorithm the new algorithm enacted
     */
    protected void sendAlgorithmChangedEvent(String algorithm) {
        signalConnection.sendAlgorithmChangedEvent(algorithm);
    }

    @Override
    public void onSignal(byte[] data) {
        boolean done = AlgorithmChangeSignal.notify(data, namespace, name, this);
        if (!done) {
            done = ParameterChangeSignal.notify(data, namespace, name, this);
        }
        if (!done) {
            done = ShutdownSignal.notify(data, namespace, name, this);
        }
        if (!done) {
            done = LoadSheddingSignal.notify(data, namespace, name, this);
        }
    }

    @Override
    public void notifyAlgorithmChange(AlgorithmChangeSignal signal) {
     // empty: keep interface/implementations stable
    }

    @Override
    public void notifyParameterChange(ParameterChangeSignal signal) {
     // empty: keep interface/implementations stable
    }

    // intentionally final so that subclasses cannot overwrite required shutdown sequence
    @Override
    public final void notifyShutdown(ShutdownSignal signal) {
        prepareShutdown(signal);
        monitor.shutdown();
        signalConnection.close();
        if (Configuration.getPipelineSignalsQmEvents()) {
            EventManager.unregister(algorithmEventHandler);
            EventManager.unregister(parameterEventHandler);
            EventManager.unregister(shutdownEventHandler);
        }
        EventManager.stop(); // end of pipeline, don't process missing events
    }

    @Override
    public void notifyLoadShedding(LoadSheddingSignal signal) {
        shedder = LoadShedderFactory.createShedder(signal.getShedder());
        shedder.configure(signal);
        EventManager.send(new LoadSheddingChangedMonitoringEvent(namespace, name, 
            signal.getShedder(), shedder.getDescriptor().getIdentifier(), signal.getCauseMessageId()));
    }
    
    /**
     * Returns the active load shedder.
     * 
     * @return the load shedder
     */
    protected LoadShedder<?> getShedder() {
        return shedder;
    }
    
    /**
     * Asks the active load shedder whether the given <code>tuple</code> is enabled.
     * 
     * @param tuple the tuple to ask for
     * @return <code>true</code> for enabled for processing, <code>false</code> for throw <code>tuple</code> away
     */
    protected boolean isEnabled(Object tuple) {
        return shedder.isEnabled(tuple);
    }
    
    /**
     * Called to prepare the shutdown of this executor as part of shutting down
     * the related topology. Please do not perform time consuming actions here,
     * or adjust the maximum guaranteed shutdown waiting time in 
     * {@link eu.qualimaster.Configuration#getShutdownSignalWaitTime()}. Please ensure
     * that after cleanup the remaining implementation does not throw any exceptions,
     * e.g., setting dynamic algorithms to <b>null</b> while the remainder of the 
     * implementation cannot handle a <b>null</b> algorithm.
     * This method is called by {@link #notifyShutdown(ShutdownSignal)}.
     * 
     * @param signal the shutdown signal
     */
    protected void prepareShutdown(ShutdownSignal signal) {
        // empty: keep interface/implementations stable
    }

    /**
     * Returns the name of this bolt.
     * 
     * @return the name of this bolt
     */
    public String getName() {
        return name;
    }
    
    /**
     * Returns the namespace of this bolt.
     * 
     * @return the namespace of this bolt
     */
    public String getNamespace() {
        return namespace;
    }

}
