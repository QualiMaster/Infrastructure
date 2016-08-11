package eu.qualimaster.dataManagement.sources;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.util.Time;

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
	private static final String DATE_FORMAT = "MM/DD/YYYY,hh:mm:ss";
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
    	// get data using HBase
    	List<String> data = getDataFromHBaseTable(timeHorizon, term);
    	
    	// store data in the output file
    	storeData(data, target);
    }
    
    private List<String> getDataFromHBaseTable(Long timeHorizon, String term)
    {
    	// Get the HBase table containing data for the input term
    	// TODO which strategy should be used?
    	HBaseStorageSupport table = (HBaseStorageSupport) DataManager.VOLUME_PREDICTION_STORAGE_MANAGER.getTable("", term, null);
    	table.connect();
    	
    	// Get all the keys (dates) of the table and keep only those within the time horizon
    	List<Object> keys = table.getKeys();
    	List<String> validKeys = filterKeys(keys, System.currentTimeMillis() - timeHorizon);
    	
    	// Get values for the remaining keys and put both in the same string
    	List<String> data = new ArrayList<>();
    	for(String key : validKeys) data.add(makeDataLine(key, table));
    	table.disconnect();
    	
    	return data;
    }
    
    private String makeDataLine(String key, HBaseStorageSupport table)
    {
    	String value = (String) table.get(key);
    	return key + "," + value;
    }
    
    private List<String> filterKeys(List<Object> keys, Long minTime)
    {
    	List<String> filteredKeys = new ArrayList<String>();
    	for(Object o : keys)
    	{
    		byte[] key = (byte[]) o;
    		String keyString = Bytes.toString(key);
    		if(isDateInHorizon(keyString, minTime)) filteredKeys.add(keyString); 
    	}
    	return filteredKeys;
    }
    
    private boolean isDateInHorizon(String dateStr, Long minTime)
    {
    	// check if the date represented by the input string is within the input time horizon
    	DateFormat format = new SimpleDateFormat(DATE_FORMAT);
    	try {
			Date date = format.parse(dateStr);
			if(date.getTime() > minTime) return true;
			else return false;
		}
    	catch (ParseException e) {
			e.printStackTrace();
			return false;
		}
    }
    
    private void storeData(List<String> data, File outputFile) throws IOException{
    	BufferedWriter writer = null;
		writer = new BufferedWriter(new FileWriter(outputFile));
		for(String s : data){
			writer.write(s);
			writer.newLine();
		}
		writer.close();
    }
}
