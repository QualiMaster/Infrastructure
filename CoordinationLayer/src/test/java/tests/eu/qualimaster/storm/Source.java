package tests.eu.qualimaster.storm;

import java.util.Map;

import eu.qualimaster.common.signal.AggregationKeyProvider;
import eu.qualimaster.common.signal.BaseSignalSourceSpout;
import eu.qualimaster.common.signal.ParameterChangeSignal;
import eu.qualimaster.common.signal.ShutdownSignal;
import eu.qualimaster.common.signal.SourceMonitor;
import eu.qualimaster.dataManagement.DataManager;
import eu.qualimaster.dataManagement.strategies.NoStorageStrategyDescriptor;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.monitoring.events.AlgorithmChangedMonitoringEvent;
import eu.qualimaster.reflection.ReflectionHelper;
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
        if (Naming.defaultInitializeAlgorithms(stormConf)) {
            try {
                source = ReflectionHelper.createInstance(srcClass);
                EventManager.send(new AlgorithmChangedMonitoringEvent(getPipeline(), Naming.NODE_SOURCE, "source"));
                // for coordination level tests, there is no monitoring layer to support auto-connect
                source.connect(); 
            } catch (IllegalAccessException e) {
            } catch (InstantiationException e) {
            }
        } else {
            source = DataManager.DATA_SOURCE_MANAGER.createDataSource(getPipeline(), srcClass, 
                NoStorageStrategyDescriptor.INSTANCE);
            if (!DataManager.isStarted()) { // DML workaround
                source.connect();
            }
        }
        initializeParams(stormConf, this.source);
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
        if (null != value && isEnabled(value)) {
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
    
    @Override
    public void configure(SourceMonitor monitor) {
        monitor.setAggregationInterval(1000);
        // go for direct superclass for testing
        monitor.registerAggregationKeyProvider(new AggregationKeyProvider<Number>(Number.class) {

            @Override
            public String getAggregationKey(Number tuple) {
                return String.valueOf(tuple.intValue() % 5); // % 5 force some source volume aggregation
            }
        });
    }
    
}
