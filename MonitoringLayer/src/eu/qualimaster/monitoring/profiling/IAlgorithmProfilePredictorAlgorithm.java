/*
 * Copyright 2009-2016 University of Hildesheim, Software Systems Engineering
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.qualimaster.monitoring.profiling;

import java.util.ArrayList;

/**
 * This interface serves as the common ground for different implementations 
 * of prediction algorithms used for/by the profiling.
 * @author Christopher Voges
 *
 */
public interface IAlgorithmProfilePredictorAlgorithm extends Cloneable {

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
    
    /** 
     * Generates a String representation of a {@link IAlgorithmProfilePredictorAlgorithm} instance.
     * @return {@link ArrayList} of {@link String} representing a {@link IAlgorithmProfilePredictorAlgorithm} instance.
     */
    public ArrayList<String> toStringArrayList();
    
    /**
     * Returns the key identifier for this predictor.
     * 
     * @return the identifier
     */
    public String getIdentifier();
    
}
