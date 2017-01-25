package tests.eu.qualimaster.monitoring.genTopo.profiling;

import java.io.*;
import java.util.Queue;

import javax.annotation.Generated;
import eu.qualimaster.dataManagement.strategies.*;
import eu.qualimaster.dataManagement.serialization.*;
import eu.qualimaster.observables.*;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.EndOfDataEvent;
import eu.qualimaster.dataManagement.sources.GenericMultiSourceHandler;
import eu.qualimaster.dataManagement.sources.ReplayMechanism;
import eu.qualimaster.dataManagement.sources.ReplayMechanism.ProfilingQueueItem;
import eu.qualimaster.dataManagement.sources.replay.LongTimestampParser;
import eu.qualimaster.dataManagement.sources.IHistoricalDataProvider;

/**
* Implements the data source class for profiling (GEN).
**/
@Generated("by QM")
public class TestSourceProfilingProfiling extends TestSourceProfiling {
    private static final int MAXIMUM_DATA_ENTRIES = 400000;
    private GenericMultiSourceHandler handler = new GenericMultiSourceHandler(2);
    private ReplayMechanism replay = new ReplayMechanism(LongTimestampParser.INSTANCE);
    private boolean eodSent = false;
    private boolean isConnected = false;
    
    private transient Queue<ProfilingQueueItem<TestSourceProfilingPreprocessedStreamOutput>> streamProQueue = null;
    private transient Queue<ProfilingQueueItem<TestSourceProfilingSymbolListOutput>> symbolProQueue = null;
    
    /**
    * Provides a serializer for the test data.
    */
    public static class TestSourceProfilingPreprocessedStreamOutputSerializer 
        implements ISerializer<TestSourceProfilingPreprocessedStreamOutput> {

        @Override
        public void serializeTo(TestSourceProfilingPreprocessedStreamOutput object, OutputStream out) 
            throws IOException {
            // no protobuf by now
        }

        @Override
        public TestSourceProfilingPreprocessedStreamOutput deserializeFrom(InputStream in) throws IOException {
            return null; // no protobuf by now
        }

        @Override
        public void serializeTo(TestSourceProfilingPreprocessedStreamOutput object, IDataOutput out) 
            throws IOException {
            out.writeString(object.getSymbolId());
            out.writeLong(object.getTimestamp());
            out.writeDouble(object.getValue());
            out.writeInt(object.getVolume());
        }


        @Override
        public TestSourceProfilingPreprocessedStreamOutput deserializeFrom(IDataInput in) throws IOException {
            TestSourceProfilingPreprocessedStreamOutput result = new TestSourceProfilingPreprocessedStreamOutput();
            result.setSymbolId(in.nextString());
            result.setTimestamp(in.nextLong());
            result.setValue(in.nextDouble());
            result.setVolume(in.nextInt());
            return result;
        }

    }
    
    @Override
    public TestSourceProfilingPreprocessedStreamOutput getPreprocessedStream() {
        TestSourceProfilingPreprocessedStreamOutput result = null;
        if (isConnected) {
            if (!streamProQueue.isEmpty()) {
                try {
                    result = replay.getNextItem(streamProQueue);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }            
            if (streamProQueue.isEmpty() && symbolProQueue.isEmpty()) {
                if (!eodSent) {
                    EventManager.send(new EndOfDataEvent("TestPip", "TestSource"));
                    eodSent = true;
                }
            }             
            
            /*
            if (replay.isEOD()) {
                if (!eodSent) {
                    EventManager.send(new EndOfDataEvent("TestPip", "TestSource"));
                    eodSent = true;
                }
            } else {
                String genericInput = replay.getNext();
                if (null != genericInput) {
                    char separator = replay.getSeparator();
                    result = handler.next("preprocessedStream", TestSourceProfilingPreprocessedStreamOutput.class, 
                        genericInput, separator, false);
                }
            }
            */
        }
        return result;
    }
    
    @Override
    public String getAggregationKey(ITestSourceProfilingPreprocessedStreamOutput tuple) {
        return null;
    }
    
    /**
    * Provides a serializer for the test data.
    */
    public static class TestSourceProfilingSymbolListOutputSerializer 
        implements ISerializer<TestSourceProfilingSymbolListOutput> {

        @Override
        public void serializeTo(TestSourceProfilingSymbolListOutput object, OutputStream out) throws IOException {
            // no protobuf by now
        }

        @Override
        public TestSourceProfilingSymbolListOutput deserializeFrom(InputStream in) throws IOException {
            return null; // no protobuf by now
        }

        @Override
        public void serializeTo(TestSourceProfilingSymbolListOutput object, IDataOutput out) throws IOException {
            SerializerRegistry.getListSerializerSafe("STRINGLIST", String.class).serializeTo(
                object.getAllSymbols(), out);
        }


        @Override
        public TestSourceProfilingSymbolListOutput deserializeFrom(IDataInput in) throws IOException {
            TestSourceProfilingSymbolListOutput result = new TestSourceProfilingSymbolListOutput();
            result.setAllSymbols(SerializerRegistry.getListSerializerSafe(
                "STRINGLIST", String.class).deserializeFrom(in));
            return result;
        }

    }

    @Override
    public TestSourceProfilingSymbolListOutput getSymbolList() {
        TestSourceProfilingSymbolListOutput result = null;
        if (isConnected) {
            if (!symbolProQueue.isEmpty()) {
                try {
                    result = replay.getNextItem(symbolProQueue);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (streamProQueue.isEmpty() && symbolProQueue.isEmpty()) {
                if (!eodSent) {
                    EventManager.send(new EndOfDataEvent("TestPip", "TestSource"));
                    eodSent = true;
                }
            } 
            /*
            if (replay.isEOD()) {
                if (!eodSent) {
                    EventManager.send(new EndOfDataEvent("TestPip", "TestSource"));
                    eodSent = true;
                }
            } else {
                String genericInput = replay.getNext();
                if (null != genericInput) {
                    char separator = replay.getSeparator();
                    result = handler.next("symbolList", TestSourceProfilingSymbolListOutput.class, 
                        genericInput, separator, false);
                }
            }
            */
        }
        return result;
    }
    @Override
    public String getAggregationKey(ITestSourceProfilingSymbolListOutput tuple) {
        return null;
    }
    // data source parameters

    @Override
    public void setParameterDataFile(String value) {
        replay.setParameterDataFile(value);
    }

    @Override
    public void setParameterHdfsDataFile(String value) {
        replay.setParameterHdfsDataFile(value);
    }

    @Override
    public void setParameterReplaySpeed(int value) {
        replay.setParameterReplaySpeed(value);
    }

    @Override
    public void forceAutoconnect() {
        replay.forceAutoconnect();
    }

    @Override
    public void connect() {
        isConnected = true;
        SerializerRegistry.register("STRINGLIST", eu.qualimaster.base.serializer.StringListSerializer.class);
        SerializerRegistry.register(TestSourceProfilingPreprocessedStreamOutput.class, 
            TestSourceProfilingPreprocessedStreamOutputSerializer.class);
        SerializerRegistry.register(TestSourceProfilingSymbolListOutput.class, 
            TestSourceProfilingSymbolListOutputSerializer.class);
        replay.connect();
        //read profiling data in advance
        try {
            symbolProQueue = replay.readProfilingData("symbolList", TestSourceProfilingSymbolListOutput.class, 
                    handler, 1);
            streamProQueue = replay.readProfilingData("preprocessedStream", 
                    TestSourceProfilingPreprocessedStreamOutput.class, handler, MAXIMUM_DATA_ENTRIES);
            System.err.println("symbolProQueue: " + symbolProQueue.size() + ", streamProQueue: " 
                    + streamProQueue.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void disconnect() {
        isConnected = false;
        replay.disconnect();
        SerializerRegistry.unregister(TestSourceProfilingPreprocessedStreamOutput.class);
        SerializerRegistry.unregister(TestSourceProfilingSymbolListOutput.class);
    }
    @Override
    public void setStrategy(IStorageStrategyDescriptor strategy) {
        replay.setStrategy(strategy);
    }
    @Override
    public IStorageStrategyDescriptor getStrategy() {
        return replay.getStrategy();
    }
    @Override
    public Double getMeasurement(IObservable observable) {
        return replay.getMeasurement(observable);
    }

    @Override
    public IHistoricalDataProvider getHistoricalDataProvider() {
        return replay.getHistoricalDataProvider();
    }
}
