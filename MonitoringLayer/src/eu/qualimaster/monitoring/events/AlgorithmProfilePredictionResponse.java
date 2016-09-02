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

import eu.qualimaster.common.QMInternal;
import eu.qualimaster.events.AbstractResponseEvent;

/**
 * An event for responding to an algorithm profile prediction request.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public class AlgorithmProfilePredictionResponse extends AbstractResponseEvent<AlgorithmProfilePredictionRequest> {

    private static final long serialVersionUID = 3749223586800239726L;

    private double prediction;
    private String algorithm;
    
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
     */
    public AlgorithmProfilePredictionResponse(AlgorithmProfilePredictionRequest request, String algorithm) {
        super(request);
        this.algorithm = algorithm;
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
     */
    public String getAlgorithm() {
        return algorithm;
    }

}
