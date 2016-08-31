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
	public static void main(String[] args){
		// test historical data retrieval
		testHistoricalDataRetrieval();
	}
	
	private static void testHistoricalDataRetrieval(){
		VolumePredictionManager.setTest(true);
		VolumePredictionManager.handleTest(new HistoricalDataProviderRegistrationEvent("pipeline", "spring", new SpringHistoricalDataProvider()));
	}
}