package tests.eu.qualimaster.monitoring.volumePrediction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import eu.qualimaster.dataManagement.events.HistoricalDataProviderRegistrationEvent;
import eu.qualimaster.dataManagement.sources.SpringHistoricalDataProvider;
import eu.qualimaster.monitoring.events.SourceVolumeMonitoringEvent;
import eu.qualimaster.monitoring.volumePrediction.DataUtils;
import eu.qualimaster.monitoring.volumePrediction.VolumePredictionManager;

/**
 * Class for testing the volume prediction in a stand-alone way (not running in the infrastructure).
 * 
 * @author  Andrea Ceroni
 */
public class StandaloneTests
{
	private static final String TEST_MAIN_FOLDER = "./testdata/volumePrediction/";

	private static final String TEST_WARMUP_FOLDER = TEST_MAIN_FOLDER + "warmupData/";
	private static final String TEST_STREAMING_FOLDER = TEST_MAIN_FOLDER + "streamingData";
	
	private static final String TEST_PIPELINE = "pipeline";
	private static final String TEST_SOURCE = "spring";

	public static void main(String[] args){
		// test historical data retrieval
		testHistoricalDataRetrieval();
		
		// test warm up
		testWarmUp();

		// test monitoring of a source
		testMonitoring();
	}
	
	public static void testHistoricalDataRetrieval(){
		VolumePredictionManager.setTest(true);
		VolumePredictionManager.handleHistoricalDataProviderRegistrationEventTest(new HistoricalDataProviderRegistrationEvent(TEST_PIPELINE, TEST_SOURCE, new SpringHistoricalDataProvider()), TEST_MAIN_FOLDER);
	}
	
	public static void testWarmUp(){
		VolumePredictionManager.setTest(true);
		VolumePredictionManager.warmUp(TEST_WARMUP_FOLDER);
	}
	
	public static void testMonitoring(){
		VolumePredictionManager.setTest(true);
		
		// get streaming data from folder for each term
		HashMap<String,ArrayList<Integer>> streamingData = DataUtils.readStreamingData(TEST_STREAMING_FOLDER, VolumePredictionManager.getPredictor(TEST_SOURCE).getMonitoredTerms());
		
		// trigger events simulating the observations from the source
		int step = 0;
		while(!streamingData.keySet().isEmpty()){
			HashSet<String> toRemove = new HashSet<>();
			HashMap<String,Integer> observations = new HashMap<>();
			for(String term : streamingData.keySet()){
				ArrayList<Integer> volumes = streamingData.get(term);
				if(step < volumes.size()) observations.put(term, volumes.get(step));
				if(step == volumes.size() - 1){
					toRemove.add(term);
				}
			}
			for(String term : toRemove) streamingData.remove(term);
			
			SourceVolumeMonitoringEvent event = new SourceVolumeMonitoringEvent(TEST_PIPELINE, TEST_SOURCE, observations);
			VolumePredictionManager.handleNotifySourceVolumeMonitoringEventTest(event);
			
			step++;
		}
	}
}
