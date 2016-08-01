package eu.qualimaster.monitoring.volumePrediction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

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
	private ArrayList<Double> recentVolumes;
	
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
		this.recentVolumes = new ArrayList<>();
		this.forecaster = new WekaForecaster();
		this.historicalVolumes = new HashMap<>();
		trainModels(dataPath);
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
				
			// define the attributes
			Attribute date = new Attribute(DATE_FIELD, DATE_FORMAT);
			Attribute volume = new Attribute(VOLUME_FIELD);
			
			// create the dataset
			ArrayList<Attribute> attrs = new ArrayList<>();
			attrs.add(date);
			attrs.add(volume);
			Instances dataset = new	Instances("dataset", attrs, 12);
			
			// add the instances
			for(String key : sortedData.keySet())
			{
				Instance instance = new DenseInstance(dataset.get(0).numAttributes());
				instance.setValue(date, date.parseDate(key));
				instance.setValue(volume, sortedData.get(key));
				dataset.add(instance);
			}
			
			return dataset;
		}
		catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	public HashMap<String,Double> computeAverageVolumes(HashMap<String,Long> map)
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
	public ArrayList<Double> getRecentVolumes() {
		return recentVolumes;
	}

	/**
	 * @param recentVolumes the recentVolumes to set
	 */
	public void setRecentVolumes(ArrayList<Double> recentVolumes) {
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
