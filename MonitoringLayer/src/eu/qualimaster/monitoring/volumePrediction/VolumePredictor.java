package eu.qualimaster.monitoring.volumePrediction;

import java.util.HashMap;

/**
 * Main class implementing the available methods of the volume prediction.
 * 
 * @author  Andrea Ceroni
 */
public class VolumePredictor {
	
	/** Map containing the terms (either stocks or hashtags) for which a prediction model is available (along with the corresponding model) */
	private HashMap<String,PredictionModel> models;
	
}
