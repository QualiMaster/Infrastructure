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
package eu.qualimaster.monitoring.profiling.predictors;

import java.io.File;
import java.io.IOException;

/**
 * This interface serves as the common ground for different implementations 
 * of prediction algorithms used for/by the profiling.
 * @author Christopher Voges
 *
 */
public interface IAlgorithmProfilePredictor extends Cloneable {

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
     * Stores this predictor to file.
     * 
     * @param file the target file
     * @param identifier the profile identifier
     * @throws IOException in case that the file cannot be written for some reason
     */
    public void store(File file, String identifier) throws IOException;

    /**
     * Loads this predictor from a given file.
     * 
     * @param file the file to load from
     * @param identifier the profile identifier
     * @throws IOException in case that the file cannot be read for some reason
     */
    public void load(File file, String identifier) throws IOException;
    
    /**
     * Returns whether two predictors are considered equal.
     * 
     * @param other the other predictor
     * @param diff the tolerance for double comparisons
     * @return <code>true</code> if considered equal, <code>false</code> else
     */
    public boolean equals(IAlgorithmProfilePredictor other, double diff);

    /**
     * Returns the timestamp of the last update.
     * 
     * @return the timestamp
     */
    public long getLastUpdated();
    
}
