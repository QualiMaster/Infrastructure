package eu.qualimaster.monitoring.volumePrediction;

import java.util.HashMap;
import java.util.HashSet;

import eu.qualimaster.dataManagement.events.HistoricalDataProviderRegistrationEvent;
import eu.qualimaster.events.EventHandler;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.monitoring.events.SourceVolumeMonitoringEvent;

/**
 * Entrance point for the volume prediction. It handles the events and contains 2 sets of predictors (for Twitter and Spring) separately.
 * 
 * @author  Andrea Ceroni
 */
public class VolumePredictionManager {

	/**
	 * Volume predictor for Spring (terms and corresponding predictors are stored as hashmap) 
	 */
	private static VolumePredictor springVolumePredictor = new VolumePredictor(SourceName.SPRING) ;
	
	/**
	 * Volume predictor for Twitter (terms and corresponding predictors are stored ad hashmap)
	 */
	private static VolumePredictor twitterVolumePredictor = new VolumePredictor(SourceName.TWITTER);
	
	/**
	 * Initializes the volume predictor for a given source (either Spring or Twitter), assuming that the data provider has been already
	 * set via the proper event. This must be called before feeding the predictor with volume data, with enough advance to let the 
	 * predictors (one for each input term) be trained.
	 * @param s the source (either Spring or Twitter) the predictor will refer to.
	 * @param monitoredTerms the initial set of terms to be monitored (with their volume thresholds) and for which a predictor must be 
	 * trained. It can be null or empty, in this case the predictor will not be able to make any prediction and will need to be updated at some point. 
	 * @param blindTerms the initial set of terms whose historical volume can looked up. It can be null or empty. 
	 * @param path the path of a temporary file used within the predictor to store and read data.
	 */
	public static void initialize(SourceName s, HashMap<String,Long> monitoredTerms, HashSet<String> blindTerms, String path){
		if(s == SourceName.SPRING) springVolumePredictor.initialize(monitoredTerms, blindTerms, path);
		else if(s == SourceName.TWITTER) twitterVolumePredictor.initialize(monitoredTerms, blindTerms, path);
		else System.out.println("ERROR: the input source is not handled.");
	}
	
	/**
	 * Performs a blind prediction (i.e. only based on historical data, if available) for a given term in a given source
	 * @param source the source containing the term
	 * @param term the term whose volume has to be predicted
	 * @return
	 */
	public static double predictBlindly(SourceName source, String term){
		if(source == SourceName.SPRING) return springVolumePredictor.predictBlindly(term);
		else if(source == SourceName.TWITTER) return twitterVolumePredictor.predictBlindly(term);
		else{
			System.out.println("ERROR: the input source is not handled.");
			return -1;
		}
	}
	
	/**
	 * Updates the models for all the terms in all the sources.
	 */
	public static void updatePredictors(){
		springVolumePredictor.updatePrediction();
		twitterVolumePredictor.updatePrediction();
	}
	
	/**
	 * Trains a model for a new term to be monitored and adds it to the predictor (useful for having a model ready before the term is added to the source).
	 * @param source the source that the term belongs to
	 * @param term the new monitored term to be added
	 * @param threshold the volume threshold for the new term
	 */
	public static void addMonitoredTerm(SourceName source, String term, long threshold){
		if(source == SourceName.SPRING) springVolumePredictor.addMonitoredTerm(term, threshold);
		else if(source == SourceName.TWITTER) twitterVolumePredictor.addMonitoredTerm(term, threshold);
		else System.out.println("ERROR: the input source is not handled.");
	}
	
	/**
	 * Removes a monitored term (and the corresponding model) from the predictor.
	 * @param source the source that the term belongs to
	 * @param term the monitored term to be removed
	 */
	public static void removeMonitoredTerm(SourceName source, String term){
		if(source == SourceName.SPRING) springVolumePredictor.removeMonitoredTerm(term);
		else if(source == SourceName.TWITTER) twitterVolumePredictor.removeMonitoredTerm(term);
		else System.out.println("ERROR: the input source is not handled.");
	}
	
	/** Creates a blind model for a new term and adds it to the predictor.
	 * @param source the source that the term belongs to
	 * @param term the blind term to be added
	 */
	public static void addBlindTerm(SourceName source, String term){
		if(source == SourceName.SPRING) springVolumePredictor.addBlindTerm(term);
		else if(source == SourceName.TWITTER) twitterVolumePredictor.addBlindTerm(term);
		else System.out.println("ERROR: the input source is not handled.");
	}
	
	/**
	 * Removes a blind term (and the corresponding bling model) from the predictor
	 * @param source the source that the term belongs to
	 * @param term the blind term to be removed
	 */
	public static void removeBlindTerm(SourceName source, String term){
		if(source == SourceName.SPRING) springVolumePredictor.removeBlindTerm(term);
		else if(source == SourceName.TWITTER) twitterVolumePredictor.removeBlindTerm(term);
		else System.out.println("ERROR: the input source is not handled.");
	}
	
	/**
	 * Updates the volume threshold for a given monitored term.
	 * @param source the source that the term belongs to
	 * @param term the monitored term whose threshold has to be changed
	 * @param threshold the new volume threshold for the monitored term
	 */
	public static void updateTermThreshold(SourceName source, String term, long threshold){
		if(source == SourceName.SPRING) springVolumePredictor.updateTermThreshold(term, threshold);
		else if(source == SourceName.TWITTER) twitterVolumePredictor.updateTermThreshold(term, threshold);
		else System.out.println("ERROR: the input source is not handled.");
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
	        // called when a data source comes up in a pipeline. Carries the historical data provider.
	        // If the source changes, an event with the same pipeline / element name will occur
	    	
	    	if(event.getSource().equalsIgnoreCase(SourceName.SPRING.toString())){
	    		VolumePredictionManager.springVolumePredictor.setHistoryProvider(event.getProvider());
	    	}
	    	else if(event.getSource().equalsIgnoreCase(SourceName.TWITTER.toString())){
	    		VolumePredictionManager.twitterVolumePredictor.setHistoryProvider(event.getProvider());
	    	}
	    	else{
	    		System.out.println("ERROR: the input source is unknown.");
	    	}
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
		
		// use the right predictor based on the source
		VolumePredictor predictor = null;
		if(event.getPipelineElement().equalsIgnoreCase(SourceName.SPRING.toString())) predictor = springVolumePredictor;
		else if(event.getPipelineElement().equalsIgnoreCase(SourceName.TWITTER.toString())) predictor = twitterVolumePredictor;
		else{
    		System.out.println("ERROR: the input source is unknown.");
    		return;
    	}
		
		// handle the prediction for the incoming terms
		predictor.handlePredictionStep(event.getObservations());
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
	
	public enum SourceName {
	    SPRING, TWITTER 
	}
}
