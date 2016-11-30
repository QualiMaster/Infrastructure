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

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import eu.qualimaster.monitoring.profiling.approximation.IStorageStrategy;
import eu.qualimaster.monitoring.profiling.predictors.IAlgorithmProfilePredictor;
import eu.qualimaster.monitoring.profiling.predictors.Kalman;
import eu.qualimaster.observables.IObservable;

/**
 * Creates profiles and related predictors for the actual Kalman-based approach.
 * 
 * @author Holger Eichelberger
 */
public class KalmanProfileCreator extends AbstractAlgorithmProfileCreator {

    @Override
    public IAlgorithmProfilePredictor createPredictor() {
        return new Kalman();
    }

    @Override
    public IAlgorithmProfile createProfile(PipelineElement element, Map<Object, Serializable> key) {
        return new SeparateObservableAlgorithmProfile(element, key);
    }

    @Override
    public String getStorageSubFolder() {
        return "predictor=kalman";
    }

    @Override
    public List<String> getKnownParameterValues(PipelineElement element, Map<Object, Serializable> key, 
        IObservable observable, String parameter) throws IOException {
        return SeparateObservableAlgorithmProfile.getKnownParameterValues(element, key, observable, parameter);
    }

    @Override
    public IStorageStrategy getStorageStrategy() {
        return DefaultStorageStrategy.INSTANCE;
    }

}
