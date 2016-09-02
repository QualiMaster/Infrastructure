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

/**
 * Creates algorithm profile predictors.
 * 
 * @author Holger Eichelberger
 */
public interface IAlgorithmProfileCreator {

    /**
     * Creates a predictor instance.
     * 
     * @return the predictor instance
     */
    public IAlgorithmProfilePredictor createPredictor();

    /**
     * Create a profile instance.
     * 
     * @param element the pipeline element
     * @param key the profile key
     * @return the profile instance
     */
    public IAlgorithmProfile createProfile(PipelineElement element, Map<Object, Serializable> key);
    
}
