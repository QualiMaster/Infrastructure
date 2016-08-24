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

import java.util.Map;

import eu.qualimaster.common.QMInternal;
import eu.qualimaster.events.AbstractResponseEvent;

/**
 * An event for responding to a source volume prediction request.
 * 
 * @author Holger Eichelberger
 * @author Andrea Ceroni
 */
@QMInternal
public class SourceVolumePredictionResponse extends AbstractResponseEvent<SourceVolumePredictionRequest> {

    private static final long serialVersionUID = -4501288532194129558L;
    private Map<String, Double> predictions;
    
    /**
     * Creates a response event.
     * 
     * @param request the request event
     * @param predictions the predictions made for <code>request</code>
     */
    public SourceVolumePredictionResponse(SourceVolumePredictionRequest request, Map<String, Double> predictions) {
        super(request);
        this.predictions = predictions;
    }
    
    /**
     * Returns the predictions as a keyword-prediction map.
     * 
     * @return the predictions
     */
    public Map<String, Double> getPredictions() {
        return predictions;
    }

}
