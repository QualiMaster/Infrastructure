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
package eu.qualimaster.monitoring.events;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.qualimaster.common.QMInternal;
import eu.qualimaster.events.AbstractResponseEvent;
import eu.qualimaster.monitoring.profiling.MultiPredictionResult;
import eu.qualimaster.monitoring.profiling.MultiPredictionResult.Prediction;
import eu.qualimaster.observables.IObservable;

/**
 * An event for responding to an algorithm profile prediction request.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public class AlgorithmProfilePredictionResponse extends AbstractResponseEvent<AlgorithmProfilePredictionRequest> {

    private static final long serialVersionUID = 3749223586800239726L;
    private static final String IDENTIFIER_SEPARATOR = "-";

    private double prediction;
    @Deprecated
    private String algorithm;
    private Map<String, Map<IObservable, Double>> massPrediction;
    private Map<String, Map<Object, Serializable>> parameters;
    
    /**
     * Creates the response.
     * 
     * @param request the request
     * @param prediction the predicted value (for algorithm-observation predictions)
     */
    public AlgorithmProfilePredictionResponse(AlgorithmProfilePredictionRequest request, double prediction) {
        super(request);
        this.prediction = prediction;
    }
    
    /**
     * Creates the response.
     * 
     * @param request the request
     * @param algorithm the best algorithm (for algorithm-choice predictions)
     * @deprecated use {@link #AlgorithmProfilePredictionResponse(AlgorithmProfilePredictionRequest, Map)} instead
     */
    @Deprecated
    public AlgorithmProfilePredictionResponse(AlgorithmProfilePredictionRequest request, String algorithm) {
        super(request);
        this.algorithm = algorithm;
    }

    /**
     * Creates the response for a mass-prediction request.
     * 
     * @param request the request
     * @param massPrediction the prediction results, may be <b>null</b>, if there is no prediction the double objects 
     * shall be <b>null</b>
     */
    public AlgorithmProfilePredictionResponse(AlgorithmProfilePredictionRequest request, 
        Map<String, Map<IObservable, Double>> massPrediction) {
        super(request);
        this.massPrediction = massPrediction;
    }

    /**
     * Creates the response for a multi-algorithm mass-prediction request. Here, the response does not use plain
     * algorithm names rather than algorithm-prediction idenfifiers with a numerical prediction index postfix and 
     * fills {@link #getParameters()}. Use {@link #getAlgorithmName()} to retrieve the underlying algorithm name.
     * 
     * @param request the request
     * @param result the multi-prediction result
     */
    public AlgorithmProfilePredictionResponse(AlgorithmProfilePredictionRequest request, MultiPredictionResult result) {
        super(request);
        // rewrite result so that it fits to existing interfaces / weighting functions
        massPrediction = new HashMap<String, Map<IObservable, Double>>();
        for (String alg : result.algorithms()) {
            List<Prediction> predictions = result.getPredictions(alg);
            for (int i = 0; i < predictions.size(); i++) {
                Prediction pred = predictions.get(i);
                String identifier = getAlgorithmIdentifier(alg, i);
                massPrediction.put(identifier, pred.getResult());
                if (null == parameters) {
                    parameters = new HashMap<>();
                }
                parameters.put(identifier, pred.getParameters());
            }
        }
    }
    
    /**
     * Returns an algorithm identifier.
     * 
     * @param algorithmName the algorithm name
     * @param index an index value indicating different predictions for the same name
     * @return the algorithm identifier
     */
    public static String getAlgorithmIdentifier(String algorithmName, int index) {
        return algorithmName + IDENTIFIER_SEPARATOR + index;
    }
    
    /**
     * Turns an algorithm-prediction identifier used in multi-algorithm mass-predictions to the original algorithm name.
     * 
     * @param identifier the algorithm-prediction identifier
     * @return the algorithm name
     */
    public static String getAlgorithmName(String identifier) {
        String result = identifier;
        int pos = result.lastIndexOf(IDENTIFIER_SEPARATOR);
        if (pos > 0) {
            result = result.substring(0, pos);
        }
        return result;
    }
    
    /**
     * The predicted value.
     * 
     * @return the predicted value.
     */
    public double getPrediction() {
        return prediction;
    }
    
    /**
     * The "best" algorithm.
     * 
     * @return the best algorithm (may be <b>null</b>)
     * @deprecated
     */
    @Deprecated
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * Returns the result for a mass-prediction request.
     * 
     * @return the prediction results, may be <b>null</b>, if there is no prediction the double objects 
     *     shall be <b>null</b>. In case of a multi-algorithm prediction, the algorithm names are algorithm-prediction 
     *     identifiers postfixed by a prediction id ({@link #getAlgorithmName(String)}.
     */
    public Map<String, Map<IObservable, Double>> getMassPrediction() {
        return massPrediction;
    }
    
    /**
     * Returns the parameters, but only in multi-algorithm mass predictions.
     * 
     * @return the parameters (containing algorithm-prediction identifiers, see {@link #getAlgorithmName(String)}), 
     *     may be <b>null</b>
     */
    public Map<String, Map<Object, Serializable>> getParameters() {
        return parameters;
    }
    
}
