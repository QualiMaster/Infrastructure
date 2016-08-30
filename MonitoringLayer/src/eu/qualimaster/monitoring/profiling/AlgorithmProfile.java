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

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

import eu.qualimaster.monitoring.observations.IObservation;
import eu.qualimaster.monitoring.observations.SingleObservation;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.ResourceUsage;
import eu.qualimaster.observables.Scalability;
import eu.qualimaster.observables.TimeBehavior;

/**
 * Data class to modulate the relation of
 * <p> - one Pipeline
 * <p> - one FamilyElement
 * <p> - one Algorithm
 * <p> - the {@link IObservable} to predict and its last {@link IObservation} 
 * represented by their values ({@link String} and {@link Double}).
 * Currently only one {@link IObservable} is predicted therefore this {@link Map} always needs to have the size 1! 
 * <p> - and multiple {@link IObservable} and their {@link IObservation}s serving as parameters for the Pipeline,
 * PipelineElement and the Algorithm (e.g. the number of TASKS or EXECUTORS or
 * the current INPUT). They are represented by their values ({@link String} and {@link Double}).
 * <p> Furthermore the {@link AlgorithmProfilePredictorAlgorithm}
 * instance used to predict the future for the outlined relation is also
 * accessible from this classes instances.
 * 
 * @author Christopher Voges
 *
 */
public class AlgorithmProfile {
    // Profiling attributes (inserted via constructor or setter)
    private String pipeline = null;
    private String element = null;
    private String algorithm = null;
    private Map<String, Double> predicted = null;
    private Map<String, Double> parameters = null;
    
    // Calculated attributes
    private AlgorithmProfilePredictorAlgorithm predictor = null;
    /**
     * If <code>true</code> the generation/loading of a {@link AlgorithmProfilePredictorAlgorithm} is allowed.
     */
    private boolean sane = false;
    /**
     * If <code>true</code> only the {@link IObservable} 'observed' may change. Other
     * changes are either ignored or lead to a new {@link AlgorithmProfile} instance, to
     * which the profiling attributes are copied over.
     */
    private boolean used = false;
    
    private String key = null;
    
    /**
     * Generates an empty {@link AlgorithmProfile}.
     */
    public AlgorithmProfile() {
    }
    
    /**
     * Generating a {@link AlgorithmProfile} from the given information.
     * @param pipeline Name of the pipeline
     * @param element Name of the pipeline element
     * @param algorithm Name of the algorithm
     * @param predicted The predicted {@link IObservable} and its {@link IObservation}.
     * @param parameters All (other) observed {@link IObservable} and their {@link IObservation}s. 
     */
    public AlgorithmProfile(String pipeline, String element, String algorithm, Map<String, Double> predicted,
            Map<String, Double> parameters) {
        super();
        this.pipeline = pipeline;
        this.element = element;
        this.algorithm = algorithm;
        this.predicted = predicted;
        this.parameters = parameters;
    }

    /** TODO Additional Method to allow parameter values to differ to a given degree.
     * Compares two {@link AlgorithmProfile} instances.
     * @param profile The {@link AlgorithmProfile} instance to compare this instance to.
     * @return true if both instances have the same attributes (pipeline, element, algorithm, predicted 
     * (only the {@link IObservable}) and parameter values).
     */
    public boolean equalTo(AlgorithmProfile profile) {
        // TODO refine
        return this.key.equals(profile.getStringKey());
    }
    /**
     * Dummy.
     * @param profile Dummy
     * @param parameters Dummy
     * @param allowedRelativeDifference Dummy
     * @return Dummy
     */
    @SuppressWarnings("unused")
    private boolean isSimilar(AlgorithmProfile profile, Map<String, Double> parameters, 
            Map<String, Double> allowedRelativeDifference) {
        /* TODO Implement similarity comparison algorithm on basis on the given parameters
         * To be similar they must have the same pipeline, element, algorithm, predicted values and parameters.
         * Also the parameter value must (only) be relatively similar to another according to 
         * the given allowed relative difference for each parameter.
         */
        return false;
    }
    /**
     * Calls generateKey if pipeline, element are algorithm not null and at least one value to predict is set.
     */
    private void updateKey() {
        key = null;
        if (null != pipeline && null != element && null != algorithm && null != predicted) {
            if (predicted.size() > 0) {
                key = generateKey();
            }
        }
    }
    /**
     * Generates a string key (identifier) based on the attributes.
     * @return The key representing this {@link AlgorithmProfile} instance in its current configuration.
     */
    private String generateKey() {
        return "PIPELINE=" + pipeline + ";element=" + element + ";algorithm=" + algorithm
                + ";predicted=" + predicted + ";parameters=" + parameters;
    }
    /**
     * Generates a String identifier using the set attributes.
     * @return String identifier representing this attribute combination. 
     * Can be null if the mandatory atrributes (pipeline, element, algorithm and predicted {@link IObservable}) 
     * are not set.
     */
    public String getStringKey() {
        updateKey();
        return key;
    }
    
    /**
     * This method updates the predictor algorithm with the last known state/measurement for one value.
     * 
     * @param measured Current measurement.
     * @return True if the update was successful.
     */
    // If sane, pass update command and set used, if not already.
    // Else return false.
    public boolean update(double measured) {
        boolean result = false;
        if (sane) {
            preUpdate();
            result = predictor.update(measured);
        }
        return result;
    }
    /**
     * Getter for the currently used Predictor.
     * @return An instance using {@link AlgorithmProfilePredictorAlgorithm}.
     */
    public AlgorithmProfilePredictorAlgorithm getPredictor() {
        return predictor;
    }

    /**
     * This method updates the predictor algorithm with the last known state/measurement for two values.
     * 
     * @param xMeasured Time step of measurement as seconds since midnight, January 1, 1970 UTC.
     * @param yMeasured Current measurement.
     * @return True if the update was successful.
     */
    public boolean update(long xMeasured, double yMeasured) {
        boolean result = false;
        if (sane) {
            preUpdate();
            result = predictor.update(xMeasured, yMeasured);
        }
        return result;
    }
    
    /**
     * Before an update it is made sure that the right {@link AlgorithmProfile} and 
     * the right {@link AlgorithmProfilePredictorAlgorithm} is used.
     */
    private void preUpdate() {
        if (null == predictor) {
            // 1. Try to load it or else TODO
            // 2. (Search for a similar one (postponed TODO) or else )
            // 3. Create new default instance
            predictor = new Kalman();
            
            used = true;
        }
    }
    /**
     * Predict the state of the monitored value for the given number of time steps ahead.
     * 
     * @param steps Number of steps to predict ahead.
     *      <p> steps = 0: Predict one step after the time step of the last update.
     *      <p> steps > 0: Predict X step(s) ahead of 'now'.
     * @return Prediction for one time step ahead of the last update as {@link Double}.
     */
    public double predict(int steps) {
        double result = Double.MIN_VALUE;
        if (used) {
            result = predictor.predict(steps);
        }
        return result;
    }
    /**
     * Predict the state of the monitored value for one time step ahead.
     * 
     * @return Prediction for one time step ahead of the last update as {@link Double}.
     */
    public double predict() {
        return predict(0);
    }
    
    


    /**
     * Sets the currently used pipeline.
     * @param pipeline The currently used pipeline
     */
    public void setPipeline(String pipeline) {
        this.pipeline = pipeline;
        
    }
    /**
     * Sets the currently used pipeline element.
     * @param element The currently used pipeline element
     */
    public void setElement(String element) {
        this.element = element;
        
    }
    /**
     * Sets or updates a parameter with a new value.
     * @param parameter The parameter to set.
     * @param value The value to set the parameter to. Is casted into double.
     */
    public void setParameter(String parameter, Serializable value) {
        parameters.put(parameter, (double) value);
        
    }
    /**
     * Sets the currently used algorithm.
     * @param algorithm The currently used algorithm
     */
    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
        
    }
}
