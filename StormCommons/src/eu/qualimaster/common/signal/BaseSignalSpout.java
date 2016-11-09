package eu.qualimaster.common.signal;

import java.util.Map;

import org.apache.log4j.Logger;

import eu.qualimaster.Configuration;
import eu.qualimaster.common.monitoring.MonitoringPluginRegistry;
import eu.qualimaster.common.shedding.LoadShedder;
import eu.qualimaster.common.shedding.LoadShedderFactory;
import eu.qualimaster.common.shedding.NoShedder;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.monitoring.events.ComponentKeyRegistry;
import eu.qualimaster.monitoring.events.LoadSheddingChangedMonitoringEvent;
import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.base.*;

/**
 * Implements a basic signalling spout.
 * 
 * @author Cui Qin
 * @author Holger Eichelberger
 */
@SuppressWarnings("serial")
public abstract class BaseSignalSpout extends BaseRichSpout implements SignalListener, IParameterChangeListener, 
    IShutdownListener, ILoadSheddingListener, IMonitoringChangeListener {

    private String name;
    private String pipeline;
    private boolean sendRegular;
    private LoadShedder<?> shedder = NoShedder.INSTANCE;
    private transient StormSignalConnection signalConnection;
    private transient ParameterChangeEventHandler parameterEventHandler;
    private transient ShutdownEventHandler shutdownEventHandler;
    private transient Monitor monitor;
    private transient PortManager portManager;
    
    /**
     * Creates a signal spout.
     * 
     * @param name the name of the spout
     * @param pipeline the name of the containing pipeline
     */
    public BaseSignalSpout(String name, String pipeline) {
        this(name, pipeline, false);
    }

    /**
     * Creates a signal spout.
     * 
     * @param name the name of the spout
     * @param pipeline the name of the containing pipeline
     * @param sendRegular whether this monitor shall care for sending regular events (<code>true</code>) or 
     *     not (<code>false</code>, for thrift-based monitoring)
     */
    public BaseSignalSpout(String name, String pipeline, boolean sendRegular) {
        this.name = name;
        this.pipeline = pipeline;
        this.sendRegular = sendRegular;
    }

    // checkstyle: stop exception type check

    @SuppressWarnings("rawtypes")
    @Override
    public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
        if (conf.containsKey(Constants.CONFIG_KEY_SUBPIPELINE_NAME)) {
            pipeline = (String) conf.get(Constants.CONFIG_KEY_SUBPIPELINE_NAME);
        }
        StormSignalConnection.configureEventBus(conf);
        monitor = createMonitor(pipeline, name, true, context, sendRegular);
        if (Constants.MEASURE_BY_TASK_HOOKS) {
            context.addTaskHook(monitor);
        }
        try {
            getLogger().info("Prepare--basesignalspout.... " + pipeline + "/" + this.name);
            signalConnection = new StormSignalConnection(this.name, this, pipeline);
            signalConnection.init(conf);
            getLogger().info("Prepared--basesignalspout.... " + pipeline + "/" + this.name);
            if (Configuration.getPipelineSignalsQmEvents()) {
                parameterEventHandler = ParameterChangeEventHandler.createAndRegister(this, pipeline, name);
                shutdownEventHandler = ShutdownEventHandler.createAndRegister(this, pipeline, name);
            }
            portManager = BaseSignalBolt.createPortManager(signalConnection, conf);
        } catch (Exception e) {
            getLogger().error("Error SignalConnection:" + e.getMessage(), e);
        }
        ComponentKeyRegistry.register(pipeline, this, monitor.getComponentKey());
    }
    
    /**
     * Creates the monitor instance.
     * 
     * @param namespace the namespace (pipeline name)
     * @param name the element name
     * @param includeItems whether the send items shall also be included
     * @param context the topology context for creating the component id
     * @param sendRegular whether this monitor shall care for sending regular events (<code>true</code>) or 
     *     not (<code>false</code>, for thrift-based monitoring)
     * @return the monitor instance (must not be <b>null</b>)
     */
    protected Monitor createMonitor(String namespace, String name, boolean includeItems, TopologyContext context, 
        boolean sendRegular) {
        return new Monitor(namespace, name, true, context, sendRegular);
    }
    
    /**
     * Returns the port manager.
     * 
     * @return the port manager
     */
    protected PortManager getPortManager() {
        return portManager;
    }
    
    /**
     * Starts monitoring for an execution method.
     */
    protected void startMonitoring() {
        monitor.startMonitoring();
        MonitoringPluginRegistry.startMonitoring();
    }
    
    /**
     * Returns a new monitor for a parallel thread.
     * 
     * @return the monitor
     */
    protected ThreadMonitor createThreadMonitor() {
        return monitor.createThreadMonitor();
    }

    /**
     * Ends monitoring for an execution method.
     */
    protected void endMonitoring() {
        monitor.endMonitoring();
        MonitoringPluginRegistry.endMonitoring();
    }

    /**
     * Aggregate the execution time and send the recorded value to the
     * monitoring layer. Shall be used only in combination with a corresponding
     * start time measurement. Assumes one item processed. [convenience]
     * 
     * @param start the start execution time
     * @deprecated use {@link #startMonitoring()} and {@link #endMonitoring()} instead
     */
    @Deprecated
    protected void aggregateExecutionTime(long start) {
        //if (!Constants.MEASURE_BY_TASK_HOOKS) {
        monitor.aggregateExecutionTime(start);
        //}
    }
    
    /**
     * Aggregate the execution time and send the recorded value to the
     * monitoring layer. Shall be used only in combination with a corresponding
     * start time measurement. [convenience]
     * 
     * @param start the start execution time
     * @param itemsCount the number of items processed
     * @deprecated use {@link #startMonitoring()} and {@link #endMonitoring()} instead
     */
    @Deprecated
    protected void aggregateExecutionTime(long start, int itemsCount) {
        //if (!Constants.MEASURE_BY_TASK_HOOKS) {
        monitor.aggregateExecutionTime(start, itemsCount);
        //}
    }

    // checkstyle: resume exception type check

    /**
     * Sends an algorithm changed event.
     * 
     * @param algorithm
     *            the new algorithm enacted
     */
    protected void sendAlgorithmChangedEvent(String algorithm) {
        signalConnection.sendAlgorithmChangedEvent(algorithm);
    }

    @Override
    public void onSignal(byte[] data) {
        getLogger().info("onSignal: Listening on the signal! " + pipeline + "/" + name);
        boolean done = ParameterChangeSignal.notify(data, pipeline, name, this);
        if (!done) {
            done = ShutdownSignal.notify(data, pipeline, name, this);
        }
        if (!done) {
            done = LoadSheddingSignal.notify(data, pipeline, name, this);
        }
    }

    @Override
    public void notifyParameterChange(ParameterChangeSignal signal) {
        // empty: keep interface/implementations stable
        getLogger().info("This notifyParameterChange is being called!");
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

    // intentionally final so that subclasses cannot overwrite required shutdown sequence
    @Override
    public final void notifyShutdown(ShutdownSignal signal) {
        portManager.close();
        signalConnection.close();
        prepareShutdown(signal);
        ComponentKeyRegistry.unregister(this);
        monitor.shutdown();
        if (Configuration.getPipelineSignalsQmEvents()) {
            EventManager.unregister(parameterEventHandler);
            EventManager.unregister(shutdownEventHandler);
        }
        EventManager.stop(); // end of pipeline, don't process missing events
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

    @Override
    public final void notifyLoadShedding(LoadSheddingSignal signal) {
        shedder = LoadShedderFactory.createShedder(signal.getShedder());
        shedder.configure(signal);
        EventManager.send(new LoadSheddingChangedMonitoringEvent(pipeline, name, 
            signal.getShedder(), shedder.getDescriptor().getIdentifier(), signal.getCauseMessageId()));
    }

    /**
     * Notifies that monitoring shall be changed.
     * 
     * @param signal the signal describing the change
     */
    public final void notifyMonitoringChange(MonitoringChangeSignal signal) {
        monitor.notifyMonitoringChange(signal);
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
     * @deprecated use {@link #getPipeline()} instead
     */
    @Deprecated
    public String getNamespace() {
        return pipeline;
    }
    
    /**
     * Returns the name of the pipeline this bolt is part of.
     * 
     * @return the name of the pipeline
     */
    public String getPipeline() {
        return pipeline;
    }
    
    /**
     * When this method is called, Storm is requesting that the Spout emit tuples to the 
     * output collector. This method should be non-blocking, so if the Spout has no tuples
     * to emit, this method should return. nextTuple, ack, and fail are all called in a tight
     * loop in a single thread in the spout task. When there are no tuples to emit, it is courteous
     * to have nextTuple sleep for a short amount of time (like a single millisecond)
     * so as not to waste too much CPU. This method calls {@link #startMonitoring()}, {@link #endMonitoring()}
     * and {@link #doNextTuple()}, but may be overriden by subclass which should at least do the 
     * monitoring stuff.
     */
    public void nextTuple() {
        startMonitoring();
        doNextTuple();
        endMonitoring();
    }
    
    /**
     * Determines the next tuple. This method shall call the respective ack/emit methods. When there are no tuples to 
     * emit, it is courteous to have nextTuple sleep for a short amount of time (like a single millisecond)
     * so as not to waste too much CPU. Shall consider {@link #isEnabled(Object)} for load shedding.
     */
    protected void doNextTuple() {
    }

    /**
     * Returns the logger for this bolt.
     * 
     * @return the logger
     */
    protected Logger getLogger() {
        return Logger.getLogger(getClass());
    }

}
