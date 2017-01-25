package tests.eu.qualimaster.monitoring.genTopo.profiling;

import java.io.Serializable;
import eu.qualimaster.dataManagement.sources.IDataSource;

/**
* Define the data source interface(GEN).
**/
public interface ITestSourceProfiling extends IDataSource {

    /**
    * Enforce the source to autoconnect mode and prevent self-connects (profiling).
    **/
    public void forceAutoconnect();

    /**
    * Define the source data input interface.
    **/
    public static interface ITestSourceProfilingPreprocessedStreamOutput extends Serializable {
        /**
         * Returns the input value for tuple field "symbolId".
         * @return the tuple value
         */
        public String getSymbolId();

        /**
         * Sets the output value for tuple field "symbolId".
         * @param symbolId the field value
         */
        public void setSymbolId(String symbolId);
        /**
         * Returns the input value for tuple field "timestamp".
         * @return the tuple value
         */
        public long getTimestamp();

        /**
         * Sets the output value for tuple field "timestamp".
         * @param timestamp the field value
         */
        public void setTimestamp(long timestamp);
        /**
         * Returns the input value for tuple field "value".
         * @return the tuple value
         */
        public double getValue();

        /**
         * Sets the output value for tuple field "value".
         * @param value the field value
         */
        public void setValue(double value);
        /**
         * Returns the input value for tuple field "volume".
         * @return the tuple value
         */
        public int getVolume();

        /**
         * Sets the output value for tuple field "volume".
         * @param volume the field value
         */
        public void setVolume(int volume);
    }
    
    /**
    * Define the source data input interface.
    **/
    public static interface ITestSourceProfilingSymbolListOutput extends Serializable {
        /**
         * Returns the input value for tuple field "allSymbols".
         * @return the tuple value
         */
        public java.util.List<String> getAllSymbols();

        /**
         * Sets the output value for tuple field "allSymbols".
         * @param allSymbols the field value
         */
        public void setAllSymbols(java.util.List<String> allSymbols);
    }


    /**
    * Returns a specific type of data source.
    * @return ITestSourceProfilingPreprocessedStreamOutput the source data
    **/
    public ITestSourceProfilingPreprocessedStreamOutput getPreprocessedStream();

    /**
    * Returns an aggregation key from the key property of the tuple type for predicting source data. If null, 
    * do not aggregate or predict.
    * @param tuple the tuple
    * @return an aggregation key
    **/
    public String getAggregationKey(ITestSourceProfilingPreprocessedStreamOutput tuple);
    
    /**
    * Returns a specific type of data source.
    * @return ITestSourceProfilingSymbolListOutput the source data
    **/
    public ITestSourceProfilingSymbolListOutput getSymbolList();

    /**
    * Returns an aggregation key from the key property of the tuple type for predicting source data. If null, do not 
    * aggregate or predict.
    * @param tuple the tuple
    * @return an aggregation key
    **/
    public String getAggregationKey(ITestSourceProfilingSymbolListOutput tuple);
    // data source parameters

    /**
     * Sets the data source parameter "dataFile".
     *
     * @param value the new value of the data source parameter
     */
    public void setParameterDataFile(String value);

    /**
     * Sets the data source parameter "hdfsDataFile".
     *
     * @param value the new value of the data source parameter
     */
    public void setParameterHdfsDataFile(String value);

    /**
     * Sets the data source parameter "replaySpeed".
     *
     * @param value the new value of the data source parameter
     */
    public void setParameterReplaySpeed(int value);

}
