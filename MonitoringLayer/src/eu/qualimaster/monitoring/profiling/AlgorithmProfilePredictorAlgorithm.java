package eu.qualimaster.monitoring.profiling;

/**
 * This interface serves as the common ground for different implementations 
 * of prediction algorithms used for/by the profiling.
 * @author Christopher Voges
 *
 */
public interface AlgorithmProfilePredictorAlgorithm {

    /**
     * Predict the state of the monitored value for one time step
     * after the last update. 
     * 
     * @return Prediction for one time step ahead of the last update as {@link Double}.
     */
    public double predict();
    
     /**
      * This method updates the predictor algorithm with the last known state/measurement.
      * 
      * @param xMeasured Time step of measurement.
      * @param yMeasured Current measurement.
      * @return True if the update was successful.
      */
    public boolean update(double xMeasured, double yMeasured);
}
