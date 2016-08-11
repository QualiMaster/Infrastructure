package eu.qualimaster.monitoring.volumePrediction;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * Implementation of one of the types of volume prediction: the one based on historical values and unaware of recent values
 * 
 * @author  Andrea Ceroni
 */
public class BlindPrediction {

	/** The name of the term the model refers to (can be either the name of a stock or an hashtag). */
	private String source;
	
	/** The set of average volume values, at each minute of the day, used to estimate the volume of a source without knowing its recent values. */
	private HashMap<String,Double> historicalVolumes;
	
	/** Format of the date field */
	private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
	
	/**
	 * Constructor initializing the model given the available historical data
	 * @param source The name of the source the model makes predictions for
	 * @param dataPath The path to the data used for building the model
	 */
	public BlindPrediction(String source, File dataFile)
	{
		this.source = source;
		this.historicalVolumes = new HashMap<>();
		trainModel(dataFile);
	}
	
	/**
	 * Estimates the volume at the current time (minute) of the day by computing the average over
	 * the volumes at the same time of the day in different past days. This can be used without
	 * knowing the sequence of recent volumes (e.g. to estimate the volume introduced by crawling
	 * a stock before actually doing it).
	 * 
	 * @return The estimated volume at the current time of the day (-1 if no historical data is available)
	 */
	public double predictBlindly()
	{
		// check if there are data
		if(this.historicalVolumes == null || this.historicalVolumes.isEmpty())
		{
			System.out.println("Not historical data available to make predictions.");
			return -1;
		}
		
		// get the current timestamp
		DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
		Date date = new Date();
		String time = dateFormat.format(date).split("'T'")[1];
		
		// fetch the historical average value at the required time of the day
		if(this.getHistoricalVolumes().containsKey(time)) return this.getHistoricalVolumes().get(time);
		else return getNeighborValue(time);
	}
	
	private void trainModel(File dataFile)
	{
		if(dataFile == null) this.historicalVolumes = null;
		else
		{
			TreeMap<String,Long> trainingData = DataUtils.readData(dataFile);
			this.historicalVolumes = computeAverageVolumes(trainingData);
		}
	}
	
	private HashMap<String,Double> computeAverageVolumes(TreeMap<String,Long> map)
	{
		HashMap<String,Double> outputMap = new HashMap<String, Double>();
		HashMap<String, ArrayList<Long>> mapByTime = new HashMap<String, ArrayList<Long>>();
		
		for(String k : map.keySet())
		{
			String time = k.split("'T'")[1];
			if(mapByTime.containsKey(time)) mapByTime.get(time).add(map.get(k));
			else
			{
				ArrayList<Long> values = new ArrayList<Long>();
				values.add(map.get(k));
				mapByTime.put(time, values);
			}
		}
		
		// compute average values for each entry
		for(String k : mapByTime.keySet())
		{
			ArrayList<Long> values = mapByTime.get(k);
			double sum = 0;
			for(Long l : values) sum += l;
			double avg = sum / values.size();
			outputMap.put(k, avg);
		}
		
		return outputMap;
	}
	
	private double getNeighborValue(String time)
	{
		String[] fields = time.split(":");
		int hour = Integer.valueOf(fields[0]);
		int minutes = Integer.valueOf(fields[1]);
		int seconds = Integer.valueOf(fields[2]);
		
		Calendar prevCalendar = Calendar.getInstance();
		Calendar succCalendar = Calendar.getInstance();
		prevCalendar.set(Calendar.HOUR_OF_DAY, hour);
		prevCalendar.set(Calendar.MINUTE, minutes);
		prevCalendar.set(Calendar.SECOND, seconds);
		succCalendar.set(Calendar.HOUR_OF_DAY, hour);
		succCalendar.set(Calendar.MINUTE, minutes);
		succCalendar.set(Calendar.SECOND, seconds);
		
		double neighborValue = -1;
		while(neighborValue == -1)
		{
			prevCalendar.add(Calendar.MINUTE, -1);
			succCalendar.add(Calendar.MINUTE, 1);
			
			int prevHour = prevCalendar.get(Calendar.HOUR_OF_DAY);
			int prevMinute = prevCalendar.get(Calendar.MINUTE);
			int prevSecond = prevCalendar.get(Calendar.SECOND);
			int succHour = succCalendar.get(Calendar.HOUR_OF_DAY);
			int succMinute = succCalendar.get(Calendar.MINUTE);
			int succSecond = succCalendar.get(Calendar.SECOND);
			
			String prevTime = "";
			if(prevHour < 10) prevTime += "0" + prevHour + ":";
			else prevTime += prevHour + ":";
			if(prevMinute < 10) prevTime += "0" + prevMinute + ":";
			else prevTime += prevMinute + ":";
			if(prevSecond < 10) prevTime += "0" + prevSecond;
			else prevTime += prevSecond;
			
			String succTime = "";
			if(succHour < 10) succTime += "0" + succHour + ":";
			else succTime += succHour + ":";
			if(succMinute < 10) succTime += "0" + succMinute + ":";
			else succTime += succMinute + ":";
			if(succSecond < 10) succTime += "0" + succSecond;
			else succTime += succSecond;
			
			if(this.historicalVolumes.containsKey(prevTime)) neighborValue = this.historicalVolumes.get(prevTime);
			if(this.historicalVolumes.containsKey(succTime)) neighborValue = this.historicalVolumes.get(succTime);
		}
		
		return neighborValue;
	}

	/**
	 * @return the source
	 */
	public String getSource() {
		return source;
	}

	/**
	 * @param source the source to set
	 */
	public void setSource(String source) {
		this.source = source;
	}

	/**
	 * @return the historicalVolumes
	 */
	public HashMap<String,Double> getHistoricalVolumes() {
		return historicalVolumes;
	}

	/**
	 * @param historicalVolumes the historicalVolumes to set
	 */
	public void setHistoricalVolumes(HashMap<String,Double> historicalVolumes) {
		this.historicalVolumes = historicalVolumes;
	}
}
