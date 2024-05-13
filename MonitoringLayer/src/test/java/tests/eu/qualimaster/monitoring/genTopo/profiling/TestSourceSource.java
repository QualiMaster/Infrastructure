package tests.eu.qualimaster.monitoring.genTopo.profiling;

import java.util.*;
import java.io.Serializable;
import org.apache.log4j.Logger;
import backtype.storm.topology.*;
import backtype.storm.task.*;
import backtype.storm.spout.*;
import backtype.storm.tuple.*;
import eu.qualimaster.common.signal.*;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.PipelineOptions;
import eu.qualimaster.pipeline.DefaultModeException;
import eu.qualimaster.pipeline.DefaultModeMonitoringEvent;
import tests.eu.qualimaster.monitoring.genTopo.profiling.ITestSourceProfiling.*;
import eu.qualimaster.monitoring.events.AlgorithmChangedMonitoringEvent;
import eu.qualimaster.monitoring.events.ParameterChangedMonitoringEvent;
import eu.qualimaster.dataManagement.sources.*;
import eu.qualimaster.dataManagement.events.HistoricalDataProviderRegistrationEvent;
import eu.qualimaster.dataManagement.strategies.*;
import eu.qualimaster.dataManagement.DataManager;

/**
* Define the source Spout class(GEN).
**/
@SuppressWarnings({ "rawtypes", "serial" })
public class TestSourceSource extends BaseSignalSourceSpout implements IDataSourceListener {

    private static final Logger LOGGER = Logger.getLogger(TestSourceSource.class);
    private transient SpoutOutputCollector collector;
    private transient ITestSourceProfiling sourceData;

    /**
     * Creates the source.
     * 
     * @param name the name of the source
     * @param namespace the (pipeline) namespace
     */
    public TestSourceSource(String name, String namespace) {
        super(name, namespace, true);
    }


    /**
     * Sends an algorithm change event and considers whether the coordination layer shall be bypassed for direct
     * testing.
     * @param algorithm the new algorithm
     * @param causeMsgId the message id of the causing message (may be empty or null)
     */
    private void sendAlgorithmChangeEvent(String algorithm, String causeMsgId) {
        EventManager.send(new AlgorithmChangedMonitoringEvent(getPipeline(), getName(), algorithm, causeMsgId));
    }


    /**
     * Sends an parameter change event and considers whether the coordination layer shall be bypassed for direct
     * testing.
     * @param parameter the parameter to be changed
     * @param value the new value
     * @param causeMsgId the message id of the causing message (may be empty or null)
     */
    private static void sendParameterChangeEvent(String parameter, Serializable value, String causeMsgId) {
        EventManager.send(new ParameterChangedMonitoringEvent("TestPip", "TestSource", parameter, value, causeMsgId));
    }

    /**
     * Sends an event for registering the historical data provider of a data source.
     * @param source the data source
     */
    private void sendHistoricalDataProviderRegistrationEvent(IDataSource source) {
        EventManager.send(new HistoricalDataProviderRegistrationEvent(getPipeline(), getName(), 
            source.getHistoricalDataProvider(), source.getIdsNamesMap()));
    }

    /**
     * Configures the source monitor.
     */
    @Override // missing in generation
    protected void configure(SourceMonitor monitor) {
        monitor.setAggregationInterval(60000);
        monitor.registerAggregationKeyProvider(
            new AggregationKeyProvider<ITestSourceProfilingPreprocessedStreamOutput>(
                ITestSourceProfilingPreprocessedStreamOutput.class) {

                @Override
                public String getAggregationKey(ITestSourceProfilingPreprocessedStreamOutput tuple) {
                    return sourceData.getAggregationKey(tuple);
                }
            });
        monitor.registerAggregationKeyProvider(new AggregationKeyProvider<ITestSourceProfilingSymbolListOutput>(
            ITestSourceProfilingSymbolListOutput.class) {
            
            @Override
            public String getAggregationKey(ITestSourceProfilingSymbolListOutput tuple) {
                return sourceData.getAggregationKey(tuple);
            }
        });
    }

    @Override
    public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
        super.open(conf, context, collector);
        this.collector = collector;
        try {
            @SuppressWarnings("unchecked")
            Class<? extends ITestSourceProfiling> cls = (Class<? extends ITestSourceProfiling>) Class.forName(
                "tests.eu.qualimaster.monitoring.genTopo.profiling.TestSourceProfilingProfiling");
            boolean autoConnect = "true".equals(conf.get(Constants.CONFIG_KEY_SOURCE_AUTOCONNECT));
            if (autoConnect) {
                sourceData = DataManager.DATA_SOURCE_MANAGER.createDataSource(getPipeline(), cls, 
                    NoStorageStrategyDescriptor.INSTANCE);
                sourceData.forceAutoconnect();
            } else {
                sourceData = cls.newInstance();
            }
            sendHistoricalDataProviderRegistrationEvent(sourceData);
            sendAlgorithmChangeEvent("TestSourceProfilingProfiling", null);
            sourceData.setParameterDataFile(PipelineOptions.getExecutorStringArgument(conf, getName(), "dataFile", ""));
            sourceData.setParameterHdfsDataFile(PipelineOptions.getExecutorStringArgument(
                conf, getName(), "hdfsDataFile", ""));
            if (PipelineOptions.hasExecutorArgument(conf, getName(), "replaySpeed")) {
                sourceData.setParameterReplaySpeed(PipelineOptions.getExecutorIntArgument(
                    conf, getName(), "replaySpeed", 0));
            }
            if (!autoConnect) {
                sourceData.connect();
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
        initMonitor();
        LOGGER.info("The end of the open method.");
    }

    @Override
    protected boolean initMonitorDuringOpen() {
        return false;
    }

    /**
     * Sends an a default mode monitoring event with a DefaultModeException case.
     * @param exceptionCase the DefaultModeException case
     */
    private static void sendDefaultModeMonitoringEvent(DefaultModeException exceptionCase) {
        EventManager.send(new DefaultModeMonitoringEvent("TestPip", "TestSource", exceptionCase));
    }
    @Override
    public void nextTuple() {
        startMonitoring();
        
     // Emitting stream "TestSourceSymbolList".
        ITestSourceProfilingSymbolListOutput dataItemSymbolList 
            = new TestSourceProfiling.TestSourceProfilingSymbolListOutput();
        try {
            dataItemSymbolList = sourceData.getSymbolList();
        } catch (DefaultModeException e) {
            dataItemSymbolList.setAllSymbols(null);
            sendDefaultModeMonitoringEvent(e);
        }
        if (dataItemSymbolList != null) {
            this.collector.emit("TestSourceSymbolList", new Values(dataItemSymbolList));
        }
        
        // Emitting stream "TestSourceStreamPreprocessedStream".
        ITestSourceProfilingPreprocessedStreamOutput dataItemPreprocessedStream 
            = new TestSourceProfiling.TestSourceProfilingPreprocessedStreamOutput();
        try {
            dataItemPreprocessedStream = sourceData.getPreprocessedStream();
        } catch (DefaultModeException e) {
            dataItemPreprocessedStream.setSymbolId("");
            dataItemPreprocessedStream.setTimestamp(0);
            dataItemPreprocessedStream.setValue(0.0);
            dataItemPreprocessedStream.setVolume(0);
            sendDefaultModeMonitoringEvent(e);
        }
        if (dataItemPreprocessedStream != null) {
            this.collector.emit("TestSourcePreprocessedStream", new Values(dataItemPreprocessedStream));
        }

        endMonitoring();
    }

    @Override
    public void notifyAlgorithmChange(AlgorithmChangeSignal signal) {
        sendAlgorithmChangeEvent(signal.getAlgorithm(), signal.getCauseMessageId());
        super.notifyAlgorithmChange(signal);
    }

    @Override
    public void notifyParameterChange(ParameterChangeSignal signal) {
        LOGGER.info("Received the parameter change signal!");
        for (int i = 0; i < signal.getChangeCount(); i++) {
            LOGGER.info("For-loop: Checking each parameter!");
            ParameterChange para = signal.getChange(i);
            switch (para.getName()) {
            case "dataFile" :
                LOGGER.info("Received the parameter dataFile!");
                sourceData.setParameterDataFile(para.getStringValue()); 
                sendParameterChangeEvent("dataFile", para.getStringValue(), signal.getCauseMessageId());
                break;
            case "hdfsDataFile" :
                LOGGER.info("Received the parameter hdfsDataFile!");
                sourceData.setParameterHdfsDataFile(para.getStringValue()); 
                sendParameterChangeEvent("hdfsDataFile", para.getStringValue(), signal.getCauseMessageId());
                break;
            case "replaySpeed" :
                try {
                    LOGGER.info("Received the parameter replaySpeed!");
                    sourceData.setParameterReplaySpeed(para.getIntValue()); 
                    sendParameterChangeEvent("replaySpeed", para.getIntValue(), signal.getCauseMessageId());
                } catch (ValueFormatException e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
            }
        }
    }

    @Override
    public void notifyIdsNamesMapChanged() {
        sendHistoricalDataProviderRegistrationEvent(sourceData);
    }

    @Override
    public void close() {
        super.close();
        sourceData.disconnect();
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declareStream("TestSourcePreprocessedStream", new Fields("TestSourcePreprocessedStreamFields"));
        declarer.declareStream("TestSourceSymbolList", new Fields("TestSourceSymbolListFields"));
    }

    @Override
    protected void prepareShutdown(ShutdownSignal signal) {
        super.prepareShutdown(signal);
        sourceData.disconnect();
    }

}
