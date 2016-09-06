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

import eu.qualimaster.monitoring.profiling.predictors.IAlgorithmProfilePredictor;
import eu.qualimaster.monitoring.profiling.predictors.Kalman;

/**
 * Creates profiles and related predictors for the actual Kalman-based approach.
 * 
 * @author Holger Eichelberger
 */
public class KalmanProfileCreator implements IAlgorithmProfileCreator {

    @Override
    public IAlgorithmProfilePredictor createPredictor() {
        return new Kalman();
    }

    @Override
    public IAlgorithmProfile createProfile(PipelineElement element, Map<Object, Serializable> key) {
        return new SeparateObservableAlgorithmProfile(element, key);
    }

}
