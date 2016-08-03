package eu.qualimaster.monitoring.volumePrediction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import eu.qualimaster.dataManagement.events.HistoricalDataProviderRegistrationEvent;
import eu.qualimaster.events.EventHandler;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.monitoring.events.SourceVolumeMonitoringEvent;

/**
 * Main class implementing the available methods of the volume prediction.
 * 
 * @author  Andrea Ceroni
 */
public class VolumePredictor {
	
	/** Set of sources (either stocks or hashtags) monitored by the volume prediction */
	/** Each source contains a thresholds for identifying too high volumes and raising alarms to the adaptation layer
	 *  TODO Decide how to identify these thresholds (e.g. wrt to the recent values? wrt to the current load of the infrastructure?)
	 * */
	private HashSet<Source> monitoredSources;
	
	/** Map containing the names of the sources (either stocks or hashtags) for which a prediction model is available (along with the corresponding model) */
	private HashMap<String,Prediction> models;
	
	/** Map containing the names of the sources (either stocks or hashtags) for which a "blind" prediction model is available (along with the corresponding model) */
	private HashMap<String,BlindPrediction> blindModels;
	
	/** The status of the component (whether it is running or not) */
	private boolean running;

	/** The number of month of data to consider when training the model */
	private static final int NUM_MONTHS = 4;
	
	/** The granularity of the prediction (in milliseconds) */
	private static final int GRANULARITY = 60000;
	
	/**
	 * Constructor taking a set of sources (terms and their volume thresholds) as input.
	 * 
	 * @param monitoredSources The set of sources to be monitored by the volume prediction.
	 */
	public VolumePredictor(ArrayList<Source> monitoredSources)
	{
		this.monitoredSources = new HashSet<>(monitoredSources);
		this.running = false;
		initializeModels();
	}
	
	public void runPrediction()
	{
		// set "running" to true to start the component. It can be stopped by setting "running" to false
		this.running = true;
		
		// prediction loop (for each monitored source)
		while(this.running)
		{
			Long startTime = System.nanoTime();
			for(Source s : this.monitoredSources)
			{
				Prediction model = this.models.get(s.getName());
				
				// observe the current volume of the source and update the recent values within the model
				// TODO clarify whether the getCurrentData method will return only the volume or also the timestamp
				Long currVolume = getCurrentData(s.getName());
				model.updateRecentVolumes(null, currVolume);
				
				// predict the volume within the next time step
				double prediction = model.predict();
				
				// check whether the predicted volume is critical and, if so, raise an alarm to the adaptation layer
				evaluatePrediction(s, prediction);
				
				// store the current observation in the historical data of the current source
				storeInHistoricalData(s.getName(), null, currVolume);
			}
			
			// store observed volumes for the blind models (to have historical data available in future)
			// TODO consider to move this within the source component: the aggregated volume is always stored
			for(String blindSource : this.blindModels.keySet())
			{
				if(!this.models.containsKey(blindSource))
				{
					Long currVolume = getCurrentData(blindSource);
					storeInHistoricalData(blindSource, null, currVolume);
				}
			}
			
			// sleep for the remaining time (given by the granularity)
			Long elapsedTime = System.nanoTime() - startTime;
			try
			{
				Thread.sleep(Math.max(0, GRANULARITY - elapsedTime));
			}
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public double predictBlindly(String sourceName)
	{
		if(blindModels.containsKey(sourceName)) return blindModels.get(sourceName).predictBlindly();
		else
		{
			// add the source to the list of blind models, with a null model, so that volumes for this new
			// source is observed and stored. Since the source is present in the map, the model will be
			// trained during the next updated.
			BlindPrediction newModel = new BlindPrediction(sourceName, null);
			blindModels.put(sourceName, null);
			return -1;
		}
	}
	
	public void stopPrediction()
	{
		this.running = false;
	}
	
	/**
	 * Updates the prediction models of each monitored source.
	 * TODO At the moment it assumes that the prediction (i.e. the runPrediction() method) is not running.
	 * 		Could be run as a separate thread and exchange the models once the new ones are ready.
	 */
	public void updatePrediction()
	{
		this.models.clear();
		this.blindModels.clear();
		initializeModels();
	}
	
	private void evaluatePrediction(Source s, double prediction)
	{
		// TODO clarify the policy to raise alarms. At the moment is a simple comparison with the threshold
		if(prediction > s.getThreshold()) raiseAlarm(s.getName(), prediction);
	}
	
	private void raiseAlarm(String term, double volume)
	{
		// TODO use the event class defined in the infrastructure to send alarms to the adaptation layer
	}
	
	private void initializeModels()
	{
		// models
		for(Source source : this.monitoredSources)
		{
			Prediction model = new Prediction(source.getName(), getHistoricalData(source.getName(), NUM_MONTHS));
			this.models.put(source.getName(), model);
		}
		
		// blind models
		ArrayList<String> availableSources = getSourcesWithHistoricalData();
		for(String source : availableSources)
		{
			BlindPrediction model = new BlindPrediction(source, getHistoricalData(source, NUM_MONTHS));
			this.blindModels.put(source, model);
		}
	}
	
	private long getCurrentData(String term)
	{
		// TODO get current data via the source
		return -1;
	}
	
	private String getHistoricalData(String term, int months)
	{
		// TODO get historical data via the code in the source (the actual return value still has to be decided)
		return null;
	}
	
	private ArrayList<String> getSourcesWithHistoricalData()
	{
		// TODO get the sources for which there is some historical data via the code in the source component
		return null;
	}
	
	private String storeInHistoricalData(String term, String timestamp, Long value)
	{
		// TODO store a value in the historical data (the presence of the timestamp still has to be decided)
		return null;
	}

	/**
	 * @return the monitoredSources
	 */
	public HashSet<Source> getMonitoredSources() {
		return this.monitoredSources;
	}
	
	/**
	 * Removes a source from the monitored ones (but keep the corresponding model to still support blind predictions)
	 * @param term The source to be removed
	 */
	public void removeMonitoredSource(Source source)
	{
		this.monitoredSources.remove(source);
	}
	
	/**
	 * Adds a source to be monitored and initializes its prediction model.
	 * The model is initialized even if a model for the source already exists, because it might be outdated.
	 * @param source The term to be monitored
	 */
	public void addMonitoredSource(Source source)
	{
		Prediction model = new Prediction(source.getName(), getHistoricalData(source.getName(), NUM_MONTHS));
		this.models.put(source.getName(), model);
		this.monitoredSources.add(source);
	}

	/**
	 * @return the running
	 */
	public boolean isRunning() {
return running;
	}
	
	/**
	 * A handler for upcoming data sources. Transports the historical data providers if available.
	 * 
	 * @author  Andrea Ceroni
	 */
	private static class HistoricalDataProviderRegistrationEventHandler extends EventHandler<HistoricalDataProviderRegistrationEvent> {

	    /**
	     * Creates an instance.
	     */
	    protected HistoricalDataProviderRegistrationEventHandler() {
	        super(HistoricalDataProviderRegistrationEvent.class);
	    }

	    @Override
	    protected void handle(HistoricalDataProviderRegistrationEvent event) {
	        // TODO called when a data source comes up in a pipeline. Carries the historical data provider.
	        // If the source changes, an event with the same pipeline / element name will occur
	    }
	            
	}

	/**
	 * Is called when the monitoring manager receives a {@link SourceVolumeMonitoringEvent}.
	 * Although a full event bus handler would also do the job, this shall be less resource consumptive as 
	 * the event is anyway received in the Monitoring Layer.
	 * 
	 * @param event the event
	 */
	public static void notifySourceVolumeMonitoringEvent(SourceVolumeMonitoringEvent event) {
	    // TODO called when aggregated source volume information is available. The frequency is by default 60000ms
	    // but can be defined in the infrastructure configuration via QM-IConf. May be delayed if the source does 
	    // not emit data. No data is aggregated in the source if the getAggregationKey(.) method returns null.
	}
	    
	/**
	* Called upon startup of the infrastructure.
	*/
	public static void start() {
	    EventManager.register(new HistoricalDataProviderRegistrationEventHandler());
	}

	/**
	* Notifies the predictor about changes in the lifecycle of pipelines.
	*  
	* @param event the lifecycle event
	*/
	public static void notifyPipelineLifecycleChange(PipelineLifecycleEvent event) {
	}

	/**
	* Called upon shutdown of the infrastructure. Clean up global resources here.
	*/
	public static void stop() {
	}

}