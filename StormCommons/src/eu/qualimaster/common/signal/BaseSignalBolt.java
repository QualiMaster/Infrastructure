package eu.qualimaster.common.signal;

import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.storm.curator.framework.CuratorFramework;
import org.apache.storm.curator.framework.state.ConnectionState;
import org.apache.storm.curator.framework.state.ConnectionStateListener;

import eu.qualimaster.Configuration;
import eu.qualimaster.common.monitoring.MonitoringPluginRegistry;
import eu.qualimaster.common.shedding.LoadShedder;
import eu.qualimaster.common.shedding.LoadShedderFactory;
import eu.qualimaster.common.shedding.NoShedder;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.monitoring.events.ComponentKeyRegistry;
import eu.qualimaster.monitoring.events.LoadSheddingChangedMonitoringEvent;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;

/**
 * Extends the basic Storm Bolt by signalling capabilities.
 * 
 * @author Cui Qin
 * @author Holger Eichelberger
 */
@SuppressWarnings("serial")
public abstract class BaseSignalBolt extends BaseRichBolt implements SignalListener, IAlgorithmChangeListener, 
    IParameterChangeListener, IShutdownListener, ILoadSheddingListener, IMonitoringChangeListener {

    private String name;
    private String pipeline;
    private boolean sendRegular;
    private LoadShedder<?> shedder = NoShedder.INSTANCE;
    private String interconnPorts;
    private transient StormSignalConnection signalConnection;
    private transient AlgorithmChangeEventHandler algorithmEventHandler;
    private transient ParameterChangeEventHandler parameterEventHandler;
    private transient ShutdownEventHandler shutdownEventHandler;
    private transient Monitor monitor;
    private transient PortManager portManager;
    private transient boolean signalConnInitialized;
    private transient boolean initMonitorOnSignalConnInit;

    /**
     * Creates a base signal Bolt with no regular event sending.
     * 
     * @param name the name of the bolt
     * @param pipeline the name of the containing pipeline
     */
    public BaseSignalBolt(String name, String pipeline) {
        this(name, pipeline, false);
    }
    
    /**
     * Creates a base signal Bolt.
     * 
     * @param name the name of the bolt
     * @param pipeline the name of the containing pipeline
     * @param sendRegular whether this monitor shall care for sending regular events (<code>true</code>) or 
     *     not (<code>false</code>, for thrift-based monitoring)
     */
    public BaseSignalBolt(String name, String pipeline, boolean sendRegular) {
        this.name = name;
        this.pipeline = pipeline;
        this.sendRegular = sendRegular;
    }
    
    // checkstyle: stop exception type check

    @Override
    @SuppressWarnings("rawtypes")
    public void prepare(Map conf, TopologyContext context, OutputCollector collector) {
        getLogger().info("Prepare--basesignalbolt.... " + pipeline + "/" + this.name);
        StormSignalConnection.configureEventBus(conf);
        interconnPorts = getInterconnPorts(conf);
        if (conf.containsKey(Constants.CONFIG_KEY_SUBPIPELINE_NAME)) {
            pipeline = (String) conf.get(Constants.CONFIG_KEY_SUBPIPELINE_NAME);
        }
        monitor = createMonitor(pipeline, name, true, context);
        EventManager.asyncSend(new ConnectTaskMonitoringEvent(pipeline, name, monitor.getComponentKey()));
        if (initMonitorDuringPrepare()) {
            monitor.init(sendRegular);
        }
        if (Constants.MEASURE_BY_TASK_HOOKS) {
            context.addTaskHook(monitor);
        }
        try {
            signalConnection = new StormSignalConnection(this.name, this, pipeline, conf);
            if (Configuration.getPipelineSignalsQmEvents()) {
                algorithmEventHandler = AlgorithmChangeEventHandler.createAndRegister(this, pipeline, name);
                parameterEventHandler = ParameterChangeEventHandler.createAndRegister(this, pipeline, name);
                shutdownEventHandler = ShutdownEventHandler.createAndRegister(this, pipeline, name);
                signalConnInitialized = true;
            } else {
                signalConnection.init(new ConnectionStateListener() {
                    
                    @Override
                    public void stateChanged(CuratorFramework client, ConnectionState state) {
                        if (ConnectionState.CONNECTED == state) {
                            getLogger().info("Curator state changed " + state + " " + pipeline + "/" + name + " "
                                + " sigConInit " + signalConnInitialized + " initMonOnSigConn " 
                                + initMonitorOnSignalConnInit);
                            signalConnInitialized = true;
                            if (initMonitorOnSignalConnInit) {
                                monitor.init(sendRegular);                            
                            }
                        }
                    }
                });
                portManager = createPortManager(signalConnection, interconnPorts);
            }
        } catch (Exception e) {
            getLogger().error("Error SignalConnection:" + e.getMessage(), e);
        }
        ComponentKeyRegistry.register(pipeline, this, monitor.getComponentKey());
        getLogger().info("Prepared--basesignalbolt.... " + pipeline + "/" + this.name);
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
        if (null != signalConnection) {
            signalConnection.send(toPath, signal);
        } else {
            getLogger().error("Deprecated signal sending not supported by signal mechanism.");
        }
    }

    // checkstyle: resume exception type check

    /**
     * Creates the port manager and considers pipeline interconnection ports.
     * 
     * @param signalConnection the signal connection
     * @param interconnPorts the pipeline interconnection ports.
     * @return the port manager instance
     */
    static PortManager createPortManager(StormSignalConnection signalConnection, String interconnPorts) {
        return new PortManager(signalConnection.getClient(), PortManager.createPortRangeQuietly(interconnPorts));
    }
    
    /**
     * Returns the pipeline interconnection ports.
     * 
     * @param conf the storm configuration
     * @return the interconnection ports if configured
     */
    @SuppressWarnings("rawtypes")
    static String getInterconnPorts(Map conf) {
        return String.valueOf(conf.get(Configuration.PIPELINE_INTERCONN_PORTS));
    }
    
    /**
     * Creates the monitor instance.
     * 
     * @param namespace the namespace (pipeline name)
     * @param name the element name
     * @param includeItems whether the send items shall also be included
     * @param context the topology context for creating the component id
     * @return the monitor instance (must not be <b>null</b>)
     */
    protected Monitor createMonitor(String namespace, String name, boolean includeItems, TopologyContext context) {
        return new Monitor(namespace, name, true, context);
    }
    
    /**
     * Initialize monitoring during {@link #prepare(Map, TopologyContext, OutputCollector)}. This method is intended
     * to be overridden if the subclass takes over the responsibility and must call {@link #initMonitor()} on its own.
     * Otherwise data processing may not start.
     * 
     * @return <code>true</code> for starting the monitor during open, <code>false</code> else 
     */
    protected boolean initMonitorDuringPrepare() {
        return true;
    }
    
    /**
     * Initializes monitoring for this node.
     */
    protected final void initMonitor() {
        getLogger().info("Init monitor " + pipeline + "/" + name + " sigConInit " + signalConnInitialized
            + " initMonOnSigConn " + initMonitorOnSignalConnInit);
        if (signalConnInitialized) {
            monitor.init(sendRegular);
        } else {
            initMonitorOnSignalConnInit = true;
        }
    }
    
    /**
     * Returns the port manager.
     * 
     * @return the port manager
     */
    protected PortManager getPortManager() {
        if (null == portManager) { // create on demand for using QM event signals
            portManager = createPortManager(signalConnection, interconnPorts);
        }
        return portManager;
    }
    
    /**
     * Starts monitoring for an execution method.
     */
    protected void startMonitoring() {
        monitor.startMonitoring(); // else done by TaskHook
        MonitoringPluginRegistry.startMonitoring();
    }

    /**
     * Counts emitting for a sink execution method.
     * 
     * @param tuple the tuple emitted
     */
    protected void emitted(Object tuple) {
        if (Constants.MEASURE_BY_TASK_HOOKS) {
            monitor.emitted(tuple);
        } else {
            MonitoringPluginRegistry.emitted(tuple);
        }
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
        monitor.endMonitoring(); // else done by TaskHook
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

    /**
     * Sends a signal via the signal connection of this bolt.
     * 
     * @param signal the signal to be sent
     * @throws SignalException in case that the execution / signal sending fails
     */
    protected void sendSignal(TopologySignal signal) throws SignalException {
        signal.sendSignal(signalConnection);
    }

    /**
     * Creates and installs a worker-wide signal handler to enable pipeline lifecycles. Handle with care, requires
     * implicitly a forward event handler thread / connection with the infrastructure.
     */
    protected void installNamespaceLifecycleSignalHandler() {
        SignalNamespaceLifecycleEventHandler.registerHandler();
    }
    
    /**
     * Sends an algorithm changed event.
     * 
     * @param algorithm the new algorithm enacted
     */
    protected void sendAlgorithmChangedEvent(String algorithm) {
        signalConnection.sendAlgorithmChangedEvent(algorithm); // goes anyway over QMEvents
    }

    @Override
    public void onSignal(byte[] data) {
        getLogger().info("onSignal: Listening on the signal! " + pipeline + "/" + name);
        boolean done = AlgorithmChangeSignal.notify(data, pipeline, name, this);
        if (!done) {
            done = ParameterChangeSignal.notify(data, pipeline, name, this);
        }
        if (!done) {
            done = ShutdownSignal.notify(data, pipeline, name, this);
        }
        if (!done) {
            done = LoadSheddingSignal.notify(data, pipeline, name, this);
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
        if (null != portManager) {
            portManager.close();
        }
        signalConnection.close();
        prepareShutdown(signal);
        ComponentKeyRegistry.unregister(this);
        monitor.shutdown();
        SignalNamespaceLifecycleEventHandler.unregisterHandler();
        if (Configuration.getPipelineSignalsQmEvents()) {
            EventManager.unregister(algorithmEventHandler);
            EventManager.unregister(parameterEventHandler);
            EventManager.unregister(shutdownEventHandler);
        }
        EventManager.stop(); // end of pipeline, don't process missing events
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
     * Process a single tuple of input. Considers {@link #isEnabled(Object)} for 
     * load shedding, calls {@link #startMonitoring()} and {@link #endMonitoring()}. Delegates
     * to {@link #doExecute(Tuple)} for the real work. Subclasses may override this method
     * but shall consider load shedding and monitoring (or just do their work in {@link #doExecute(Tuple)}.
     * 
     * @param input The input tuple to be processed.
     */
    @Override
    public void execute(Tuple input) {
        if (isEnabled(input)) {
            startMonitoring();
            doExecute(input);
            endMonitoring();
        }
    }

    /**
     * Process a single not load shedded tuple of input under monitoring. Pipeline sinks shall call 
     * {@link #emitted(Object)} for all data passed finally to the sink implementation.
     * 
     * @param input The input tuple to be processed.
     */
    protected void doExecute(Tuple input) {
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
     * Returns the logger for this bolt.
     * 
     * @return the logger
     */
    protected Logger getLogger() {
        return Logger.getLogger(getClass());
    }

}
