package eu.qualimaster.monitoring.volumePrediction;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import eu.qualimaster.dataManagement.events.HistoricalDataProviderRegistrationEvent;
import eu.qualimaster.events.EventHandler;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.monitoring.MonitoringConfiguration;
import eu.qualimaster.monitoring.events.SourceVolumeMonitoringEvent;
import eu.qualimaster.monitoring.events.SourceVolumePredictionRequest;
import eu.qualimaster.monitoring.events.SourceVolumePredictionResponse;
import eu.qualimaster.monitoring.utils.IScheduler;

/**
 * Entrance point for the volume prediction, it handles the events and provides the methods to use the functionalities of the volume prediction.
 * It contains a source-predictor map to efficiently resolve which predictor to use based on the source.
 * 
 * @author  Andrea Ceroni
 */
public class VolumePredictionManager {
	
	private static final String DEFAULT_FILE_NAME = "historical_data.txt";

	private static final HistoricalDataProviderRegistrationEventHandler HISTORICAL_DATA_REGISTRATION_EVENT_HANDLER 
            = new HistoricalDataProviderRegistrationEventHandler();

	private static final SourceVolumePredictionRequestHandler SOURCE_VOLUME_PREDICTION_REQUEST_HANDLER 
	    = new SourceVolumePredictionRequestHandler();
	
	/**
	 * Set of available volume predictors, one for each different source.
	 */
	private static HashMap<String,VolumePredictor> volumePredictors = new HashMap<>();
	
	/**
	 * Initializes the volume predictor for a given source (either Spring or Twitter), assuming that the data provider has been already
	 * set via the proper event. This must be called before feeding the predictor with volume data, with enough advance to let the 
	 * predictors (one for each input term) be trained.
	 * @param s the source the predictor will refer to.
	 * @param monitoredTerms the initial set of terms to be monitored and for which a predictor must be trained. It can be null or empty, 
	 * in this case the predictor will not be able to make any prediction and will need to be updated at some point. 
	 * @param blindTerms the initial set of terms whose historical volume can looked up. It can be null or empty. 
	 * @param path the path of a temporary file used within the predictor to store and read data.
	 */
	public static void initialize(String source, HashSet<String> monitoredTerms, HashSet<String> blindTerms, String path){
		VolumePredictor predictor = volumePredictors.get(source);
		if(predictor != null) predictor.initialize(monitoredTerms, blindTerms, path);
		else System.out.println("ERROR: no volume predictor available for the input source" + source);
	}
	
	/**
	 * Performs a blind prediction (i.e. only based on historical data, if available) for a given term in a given source
	 * @param source the source containing the term
	 * @param term the term whose volume has to be predicted
	 * @return the predicted volume of the term; -1 if a model with historcal data for the input term is not available. 
	 */
	public static double predictBlindly(String source, String term){
		VolumePredictor predictor = volumePredictors.get(source);
		if(predictor != null) return predictor.predictBlindly(term);
		else{
			System.out.println("ERROR: no volume predictor available for the input source" + source);
			return -1;
		}
	}
	
	/**
	 * Updates the models for all the terms in all the sources.
	 */
	public static void updatePredictors(){
		for(String source : volumePredictors.keySet()){
			updatePredictorsForSource(source);
		}
	}
	
	/**
	 * Updates the predictors for all the terms in a single source 
	 * @param source
	 */
	public static void updatePredictorsForSource(String source){
		VolumePredictor predictor = volumePredictors.get(source);
		if(predictor != null) predictor.updatePrediction();
		else System.out.println("ERROR: no volume predictor available for the input source" + source);
	}
	
	/**
	 * Trains a model for a new term to be monitored and adds it to the predictor (useful for having a model ready before the term is added to the source).
	 * @param source the source that the term belongs to
	 * @param term the new monitored term to be added
	 */
	public static void addMonitoredTerm(String source, String term){
		VolumePredictor predictor = volumePredictors.get(source);
		if(predictor != null) predictor.addMonitoredTerm(term);
		else System.out.println("ERROR: no volume predictor available for the input source" + source);
	}
	
	/**
	 * Removes a monitored term (and the corresponding model) from the predictor.
	 * @param source the source that the term belongs to
	 * @param term the monitored term to be removed
	 */
	public static void removeMonitoredTerm(String source, String term){
		VolumePredictor predictor = volumePredictors.get(source);
		if(predictor != null) predictor.removeMonitoredTerm(term);
		else System.out.println("ERROR: no volume predictor available for the input source" + source);
		}
	
	/** Creates a blind model for a new term and adds it to the predictor.
	 * @param source the source that the term belongs to
	 * @param term the blind term to be added
	 */
	public static void addBlindTerm(String source, String term){
		VolumePredictor predictor = volumePredictors.get(source);
		if(predictor != null) predictor.addBlindTerm(term);
		else System.out.println("ERROR: no volume predictor available for the input source" + source);
		}
	
	/**
	 * Removes a blind term (and the corresponding bling model) from the predictor
	 * @param source the source that the term belongs to
	 * @param term the blind term to be removed
	 */
	public static void removeBlindTerm(String source, String term){
		VolumePredictor predictor = volumePredictors.get(source);
		if(predictor != null) predictor.removeBlindTerm(term);
		else System.out.println("ERROR: no volume predictor available for the input source" + source);
	}
	
	/**
	 * Updates the volume threshold for a given monitored term.
	 * @param source the source that the term belongs to
	 * @param term the monitored term whose threshold has to be changed
	 * @param threshold the new volume threshold for the monitored term
	 */
//	public static void updateTermThreshold(String source, String term){
//		VolumePredictor predictor = volumePredictors.get(source);
//		if(predictor != null) predictor.updateTermThreshold(term);
//		else System.out.println("ERROR: no volume predictor available for the input source" + source);
//	}
	
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
	        // called when a data source comes up in a pipeline. Carries the historical data provider.
	        // If the source changes, an event with the same pipeline / element name will occur
	    	VolumePredictor predictor = new VolumePredictor(event.getPipeline(), event.getSource(), event.getProvider());
	    	predictor.initialize(MonitoringConfiguration.getVolumeModelLocation() + event.getSource() + "_" + DEFAULT_FILE_NAME);
	    	volumePredictors.put(event.getSource(), predictor);
	    }
	}

	/**
	 * A handler for source volume prediction requests. Leads to a {@link SourceVolumePredictionResponse}.
	 *
	 * @author Andrea Ceroni
	 * @author Holger Eichelberger
	 */
	private static class SourceVolumePredictionRequestHandler extends EventHandler<SourceVolumePredictionRequest> {

	    /**
             * Creates an instance.
             */
            protected SourceVolumePredictionRequestHandler() {
                super(SourceVolumePredictionRequest.class);
            }

            @Override
            protected void handle(SourceVolumePredictionRequest event) {
                Map<String, Double> predictions = new HashMap<String, Double>();
                for (int k = 0; k < event.getKeywordCount(); k++) {
                    String keyword = event.getKeyword(k);
                    predictions.put(keyword, predictBlindly(event.getSource(), keyword));
                }
                EventManager.send(new SourceVolumePredictionResponse(event, predictions));
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
	    // called when aggregated source volume information is available. The frequency is by default 60000ms
	    // but can be defined in the infrastructure configuration via QM-IConf. May be delayed if the source does 
	    // not emit data. No data is aggregated in the source if the getAggregationKey(.) method returns null.
		
		// TODO handle exceptions like: source map does not contain an input term; the model for a source is not available (null);
		
		// TODO dynamically add an unknown term when observed from the source? This can be done if we don't expect thresholds as input
		
		// use the right predictor (based on the source) to handle the prediction for the incoming terms
		VolumePredictor predictor = volumePredictors.get(event.getPipelineElement());
		if(predictor != null) predictor.handlePredictionStep(event.getObservations());
		else System.out.println("ERROR: no volume predictor available for the input source" + event.getPipelineElement());
	}
	    
	/**
	* Called upon startup of the infrastructure.
	*/
	public static void start(IScheduler scheduler) {
	    EventManager.register(HISTORICAL_DATA_REGISTRATION_EVENT_HANDLER);
	    EventManager.register(SOURCE_VOLUME_PREDICTION_REQUEST_HANDLER );
	    
	    // this is rather initial - each day at 2:00
	    Calendar cal = Calendar.getInstance();
	    cal.add(Calendar.DAY_OF_YEAR, 1);
	    cal.set(Calendar.HOUR, 2);
	    cal.set(Calendar.MINUTE, 0);
	    cal.set(Calendar.SECOND, 0);
	    scheduler.schedule(new ModelUpdateTask(), cal.getTime(), 24 * 60 * 60 * 1000);
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
	    EventManager.unregister(HISTORICAL_DATA_REGISTRATION_EVENT_HANDLER);
	    EventManager.unregister(SOURCE_VOLUME_PREDICTION_REQUEST_HANDLER);
	}
}
