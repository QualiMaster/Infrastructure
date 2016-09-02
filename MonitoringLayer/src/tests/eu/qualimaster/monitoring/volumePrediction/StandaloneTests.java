package tests.eu.qualimaster.monitoring.volumePrediction;

import eu.qualimaster.dataManagement.events.HistoricalDataProviderRegistrationEvent;
import eu.qualimaster.dataManagement.sources.SpringHistoricalDataProvider;
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
	
	public static void main(String[] args){
		// test historical data retrieval
		testHistoricalDataRetrieval();
		
		// test warm up
		testWarmUp();
	}
	
	private static void testHistoricalDataRetrieval(){
		VolumePredictionManager.setTest(true);
		VolumePredictionManager.handleTest(new HistoricalDataProviderRegistrationEvent("pipeline", "spring", new SpringHistoricalDataProvider()), TEST_MAIN_FOLDER);
	}
	
	private static void testWarmUp(){
		VolumePredictionManager.setTest(true);
		VolumePredictionManager.warmUp(TEST_WARMUP_FOLDER);
	}
}