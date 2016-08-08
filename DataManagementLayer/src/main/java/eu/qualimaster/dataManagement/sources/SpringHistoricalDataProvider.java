package eu.qualimaster.dataManagement.sources;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.zip.ZipInputStream;


/**
 * Provides access to the historical financial data archived in the Spring server.
 * Implementations shall be self-contained, not related to infrastructure properties and fully serializable.
 * 
 * @author Andrea Ceroni
 */
public class SpringHistoricalDataProvider implements IHistoricalDataProvider,Serializable
{
	/** The url of the SPRING server storing historical data */
	private static final String BASE_URL = "http://84.200.210.254/Qualimaster/history/";
	
    /**
     * Obtains historical data from Spring server
     * 
     * @param timeHorizon the time horizon in milliseconds into the past
     * @param term the name of the stock for which the historical data is demanded (format: INDEX_NAME·STOCK_NAME)
     * @param target the target file where to store the data
     * @throws IOException in case that obtaining the historical data fails
     */
    public void obtainHistoricalData(long timeHorizon, String term, File target) throws IOException
    {
    	// derive required months of historical data from the input time horizon
    	ArrayList<String> months = getMonths(Calendar.getInstance(), timeHorizon);
    	
    	// download the files containing historical data for each month
    	ArrayList<ZipInputStream> zipFiles = downloadHistoricalData(term, months);
    	
    	// uncompress and merge the zip files within the output file
    	storeHistoricalData(zipFiles, target);
    }
    
    private ArrayList<String> getMonths(Calendar reference, Long horizon)
    {
    	ArrayList<String> months = new ArrayList<>();
    	int numMonths = (int) Math.round((double)horizon / (1000*60*60*24*30));
    	
    	// the other required months in the past
    	int currYear = reference.get(Calendar.YEAR);
    	for(int i = 0; i < numMonths; i++)
    	{
    		int currMonth = reference.get(Calendar.MONTH) + 1;
    		months.add(dateToString(currYear, currMonth));
    		reference.add(Calendar.MONTH, -1);
    	}
    	
    	return months;
    }
    
    private ArrayList<ZipInputStream> downloadHistoricalData(String term, ArrayList<String> dates) throws IOException
    {
    	ArrayList<ZipInputStream> data = new ArrayList<>();
    	for(String date : dates) data.add(downloadHistoricalData(term, date));
    	return data;
    }
    
    private ZipInputStream downloadHistoricalData(String term, String date) throws IOException
    {
		// download the data (zip file) in a ZipInputStream object
    	URL url = new URL(BASE_URL + date + "_" + term + "·NoExpiry.zip");
    	URLConnection urlConnection = url.openConnection();
		ZipInputStream zin = new ZipInputStream(urlConnection.getInputStream());
		return zin;
    }
    
    private void storeHistoricalData(ArrayList<ZipInputStream> inputStreams, File file) throws IOException
    {
    	// write the unique entry (file) within each zip file into the same output file
    	BufferedWriter writer;
    	
    	try
    	{
    		writer = new BufferedWriter(new FileWriter(file));
	    	for(ZipInputStream zis : inputStreams)
	    	{
	    		zis.getNextEntry();
	    		for(int c = zis.read(); c != -1; c = zis.read())
	    		{
		            writer.write(c);
	            }
		        zis.close();
	        }
		    writer.close();
    	}
    	catch(Exception e)
    	{
    		System.out.println("Impossible to write historical data in file " + file.getName());
    		throw new IOException();
    	}
    }
    
    private String dateToString(int year, int month)
    {
    	if(month < 10) return year + "0" + month;
    	else return "" + year + month;
    }
}
