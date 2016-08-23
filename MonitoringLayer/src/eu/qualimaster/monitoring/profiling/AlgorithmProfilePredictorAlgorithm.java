package eu.qualimaster.monitoring.profiling;

/**
 * This interface serves as the common ground for different implementations 
 * of prediction algorithms used for/by the profiling.
 * @author Christopher Voges
 *
 */
public interface AlgorithmProfilePredictorAlgorithm extends Cloneable {

    /**
     * Predict the state of the monitored value for one time step ahead.
     * 
     * @return Prediction for one time step ahead of the last update as {@link Double}.
     */
    public double predict();
    
    /**
     * Predict the state of the monitored value for the given number of time steps ahead.
     * 
     * @param steps Number of steps to predict ahead.
     *      <p> steps = 0: Predict one step after the time step of the last update.
     *      <p> steps > 0: Predict X step(s) ahead of 'now'.
     * @return Prediction for one time step ahead of the last update as {@link Double}.
     */
    public double predict(int steps);
    
     /**
      * This method updates the predictor algorithm with the last known state/measurement for two values.
      * 
      * @param xMeasured Time step of measurement as seconds since midnight, January 1, 1970 UTC.
      * @param yMeasured Current measurement.
      * @return True if the update was successful.
      */
    public boolean update(long xMeasured, double yMeasured);
    
    /**
     * This method updates the predictor algorithm with the last known state/measurement for one value.
     * 
     * @param measured Current measurement.
     * @return True if the update was successful.
     */
    public boolean update(double measured);
}
