package tests.eu.qualimaster.monitoring.volumePrediction;

import eu.qualimaster.dataManagement.sources.SpringHistoricalDataProvider;
import eu.qualimaster.monitoring.volumePrediction.VolumePredictionManager;
import eu.qualimaster.monitoring.volumePrediction.VolumePredictor;

/**
 * Class for testing the volume prediction in a stand-alone way (not running in the infrastructure).
 * 
 * @author  Andrea Ceroni
 */
public class StandaloneTests
{
	public static void main(String[] args){
		VolumePredictionManager.setTest(true);
		VolumePredictor predictor = new VolumePredictor("this_pipeline", "spring", new SpringHistoricalDataProvider(), true);
		System.out.println("done.");
	}
}