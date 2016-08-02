package tests.eu.qualimaster.storm;

import java.util.Map;

import eu.qualimaster.common.signal.BaseSignalSourceSpout;
import eu.qualimaster.common.signal.ParameterChangeSignal;
import eu.qualimaster.common.signal.ShutdownSignal;
import eu.qualimaster.dataManagement.DataManager;
import eu.qualimaster.dataManagement.strategies.NoStorageStrategyDescriptor;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.monitoring.events.AlgorithmChangedMonitoringEvent;
import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

/**
 * An simple reusable spout.
 * 
 * @param <S> the source type
 * @author Holger Eichelberger
 */
@SuppressWarnings("serial")
public class Source<S extends ISrc> extends BaseSignalSourceSpout {

    private static SignalCollector signals = new SignalCollector(Naming.LOG_SOURCE);
    private Class<S> srcClass;
    private transient SpoutOutputCollector collector;
    private transient S source;
    
    /**
     * Creates a source instance.
     * 
     * @param srcClass the source class
     * @param pipeline the pipeline name
     */
    public Source(Class<S> srcClass, String pipeline) {
        super(Naming.NODE_SOURCE, pipeline);
        this.srcClass = srcClass;
    }
    
    /**
     * Returns the source. Only valid after {@link #open(Map, TopologyContext, SpoutOutputCollector)}.
     * 
     * @return the source, may be <b>null</b>
     */
    protected S getSource() {
        return source;
    }
    
    @SuppressWarnings("rawtypes")
    @Override
    public void open(Map stormConf, TopologyContext context, SpoutOutputCollector collector) {
        super.open(stormConf, context, collector);
        this.collector = collector;
        this.source = DataManager.DATA_SOURCE_MANAGER.createDataSource(getNamespace(), srcClass, 
            NoStorageStrategyDescriptor.INSTANCE);
        initializeParams(stormConf, this.source);
        EventManager.send(new AlgorithmChangedMonitoringEvent(getNamespace(), Naming.NODE_SOURCE, "source"));
        if (Naming.defaultInitializeAlgorithms(stormConf)) {
            // for coordination level tests, there is no monitoring layer to support auto-connect
            source.connect(); 
        }
    }

    /**
     * Initializes the parameters.
     * 
     * @param stormConf the storm configuration map
     * @param source the source instance
     */
    @SuppressWarnings("rawtypes")
    protected void initializeParams(Map stormConf, S source) {
    }

    @Override
    public void nextTuple() {
        startMonitoring();
        Integer value = source.getData();
        if (null != value) {
            Values values = new Values(value);
            this.collector.emit(values);
            endMonitoring();
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("number"));
    }

    @Override
    public void notifyParameterChange(ParameterChangeSignal signal) {
        signals.notifyParameterChange(signal);
    }
    
    @Override
    protected void prepareShutdown(ShutdownSignal signal) {
        signals.notifyShutdown(signal);
    }

    // testing
    
    /**
     * Returns the signal collector.
     * 
     * @return the signal collector
     */
    public SignalCollector getSignals() {
        return signals;
    }

    @Override
    public void close() {
        if (null != source) {
            // for coordination level tests, there is no monitoring layer to support auto-disconnect
            source.disconnect(); 
        }
        super.close();
    }
    
}
