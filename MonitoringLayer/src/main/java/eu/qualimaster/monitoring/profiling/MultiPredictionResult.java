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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.qualimaster.observables.IObservable;

/**
 * The result of predicting over algorithms and parameters.
 * 
 * @author Holger Eichelberger
 */
public class MultiPredictionResult {

    private Map<String, List<Prediction>> results = new HashMap<String, List<Prediction>>();

    /**
     * Represents a single mass prediction for an algorithm.
     * 
     * @author Holger Eichelberger
     */
    public static class Prediction {
        private Map<Object, Serializable> param;
        private Map<IObservable, Double> result;

        /**
         * Creates the prediction.
         * 
         * @param param the parameter values of the profile determining the prediction
         * @param result the prediction result for algorithm-param for the queried pipeline/element
         */
        private Prediction(Map<Object, Serializable> param, Map<IObservable, Double> result) {
            this.param = param;
            this.result = result;
        }
        
        /**
         * Returns the parameters characterizing the profile.
         * 
         * @return the parameters
         */
        public Map<Object, Serializable> getParameters() {
            return param;
        }
        
        /**
         * Returns the prediction result.
         * 
         * @return the result
         */
        public Map<IObservable, Double> getResult() {
            return result;
        }
        
        @Override
        public String toString() {
            return param + " -> " + result;
        }
        
    }
    
    /**
     * Adds a prediction result to this instance. [public for testing]
     * 
     * @param algorithm the name of the algorithm
     * @param param the parameter values of the profile determining the prediction
     * @param result the prediction result for algorithm-param for the queried pipeline/element
     */
    public void add(String algorithm, Map<Object, Serializable> param, Map<IObservable, Double> result) {
        List<Prediction> predictions = results.get(algorithm);
        if (null == predictions) {
            predictions = new ArrayList<Prediction>();
            results.put(algorithm, predictions);
        }
        predictions.add(new Prediction(param, result));
    }
    
    /**
     * Returns all contained algorithms.
     * 
     * @return all contained algorithms
     */
    public Set<String> algorithms() {
        return results.keySet();
    }
    
    /**
     * Returns all predictions for the given algorithm.
     * 
     * @param algorithm the algorithm
     * @return the predictions (may be <b>null</b> if there are none)
     */
    public List<Prediction> getPredictions(String algorithm) {
        return results.get(algorithm);
    }

    @Override
    public String toString() {
        return results.toString();
    }

}
