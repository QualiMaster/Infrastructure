package tests.eu.qualimaster.monitoring.genTopo.profiling;

import java.util.Map;
import eu.qualimaster.dataManagement.strategies.*;
import eu.qualimaster.observables.*;
import eu.qualimaster.dataManagement.sources.*;

/**
* Define the data source class(GEN).
**/
public class TestSourceProfiling implements ITestSourceProfiling {

    /**
    * Define the source data input interface.
    **/
    @SuppressWarnings("serial")
    public static class TestSourceProfilingPreprocessedStreamOutput 
        implements ITestSourceProfilingPreprocessedStreamOutput {
        private String symbolId;
        private long timestamp;
        private double value;
        private int volume;
        
        /**
        * Returns the input value for tuple field "symbolId".
        * @return the tuple value
        */
        @Override
        public String getSymbolId() {
            return symbolId;
        }

        /**
        * Sets the output value for tuple field "symbolId".
        * @param symbolId the field value
        */
        @Override
        public void setSymbolId(String symbolId) {
            this.symbolId = symbolId;
        }
        
        /**
        * Returns the input value for tuple field "timestamp".
        * @return the tuple value
        */
        @Override
        public long getTimestamp() {
            return timestamp;
        }

        /**
        * Sets the output value for tuple field "timestamp".
        * @param timestamp the field value
        */
        @Override
        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
        
        /**
        * Returns the input value for tuple field "value".
        * @return the tuple value
        */
        @Override
        public double getValue() {
            return value;
        }

        /**
        * Sets the output value for tuple field "value".
        * @param value the field value
        */
        @Override
        public void setValue(double value) {
            this.value = value;
        }
        
        /**
        * Returns the input value for tuple field "volume".
        * @return the tuple value
        */
        @Override
        public int getVolume() {
            return volume;
        }

        /**
        * Sets the output value for tuple field "volume".
        * @param volume the field value
        */
        @Override
        public void setVolume(int volume) {
            this.volume = volume;
        }
    }
    /**
    * Define the source data input interface.
    **/
    @SuppressWarnings("serial")
    public static class TestSourceProfilingSymbolListOutput implements ITestSourceProfilingSymbolListOutput {
        private java.util.List<String> allSymbols;

        /**
        * Returns the input value for tuple field "allSymbols".
        * @return the tuple value
        */
        @Override
        public java.util.List<String> getAllSymbols() {
            return allSymbols;
        }

        /**
        * Sets the output value for tuple field "allSymbols".
        * @param allSymbols the field value
        */
        @Override
        public void setAllSymbols(java.util.List<String> allSymbols) {
            this.allSymbols = allSymbols;
        }
    }


    /**
    * Returns a specific type of data source.
    * @return TestSourceProfilingPreprocessedStreamOutput the source data
    **/
    @Override
    public TestSourceProfilingPreprocessedStreamOutput getPreprocessedStream() {
        return null;
    }

    /**
    * Returns an aggregation key from the key property of the tuple type for predicting source data. If null, do not 
    * aggregate or predict.
    * 
    * @return an aggregation key
    **/
    @Override
    public String getAggregationKey(ITestSourceProfilingPreprocessedStreamOutput tuple) {
        return null;
    }
    /**
    * Returns a specific type of data source.
    * @return TestSourceProfilingSymbolListOutput the source data
    **/
    @Override
    public TestSourceProfilingSymbolListOutput getSymbolList() {
        return null;
    }

    /**
    * Returns an aggregation key from the key property of the tuple type for predicting source data. If null, do 
    * not aggregate or predict.
    * @return an aggregation key
    **/
    @Override
    public String getAggregationKey(ITestSourceProfilingSymbolListOutput tuple) {
        return null;
    }
    // data source parameters

    /**
     * Sets the data source parameter "dataFile".
     *
     * @param value the new value of the data source parameter
     */
    @Override
    public void setParameterDataFile(String value) {
    }

    /**
     * Sets the data source parameter "hdfsDataFile".
     *
     * @param value the new value of the data source parameter
     */
    @Override
    public void setParameterHdfsDataFile(String value) {
    }

    /**
     * Sets the data source parameter "replaySpeed".
     *
     * @param value the new value of the data source parameter
     */
    @Override
    public void setParameterReplaySpeed(int value) {
    }

    @Override
    public void forceAutoconnect() {}
    @Override
    public void connect() {}

    @Override
    public void disconnect(){}

    @Override
    public void setStrategy(IStorageStrategyDescriptor strategy) {}

    @Override
    public IStorageStrategyDescriptor getStrategy() { 
        return NoStorageStrategyDescriptor.INSTANCE;
    }

    @Override
    public Double getMeasurement(IObservable observable) { 
        return null;
    }

    @Override
    public IHistoricalDataProvider getHistoricalDataProvider() {
        return null;
    }

    @Override
    public Map<String, String> getIdsNamesMap() {
        return null;
    }

    @Override
    public void setDataSourceListener(IDataSourceListener listener) {
        // no mapping, no listener needed
    }

}
