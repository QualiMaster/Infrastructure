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

import java.io.File;

import eu.qualimaster.monitoring.profiling.predictors.IUpdatable;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.tracing.TraceReader.Entry;
import eu.qualimaster.observables.IObservable;

/**
 * Represents an algorithm profile, regardless how multiple observables are actually handled.
 * 
 * @author Holger Eichelberger
 */
public interface IAlgorithmProfile extends IUpdatable {
    
    /**
     * Stores this instance.
     */
    public void store();

    /**
     * Updates the profile according to the measurements in <code>family</code>.
     * 
     * @param family the family used to update the predictors
     */
    public void update(PipelineNodeSystemPart family);

    /**
     * Updates the profile according to the measurements in <code>entry</code>.
     * 
     * @param timestamp the update timestamp
     * @param entry the entry used to update the predictors
     */
    public void update(long timestamp, Entry entry);

    /**
     * Predicts a value for the given <code>obserable</code>.
     * 
     * @param observable the observable to predict for
     * @param steps Number of steps to predict ahead.
     *      <p> steps = 0: Predict one step after the time step of the last update.
     *      <p> steps > 0: Predict X step(s) ahead of 'now'.
     * @return the predicted value, {@link Constants#NO_PREDICTION} if no prediction is possible
     */
    public double predict(IObservable observable, int steps);
    
    /**
     * Predicts the next value for <code>observable</code>.
     * 
     * @param observable the observable to predict for
     * @return the predicted value, {@link Constants#NO_PREDICTION} if no prediction is possible
     */
    double predict(IObservable observable);

    /**
     * Returns the folder where to store the profile of <code>observable</code>.
     * 
     * @param observable the observable 
     * @return the complete folder considering the base path of the {@link #element}
     */
    public File getFolder(IObservable observable);

}
