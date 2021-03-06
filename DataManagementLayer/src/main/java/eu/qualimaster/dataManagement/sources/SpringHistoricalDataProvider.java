package eu.qualimaster.dataManagement.sources;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.zip.ZipInputStream;

/**
 * Provides access to the historical financial data archived in the Spring server.
 * Implementations shall be self-contained, not related to infrastructure properties and fully serializable.
 * 
 * @author Andrea Ceroni
 */
public class SpringHistoricalDataProvider implements IHistoricalDataProvider,Serializable
{
	private static final long serialVersionUID = 3348888358474839444L;
	
	/** The url of the SPRING server storing historical data */
	private static final String BASE_URL = "http://84.200.210.254/Qualimaster/history/";
	
	/** The default list of terms to be monitored by default */
	private static final String[] DEFAULT_MONITORED_TERMS = {"NASDAQ-NFLX"};
	
	/** The default list of terms to be looked up for blind prediction */
	private static final String[] DEFAULT_BLIND_TERMS = {"NASDAQ-AAPL","NASDAQ-AMAT","NASDAQ-NFLX","NYSE-CHK","NASDAQ-CSCO","NASDAQ-FB","NASDAQ-GOOGL","NYSE-IBM","NASDAQ-MU","NYSE_MKT-VHC","Toronto-ECA","NYSE-F","NYSE-HPQ","Amsterdam-MT","NASDAQ-SPLS"};
	
	/** The default set of months used for testing. */
	private static final String[] TEST_MONTHS = {"201603","201604","201605"};
	
	/** The location of the historical data used for testing. */
	private static final String TEST_HISTORICAL_DATA_PATH = "./testdata/volumePrediction/historicalData/";
	
	/** Flag indicating whether the instance is running in test mode or not */
	private boolean test = false;
	
    /**
     * Obtains historical data from Spring server (default url)
     * 
     * @param timeHorizon the time horizon in milliseconds into the past
     * @param term the name of the stock for which the historical data is demanded (format: INDEX_NAME�STOCK_NAME)
     * @param target the target file where to store the data
     * @throws IOException in case that obtaining the historical data fails
     */
    public void obtainHistoricalData(long timeHorizon, String term, File target) throws IOException
    {
    	obtainHistoricalData(timeHorizon, term, target, BASE_URL);
    }
    
    /**
     * Obtains historical data from a custom server
     * 
     * @param timeHorizon the time horizon in milliseconds into the past
     * @param term the name of the stock for which the historical data is demanded (format: INDEX_NAME�STOCK_NAME)
     * @param target the target file where to store the data
     * @param server the url of the server where the data has to be downloaded
     * @throws IOException in case that obtaining the historical data fails
     */
    public void obtainHistoricalData(long timeHorizon, String term, File target, String server) throws IOException
    {
    	// derive required months of historical data from the input time horizon
    	ArrayList<String> months = new ArrayList<>();
    	if(this.test) for(int i = 0; i < TEST_MONTHS.length; i++) months.add(TEST_MONTHS[i]);
    	else months = getMonths(Calendar.getInstance(), timeHorizon);
    	
    	// download, uncompress, merge the files containing historical data for each month
    	//if(this.test) getHistoricalDataLocally(term, months, TEST_HISTORICAL_DATA_PATH, target);
    	if(this.test) getHistoricalDataLocally(term, months, server, target);
    	else downloadHistoricalData(term, months, server, target);
    }
    
    private ArrayList<String> getMonths(Calendar reference, Long horizon)
    {
    	ArrayList<String> months = new ArrayList<>();
    	int numMonths = (int) Math.round((double)horizon / (1000l*60l*60l*24l*30l));
    	
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
    
    private void downloadHistoricalData(String term, ArrayList<String> dates, String server, File output) throws IOException
    {
    	// write the unique entry (file) within each zip file into the same output file
    	boolean atLeastOneMonth = false;
    	BufferedWriter writer;
		writer = new BufferedWriter(new FileWriter(output));
    	for(String date : dates){
    		if(downloadHistoricalData(term, date, server, writer)) atLeastOneMonth = true;
    	}
    	writer.close();
    	
    	if(!atLeastOneMonth){
			System.out.println("Impossible to download historical data for term " + term);
    		throw new IOException();
    	}
    }
    
    private void getHistoricalDataLocally(String term, ArrayList<String> dates, String path, File output) throws IOException
    {
    	// write the unique entry (file) into the same output file
    	boolean atLeastOneMonth = false;
    	BufferedWriter writer;
		writer = new BufferedWriter(new FileWriter(output));
    	for(String date : dates){
    		if(getHistoricalDataLocally(term, date, path, writer)) atLeastOneMonth = true;
    	}
    	writer.close();
    	
    	if(!atLeastOneMonth){
			System.out.println("Impossible to download historical data for term " + term);
    		throw new IOException();
    	}
    }
    
    private boolean downloadHistoricalData(String term, String date, String server, BufferedWriter writer) throws IOException
    {
    	try{
			// download the data (zip file) in a ZipInputStream object
	    	URL url = new URL(server + date + "_" + term + "�NoExpiry.zip");
	    	HttpURLConnection con = (HttpURLConnection )url.openConnection();
			
	    	Charset charset = Charset.forName("CP437");
			ZipInputStream zin = new ZipInputStream(con.getInputStream(), charset);
			
			// store the data in the same output file
			storeHistoricalData(zin, writer);
			
			return true;
    	}
    	catch(IOException e){
    		System.out.println("Impossible to download historical data for term " + term + " and date " + date);
    		return false;
    	}
    }
    
    private boolean getHistoricalDataLocally(String term, String date, String path, BufferedWriter writer) throws IOException
    {
    	try{
			String fileName = date + "_" + term + "-NoExpiry.his";
			File folder = new File(path);
			for(File f : folder.listFiles()){
				if(f.getName().compareTo(fileName) == 0){
					storeHistoricalData(f, writer);
					break;
				}
			}
		
			return true;
    	}
    	catch(IOException e){
    		System.out.println("Impossible to download historical data for term " + term + " and date " + date);
    		return false;
    	}
    }
    
    private void storeHistoricalData(ZipInputStream zis, BufferedWriter writer) throws IOException
    {
    	// write the unique entry (file) within each zip file into the same output file
    	try
    	{
    		zis.getNextEntry();
    		for(int c = zis.read(); c != -1; c = zis.read())
    		{
	            writer.write(c);
            }
	        zis.close();
    	}
    	catch(Exception e)
    	{
    		System.out.println("Impossible to write historical data ");
    		e.printStackTrace();
    		throw new IOException();
    	}
    }
    
    private void storeHistoricalData(File input, BufferedWriter writer) throws IOException
    {
    	// write the content of the file into the same output file
    	BufferedReader reader = null;
    	try
    	{
    		reader = new BufferedReader(new FileReader(input));
    		String line = null;
    		while((line = reader.readLine()) != null){
	            writer.write(line);
	            writer.newLine();
            }
	        reader.close();
    	}
    	catch(Exception e)
    	{
    		System.out.println("Impossible to write historical data ");
    		e.printStackTrace();
    		throw new IOException();
    	}
    }
    
    private String dateToString(int year, int month)
    {
    	if(month < 10) return year + "0" + month;
    	else return "" + year + month;
    }

	/**
	 * @return the defaultMonitoredTerms
	 */
	public HashSet<String> getDefaultMonitoredTerms() {
		HashSet<String> monitoredTerms = new HashSet<>();
		for(int i = 0; i < DEFAULT_MONITORED_TERMS.length; i++){
			monitoredTerms.add(DEFAULT_MONITORED_TERMS[i]);
		}
		return monitoredTerms;
	}

	/**
	 * @return the defaultBlindTerms
	 */
	public HashSet<String> getDefaultBlindTerms() {
		HashSet<String> blindTerms = new HashSet<>();
		for(int i = 0; i < DEFAULT_BLIND_TERMS.length; i++){
			blindTerms.add(DEFAULT_BLIND_TERMS[i]);
		}
		return blindTerms;
	}
	
	/**
	 * @return the test
	 */
	public boolean isTest() {
		return test;
	}

	/**
	 * @param test the test to set
	 */
	public void setTest(boolean test) {
		this.test = test;
	}
}
