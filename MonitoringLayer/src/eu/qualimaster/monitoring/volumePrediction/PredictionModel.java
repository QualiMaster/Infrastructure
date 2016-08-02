package eu.qualimaster.monitoring.volumePrediction;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import weka.classifiers.evaluation.NumericPrediction;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.timeseries.WekaForecaster;
import weka.classifiers.timeseries.core.TSLagMaker;
import weka.classifiers.timeseries.core.TSLagMaker.Periodicity;
import weka.classifiers.timeseries.core.TSLagMaker.PeriodicityHandler;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Implementation of the two types of volume prediction: the one based on recent values and the one based on historical values.
 * 
 * @author  Andrea Ceroni
 */
public class PredictionModel {

	/** The name of the term the model refers to (can be either the name of a stock or an hashtag). */
	private String source;
	
	/** The window of recent volume values, used to predict the volume at the next time point. */
	private Instances recentVolumes;
	
	/** The model to make predictions based on recent volume values. */
	private WekaForecaster forecaster;
	
	/** The set of average volume values, at each minute of the day, used to estimate the volume of a source without knowing its recent values. */
	private HashMap<String,Double> historicalVolumes;
	
	/** The number of recent time points to be considered in the model. */
	private static final int NUM_RECENT_VOLUMES = 12;
	
	/** The number of month of data to consider when training the model. */
	private static final int NUM_MONTHS = 4;
	
	/** Name of the volume field */
	private static final String VOLUME_FIELD = "volume";
	
	/** Name of the date field */
	private static final String DATE_FIELD = "date";
	
	/** Format of the date field */
	private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
	
	/**
	 * Constructor initializing (training) the model given the available historical data
	 * @param source The name of the source the model makes predictions for
	 * @param dataPath The path to the data used for training the model
	 */
	public PredictionModel(String source, String dataPath){
		this.source = source;
		this.recentVolumes = createDataset();
		this.forecaster = new WekaForecaster();
		this.historicalVolumes = new HashMap<>();
		trainModels(dataPath);
	}
	
	/**
	 * Predicts the volume during the next time step (1 minute) given the recent values.
	 * It requires at least 12 previous points to make the prediction. If not enough
	 * recent points are available, the method returns a negative integer indicating the
	 * number of steps that still have to be waited before being able to make predictions.
	 * 
	 * @return The predicted volume within the next time step.
	 */
	public double predict()
	{
		try
		{
			// if there are not enough values in the recent history, return a negative value indicating the steps to wait
			if(this.forecaster.getTSLagMaker().getMaxLag() > this.recentVolumes.size())
			{
				System.out.println("Not enough recent values to make predictions.");
				return this.recentVolumes.size() - this.forecaster.getTSLagMaker().getMaxLag();
			}
			
			// prime the forecaster with enough recent historical data to cover up to the maximum lag
			this.forecaster.primeForecaster(this.recentVolumes);
	
			// forecast the desired number of data points
			List<List<NumericPrediction>> wekaForecast = this.forecaster.forecast(1, System.out);
			double forecast = wekaForecast.get(0).get(0).predicted();
			
			return forecast;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return -1;
		}
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
		if(this.historicalVolumes.isEmpty())
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
	
	/**
	 * Appends the input volume (assumed to be the new observed one) to the recent volumes, which are used to make predictions.
	 * If the size of the recent volumes exceeds the maximum one, the oldest volume is removed to make room for the new one.
	 * 
	 * @param time The timestamp when the volume was observed.
	 * @param observation The observed volume.
	 */
	public void updateRecentVolumes(String time, Long observation)
	{
		try
		{
			// Create the instance (according to the structure of the recent volumes) and add it to the recent volumes
			Instance instance = dataToInstance(time, observation, this.recentVolumes);
			if(this.recentVolumes.size() > NUM_RECENT_VOLUMES) this.recentVolumes.remove(0);
			this.recentVolumes.add(instance);
		}
		catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void trainModels(String dataPath)
	{
		HashMap<String,Long> trainingData = readDataFromPath(dataPath, this.source);
		this.forecaster = trainForecaster(trainingData);
		this.historicalVolumes = computeAverageVolumes(trainingData);
	}
	
	private HashMap<String,Long> readDataFromPath(String dataPath, String sourceName)
	{
		// placeholder for the real call to the source interface
		return null;
	}
	
	private WekaForecaster trainForecaster(HashMap<String,Long> data)
	{
		try
		{
			// data to weka instances
			Instances instances = dataToInstances(data);
			
			// new forecaster
		    WekaForecaster forecaster = new WekaForecaster();
		     
		    // set target and date fields
		    forecaster.setFieldsToForecast(VOLUME_FIELD);
		    forecaster.getTSLagMaker().setTimeStampField(DATE_FIELD);
	
		    // set the underlying classifier
		    forecaster.setBaseForecaster(new LinearRegression());
		    
		    // detect the periodicity automatically (similarly to the weka gui)
		    detectPeriodicity(forecaster, instances, DATE_FIELD);
		    
		    //forecaster.getTSLagMaker().setMinLag(1);
		    //forecaster.getTSLagMaker().setMaxLag(12); // monthly data
	
		    // add a month of the year indicator field
		    //forecaster.getTSLagMaker().setAddMonthOfYear(true);
	
		    // add a quarter of the year indicator field
		    //forecaster.getTSLagMaker().setAddQuarterOfYear(true);
	
		    // build the model
		    forecaster.buildForecaster(instances, System.out);
		    
		    return forecaster;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	private Instances dataToInstances(HashMap<String,Long> data)
	{
		try
		{
			// ensure the data is sorted by key (date) and remove outliers
			TreeMap<String,Long> sortedData = new TreeMap<>();
			for(String date : data.keySet())
			{
				Long value = data.get(date);
				if(value < Integer.MAX_VALUE) sortedData.put(date, data.get(date));
			}
				
			// create the structure of the dataset
			Instances dataset = createDataset();
			
			// add the instances
			for(String key : sortedData.keySet())
			{
				dataset.add(dataToInstance(key, sortedData.get(key), dataset));
			}
			
			return dataset;
		}
		catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	private Instance dataToInstance(String time, Long value, Instances dataset) throws ParseException
	{
		Instance instance = new DenseInstance(dataset.get(0).numAttributes());
		instance.setValue(dataset.attribute(0), dataset.attribute(0).parseDate(time));
		instance.setValue(dataset.attribute(1), value);
		
		return instance;
	}
	
	private HashMap<String,Double> computeAverageVolumes(HashMap<String,Long> map)
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
	
	private Instances createDataset()
	{
		// define the attributes
		Attribute date = new Attribute(DATE_FIELD, DATE_FORMAT);
		Attribute volume = new Attribute(VOLUME_FIELD);
		
		// create the dataset
		ArrayList<Attribute> attrs = new ArrayList<>();
		attrs.add(date);
		attrs.add(volume);
		Instances dataset = new	Instances("dataset", attrs, 0);
		
		return dataset;
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
	
	private void detectPeriodicity(WekaForecaster forecaster, Instances dataset, String dateField)
	{
		  String selectedP = "<Unknown>";
		  
		  PeriodicityHandler detected = TSLagMaker.determinePeriodicity(dataset, dateField, Periodicity.UNKNOWN);
	      switch (detected.getPeriodicity()) {
	      	case HOURLY:
	      		selectedP = "Hourly";
	      		break;
	      	case DAILY:
	      		selectedP = "Daily";
	      		break;
	      	case WEEKLY:
	      		selectedP = "Weekly";
	     	break;
	      	case MONTHLY:
	      		selectedP = "Monthly";
	      		break;
	      	case QUARTERLY:
	      		selectedP = "Quarterly";
	      		break;
	      	case YEARLY:
	      		selectedP = "Yearly";
	      		break;
	      }

	      if (forecaster != null) {
	        if (selectedP.equals("Hourly")) {
	          forecaster.getTSLagMaker().setPeriodicity(
	              TSLagMaker.Periodicity.HOURLY);
	        } else if (selectedP.equals("Daily")) {
	          forecaster.getTSLagMaker().setPeriodicity(
	              TSLagMaker.Periodicity.DAILY);
	        } else if (selectedP.equals("Weekly")) {
	          forecaster.getTSLagMaker().setPeriodicity(
	              TSLagMaker.Periodicity.WEEKLY);
	        } else if (selectedP.equals("Monthly")) {
	          forecaster.getTSLagMaker().setPeriodicity(
	              TSLagMaker.Periodicity.MONTHLY);
	        } else if (selectedP.equals("Quarterly")) {
	          forecaster.getTSLagMaker().setPeriodicity(
	              TSLagMaker.Periodicity.QUARTERLY);
	        } else if (selectedP.equals("Yearly")) {
	          forecaster.getTSLagMaker().setPeriodicity(
	              TSLagMaker.Periodicity.YEARLY);
	        } else {
	          forecaster.getTSLagMaker().setPeriodicity(
	              TSLagMaker.Periodicity.UNKNOWN);
	        }
	        
	      }

	      // only set these defaults if the user is not using custom lag lengths!
	      forecaster.getTSLagMaker().setMinLag(1);
	      if (selectedP.equals("Hourly")) {
	    	  if (forecaster != null) {
	    		  forecaster.getTSLagMaker().setMaxLag(Math.min(dataset.numInstances() / 2, 24));
	          }
	      }
	      else if (selectedP.equals("Daily")) {
	          if (forecaster != null) {
	            forecaster.getTSLagMaker().setMaxLag(Math.min(dataset.numInstances() / 2, 7));
	          }    
	      }
	      else if (selectedP.equals("Weekly")) {
	          if (forecaster != null) {
	            forecaster.getTSLagMaker().setMaxLag(Math.min(dataset.numInstances() / 2, 52));
	          }
	      }
	      else if (selectedP.equals("Monthly")) {
	          if (forecaster != null) {
	            forecaster.getTSLagMaker().setMaxLag(Math.min(dataset.numInstances() / 2, 12));
	          }
	      }
	      else if (selectedP.equals("Quarterly")) {
	          if (forecaster != null) {
	            forecaster.getTSLagMaker().setMaxLag(Math.min(dataset.numInstances() / 2, 4));
	          }
	      }
	      else if (selectedP.equals("Yearly")) {
	          if (forecaster != null) {
	            forecaster.getTSLagMaker().setMaxLag(Math.min(dataset.numInstances() / 2, 5));
	          }
	      }
	      else {
	          // default (<Unknown>)
	          if (forecaster != null) {
	            forecaster.getTSLagMaker().setMaxLag(
	                Math.min(dataset.numInstances() / 2, 12));
	          }
	      }
	      
	      // configure defaults based on the above periodicity
		  if (selectedP.equals("Hourly")) {
			  forecaster.getTSLagMaker().setAddAMIndicator(true);
		  } else if (selectedP.equals("Daily")) {
		      forecaster.getTSLagMaker().setAddDayOfWeek(true);
		      forecaster.getTSLagMaker().setAddWeekendIndicator(true);
		  } else if (selectedP.equals("Weekly")) {
		      forecaster.getTSLagMaker().setAddMonthOfYear(true);
		      forecaster.getTSLagMaker().setAddQuarterOfYear(true);
		  } else if (selectedP.equals("Monthly")) {
		      forecaster.getTSLagMaker().setAddMonthOfYear(true);
		      forecaster.getTSLagMaker().setAddQuarterOfYear(true);
		  }
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
	 * @return the recentVolumes
	 */
	public Instances getRecentVolumes() {
		return recentVolumes;
	}

	/**
	 * @param recentVolumes the recentVolumes to set
	 */
	public void setRecentVolumes(Instances recentVolumes) {
		this.recentVolumes = recentVolumes;
	}

	/**
	 * @return the forecaster
	 */
	public WekaForecaster getForecaster() {
		return forecaster;
	}

	/**
	 * @param forecaster the forecaster to set
	 */
	public void setForecaster(WekaForecaster forecaster) {
		this.forecaster = forecaster;
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
