package eu.qualimaster.dataManagement.sources;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.hbase.util.Bytes;

import eu.qualimaster.dataManagement.DataManager;
import eu.qualimaster.dataManagement.storage.hbase.HBaseStorageSupport;
import eu.qualimaster.dataManagement.strategies.FirstInFirstOutStorageStrategyDescriptor;

/**
 * Provides access to the historical twitter data archived by the Data Management Layer.
 * Implementations shall be self-contained, not related to infrastructure properties and fully serializable.
 * 
 * @author Andrea Ceroni
 */
public class TwitterHistoricalDataProvider implements IHistoricalDataProvider,Serializable
{
	private static final long serialVersionUID = 2128947946366967252L;

	/**
     * Obtains twitter data via the Data Management Layer (data are retrieved from HBase)
     * 
     * @param timeHorizon the time horizon in milliseconds into the past
     * @param term the name of the hashtag for which the historical data is demanded 
     * @param target the target file where to store the data
     * @throws IOException in case that obtaining the historical data fails
     */
    public void obtainHistoricalData(long timeHorizon, String term, File target) throws IOException
    {
    	
    	
    }
    
    private void getDataFromHBaseTable(Long timeHorizon, String term)
    {
    	// Get the HBase table containing data for the input term
    	// TODO which strategy should be used?
    	HBaseStorageSupport table = (HBaseStorageSupport) DataManager.VOLUME_PREDICTION_STORAGE_MANAGER.getTable("", term, null);
    	table.connect();
    	
    	// Get all the keys (dates) of the table and keep only those within the time horizon
    	List<Object> keys = table.getKeys();
    	List<String> validKeys = filterKeys(keys, timeHorizon);
    	
    }
    
    private List<String> filterKeys(List<Object> keys, Long timeHorizon)
    {
    	List<String> filteredKeys = new ArrayList<String>();
    	for(Object o : keys)
    	{
    		byte[] key = (byte[]) o;
    		String keyString = Bytes.toString(key);
    		if(isDateInHorizon(keyString, timeHorizon)) filteredKeys.add(keyString); 
    	}
    	return filteredKeys;
    }
    
    private boolean isDateInHorizon(String date, Long timeHorizon)
    {
    	// check if the date represented by the input string is within the input time horizon
    	return true;
    }
}
