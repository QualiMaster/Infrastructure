package eu.qualimaster.monitoring.volumePrediction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import eu.qualimaster.adaptation.events.SourceVolumeAdaptationEvent;
import eu.qualimaster.dataManagement.DataManager;
import eu.qualimaster.dataManagement.sources.IHistoricalDataProvider;
import eu.qualimaster.dataManagement.sources.TwitterHistoricalDataProvider;
import eu.qualimaster.dataManagement.storage.hbase.HBaseStorageSupport;
import eu.qualimaster.events.EventManager;

/**
 * Main class implementing the available methods of the volume prediction.
 * 
 * @author  Andrea Ceroni
 */
public class VolumePredictor {
	
	/** The pipeline the predictor refers to. */
	private String pipeline;
	
	/** The source the predictor refers to. */
	private String source;
	
	/** Set of terms (either stocks or hashtags) monitored by the volume prediction. */
	private HashSet<String> monitoredTerms;
	
	/** Set of terms (either stocks or hashtags) for which historical data might be looked up as blind prediction. */
	private HashSet<String> blindTerms;
	
	/** Recent observed volumes of the monitored terms, used to decide whether to raise alarms or not */
	private HashMap<String, ArrayList<Long>> recentVolumes;

	/** Map containing the term (either stocks or hashtags) for which a prediction model is available (along with the corresponding model) */
	private HashMap<String,Prediction> models;
	
	/** Map containing the term (either stocks or hashtags) for which a "blind" prediction model is available (along with the corresponding model) */
	private HashMap<String,BlindPrediction> blindModels;
	
	/** The status of the component (whether it is running or not) */
	private boolean running;
	
	/** The provider of historical data */
	private IHistoricalDataProvider historyProvider;
	
	/** File to temporarily store historical data */
	private File historicalDataFile;
	
	/** Flag indicating whether the class is being used in test mode or not */
	private boolean test;
	
	/** The number of months (in milliseconds) of data to consider when training the model */
	private static final long NUM_MONTHS = 4l * (1000l*60l*60l*24l*30l);
	
	/** The format for storing dates */
	private static final String DATE_FORMAT = "MM/DD/YYYY,hh:mm:ss";
	
	/** The size of the recent history (number of time points) */
	private static final int RECENT_HISTORY_SIZE = 30;
	
	/** The number of recent data points for checking small but regular increases */
	private static final int REGULAR_INCREASE_SIZE = RECENT_HISTORY_SIZE / 3;
	
	/** Url used to retrieve historical data in test mode */
	private static final String TEST_URL = "test";
	
	/**
	 * Default constructor of the predictor, no models are trained yet.
	 */
	public VolumePredictor(String pipeline, String source, IHistoricalDataProvider dataProvider, boolean test)
	{
		this.pipeline = pipeline;
		this.source = source;
		this.monitoredTerms = null;
		this.blindTerms = null;
		this.recentVolumes = null;
		this.models = null;
		this.blindModels = null;
		this.running = false;
		this.historyProvider = dataProvider;
		this.historicalDataFile = null;
		this.test = test;
	}
	
	/**
	 * Default constructor of the predictor, no models are trained yet.
	 */
	public VolumePredictor(String pipeline, String source, IHistoricalDataProvider dataProvider)
	{
		this(pipeline, source, dataProvider, false);
	}
	
	/**
	 * Initializes the volume predictor with a set of terms to be monitored and a set of terms to be looked up for blind prediction,
	 * assuming that the data provider has been already set.
	 * @param monitoredSources the initial set of terms to be monitored and for which a predictor must be trained. It can be null or 
	 * empty, in this case the predictor will not be able to make any prediction and will need to be updated at some point.
	 * @param blindSources the initial set of terms whose historical volume can looked up. It can be null or empty.
	 * @param filePath the path of a temporary file used to store and read data.
	 */
	public void initialize(HashSet<String> monitoredTerms, HashSet<String> blindTerms, String filePath)
	{
		// check if the historical data provider has been set
		if(this.historyProvider == null){
			System.out.println("ERROR: no historical data provider has been set.");
			return;
		}
		
		if(monitoredTerms != null) this.monitoredTerms = new HashSet<>(monitoredTerms);
		else this.monitoredTerms = new HashSet<>();
		if(blindTerms != null) this.blindTerms = new HashSet<>(blindTerms);
		else this.blindTerms = new HashSet<>();
		this.recentVolumes = new HashMap<>();
		for(String term : this.monitoredTerms) this.recentVolumes.put(term, new ArrayList<Long>()); 
		this.running = false;
		this.historicalDataFile = new File(filePath);
		this.models = new HashMap<>();
		this.blindModels = new HashMap<>();
		initializeModels(this.models, this.blindModels);
	}
	
	/**
	 * Initializes the volume predictor using the default terms (monitored and blind) contained within the historical data handler,
	 * assuming that the data provider has been already set.
	 * @param monitoredSources the initial set of terms to be monitored and for which a predictor must be trained. It can be null or 
	 * empty, in this case the predictor will not be able to make any prediction and will need to be updated at some point.
	 * @param blindSources the initial set of terms whose historical volume can looked up. It can be null or empty.
	 * @param filePath the path of a temporary file used to store and read data.
	 */
	public void initialize(String filePath)
	{
		// check if the historical data provider has been set
		if(this.historyProvider == null){
			System.out.println("ERROR: no historical data provider has been set.");
			return;
		}
		
		HashSet<String> monitoredTerms = this.historyProvider.getDefaultMonitoredTerms();
		HashSet<String> blindTerms = this.historyProvider.getDefaultBlindTerms();
		initialize(monitoredTerms, blindTerms, filePath);
	}
	
	/**
	 * Makes a blind prediction (based on historical data) of the volume of a term that is not being monitored.
	 * @param term the not monitored term whose volume has to be predicted.
	 * @return the predicted volume of the term; -1 if a model with historial data for the input term is not available.
	 */
	public double predictBlindly(String term)
	{
		BlindPrediction model = this.blindModels.get(term);
		if(model != null) return model.predictBlindly();
		else
		{
			// add the term to the set of blind models with a null model, so that a model for this new term will be trained during the next update.
			this.blindModels.put(term, null);
			this.blindTerms.add(term);
			return -1;
		}
	}
	
	/**
	 * Processes the current set of observed volumes: prediction, evaluation, storage.
	 * @param observations the term-volume map containing the observed volumes for each term
	 */
	public void handlePredictionStep(Map<String,Integer> observations){
		String timestamp = getTimestamp();
		HashMap<String,Double> alarms = new HashMap<>();
		ArrayList<String> unknownTerms = new ArrayList<>();
		for(String term : observations.keySet())
		{	
			long currVolume = (long)observations.get(term);
			Prediction model = this.models.get(term);
			
			// add the current observation to the recent volumes for the current term
			addRecentVolume(term, currVolume);
			
			if(model != null && model.getForecaster() != null){
				// update the recent values within the model
				model.updateRecentVolumes(timestamp, currVolume);
				
				// predict the volume within the next time step
				double prediction = model.predict();
				
				// check whether the predicted volume is critical and, if so, include the term when raising the alarm
				double deviation = evaluatePrediction(term, prediction);
				if(deviation != -1) alarms.put(term, deviation);
			}
			else{
				if(!this.monitoredTerms.contains(term)) unknownTerms.add(term);
			}
			
			// store the current observation in the historical data of the current term (only for twitter)
			storeInHistoricalData(term, timestamp, currVolume);
		}
		
		// raise an alarm to the adaptation layer containing all the critical terms and their volumes
		if(!alarms.isEmpty()) raiseAlarms(alarms);
		
		// initialize one predictor for each unknown term that was observed in the source
		for(String term : unknownTerms) addMonitoredTerm(term);
	}
	
	private String getTimestamp(){
		DateFormat format = new SimpleDateFormat(DATE_FORMAT);
		Date date = new Date();
		return format.format(date);
	}
	
	public void stopPrediction()
	{
		this.running = false;
	}
	
	/**
	 * Updates the prediction models of each monitored and blind term.
	 */
	public void updatePrediction()
	{
		// create the new models in separate objects not to interfere with any prediction that might be running
		HashMap<String,Prediction> newModels = new HashMap<>();
		HashMap<String,BlindPrediction> newBlindModels = new HashMap<>();
		initializeModels(newModels, newBlindModels);
		
		// change the models
		this.models.clear();
		this.models.putAll(newModels);
		this.blindModels.clear();
		this.blindModels.putAll(newBlindModels);
	}
	
	private double evaluatePrediction(String term, double prediction)
	{
		// handles 2 cases:
		//  - signal high peaks (using recent history to compute avg should detect these)
		//  - signal slightly but continuously increasing volumes: dangerous because the recent history is updated
		//    with always increasing values so the new values, although higher, do not result in alarms
		
		// compute average and std deviation within the recent history
		ArrayList<Long> recentVolumesForTerm = this.recentVolumes.get(term);
		double[] stats = computeStatistics(recentVolumesForTerm);
		
		// compute alarm threshold from avg and std and compare it with the predicted volume
		double threshold = stats[0] + 3 * stats[1];
		if(prediction > threshold) return (prediction - threshold);
		
		// check the trend of the recent volumes and signal if it is always increasing
		if(checkIncrease(recentVolumesForTerm, REGULAR_INCREASE_SIZE)) return computeIncrease(recentVolumesForTerm);
		
		else return -1;
	}
	
	private double[] computeStatistics(ArrayList<Long> data){
		double[] stats = new double[2];
		double avg, std;
		double sum = 0;
		double sumsq = 0;
		
		if(data == null || data.isEmpty()) return stats;

		// compute avg and standard deviation
		for(int i = 0; i < data.size(); i++) sum += data.get(i);
		avg = (double) sum / data.size();
		for(int i = 0; i < data.size(); i++) sumsq = sumsq + ((data.get(i) - avg) * (data.get(i) - avg));
		std = Math.sqrt((double) sumsq / data.size());

		// store the statistics
		stats[0] = avg;
		stats[1] = std;

		return stats;
	}
	
	private boolean checkIncrease(ArrayList<Long> history, int size){
		if(size > history.size() - 1) size = history.size() - 1;
		int i = history.size() - 1;
		int count = 0;
		while(count < size){
			if(history.get(i) < history.get(i-1)) return false;
			i--;
			count++;
		}
		return true;
	}
	
	private int computeIncrease(ArrayList<Long> values){
		int firstIndex = values.size() - REGULAR_INCREASE_SIZE;
		int lastIndex = values.size() - 1;
		return (int)(values.get(lastIndex) - values.get(firstIndex));
	}
	
	private void raiseAlarms(HashMap<String,Double> alarms)
	{
		// use the event class defined in the infrastructure to send alarms to the adaptation layer
		SourceVolumeAdaptationEvent svae = new SourceVolumeAdaptationEvent(this.pipeline, this.source, alarms);
		EventManager.send(svae);
	}
	
	private void addRecentVolume(String term, Long volume){
		if(this.recentVolumes.containsKey(term)){
			if(this.recentVolumes.get(term).size() > RECENT_HISTORY_SIZE) this.recentVolumes.get(term).remove(0);
			this.recentVolumes.get(term).add(volume);
		}
	}
	
	private void initializeModels(HashMap<String,Prediction> models, HashMap<String,BlindPrediction> blindModels){
		// make the union of monitored and blind terms to avoid getting historical data twice (in case a term appears in both the sets)
		HashSet<String> allTerms = new HashSet<>();
		allTerms.addAll(this.monitoredTerms);
		allTerms.addAll(this.blindTerms);
		
		for(String term : allTerms){
			getHistoricalData(term, NUM_MONTHS, this.historicalDataFile);
			
			// monitored models
			if(this.monitoredTerms.contains(term)){
				Prediction model = new Prediction(term, this.historicalDataFile);
				if(model.getForecaster() == null) model = null;
				models.put(term, model);
			}
			// blind models
			if(this.blindTerms.contains(term)){
				BlindPrediction model = new BlindPrediction(term, this.historicalDataFile);
				if(model.getHistoricalVolumes() == null) model = null;
				blindModels.put(term, model);
			}
		}	
	}
	
	private void getHistoricalData(String term, long months, File outputFile)
	{
		try{
			//this.historyProvider.setTest(this.test);
			if(this.test) this.historyProvider.obtainHistoricalData(NUM_MONTHS, term, this.historicalDataFile, TEST_URL);
			else this.historyProvider.obtainHistoricalData(NUM_MONTHS, term, this.historicalDataFile);
		}
		catch(IOException e){
			// handle the absence of historical data:
			// clear the content of the historicalDataFile, because it refers to the read of historical data for a previous term
			System.out.println("ERROR: historical data not available for term:" + term);
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(this.historicalDataFile));
				writer.write("");
				writer.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	private void storeInHistoricalData(String term, String timestamp, Long value)
	{
		// store a value in the historical data only for twitter
		if(this.historyProvider.getClass().equals(TwitterHistoricalDataProvider.class)) storeTwitterVolume(timestamp, term, value);
	}
	
	private void storeTwitterVolume(String timestamp, String term, Long volume){
		// Get the HBase table containing data for the input term and write into it
    	// TODO which strategy should be used?
    	HBaseStorageSupport table = (HBaseStorageSupport) DataManager.VOLUME_PREDICTION_STORAGE_MANAGER.getTable("", term, null);
    	table.connect();
    	table.doWrite(timestamp, volume);
    	table.disconnect();
	}
	
	/**
	 * Removes a term from the monitored ones
	 * @param term The term to be removed
	 */
	public void removeMonitoredTerm(String term)
	{
		this.monitoredTerms.remove(term);
		this.models.remove(term);
		this.recentVolumes.remove(term);
	}
	
	/**
	 * Adds a term to be monitored and initializes its prediction model.
	 * The model is initialized even if a model for the term already exists (because it might be outdated).
	 * @param source The term to be monitored
	 */
	public void addMonitoredTerm(String term)
	{
		getHistoricalData(term, NUM_MONTHS, this.historicalDataFile);
		
		Prediction model = new Prediction(term, this.historicalDataFile);
		if(model.getForecaster() == null) model = null;
		this.models.put(term, model);
		this.monitoredTerms.add(term);
		this.recentVolumes.put(term, new ArrayList<Long>());
	}
	
	/**
	 * Removes a term from the blind ones
	 * @param term The term to be removed
	 */
	public void removeBlindTerm(String term)
	{
		this.blindTerms.remove(term);
		this.blindModels.remove(term);
	}
	
	/**
	 * Adds a blind term and initializes its prediction model.
	 * The model is initialized even if a model for the term already exists (because it might be outdated).
	 * @param source The term to be monitored
	 */
	public void addBlindTerm(String term)
	{
		getHistoricalData(term, NUM_MONTHS, this.historicalDataFile);
		
		BlindPrediction model = new BlindPrediction(term, this.historicalDataFile);
		if(model.getHistoricalVolumes() == null) model = null;
		this.blindModels.put(term, model);
		this.blindTerms.add(term);
	}
	
//	public void updateTermThreshold(String term, long threshold){
//		if(this.monitoredTerms.containsKey(term)) this.monitoredTerms.put(term, threshold);
//		else System.out.println("ERROR: the required term is not monitored.");
//	}

	/**
	 * @return the running
	 */
	public boolean isRunning() {
		return running;
	}

	/**
	 * @return the historyProvider
	 */
	public IHistoricalDataProvider getHistoryProvider() {
		return historyProvider;
	}

	/**
	 * @param historyProvider the historyProvider to set
	 */
	public void setHistoryProvider(IHistoricalDataProvider historyProvider) {
		this.historyProvider = historyProvider;
	}

	/**
	 * @return the monitoredTerms
	 */
	public HashSet<String> getMonitoredTerms() {
		return monitoredTerms;
	}

	/**
	 * @param monitoredTerms the monitoredTerms to set
	 */
	public void setMonitoredTerms(HashSet<String> monitoredTerms) {
		this.monitoredTerms = monitoredTerms;
	}

	/**
	 * @return the blindTerms
	 */
	public HashSet<String> getBlindTerms() {
		return blindTerms;
	}

	/**
	 * @param blindTerms the blindTerms to set
	 */
	public void setBlindTerms(HashSet<String> blindTerms) {
		this.blindTerms = blindTerms;
	}

	/**
	 * @return the source
	 */
	public String getSourceName() {
		return source;
	}

	/**
	 * @param source the source to set
	 */
	public void setSourceName(String source) {
		this.source = source;
	}

	/**
	 * @return the pipeline
	 */
	public String getPipeline() {
		return pipeline;
	}

	/**
	 * @param pipeline the pipeline to set
	 */
	public void setPipeline(String pipeline) {
		this.pipeline = pipeline;
	}

	/**
	 * @return the recentVolumes
	 */
	public HashMap<String, ArrayList<Long>> getRecentVolumes() {
		return recentVolumes;
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