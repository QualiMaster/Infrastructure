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
package eu.qualimaster.monitoring.profiling.approximation;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

import eu.qualimaster.monitoring.profiling.PipelineElement;
import eu.qualimaster.observables.IObservable;

/**
 * Defines the interface for storing elements within a strategy to be used by profiles and approximators.
 * 
 * @author Holger Eichelberger
 */
public interface IStorageStrategy {
    
    /**
     * Returns the path including the predictor folder.
     * 
     * @param element the holding pipeline element
     * @param path the base path
     * @param key the profile key
     * @param observable the observable to be predicted
     * @return the path to the predictor folder
     */
    public File getPredictorPath(PipelineElement element, String path, Map<Object, Serializable> key, 
        IObservable observable);
    
    /**
     * Returns the path for the approximators.
     * 
     * @param element the holding pipeline element
     * @param path the base path
     * @param key the profile key
     * @return the path to the approximators
     */
    public File getApproximatorsPath(PipelineElement element, String path, Map<Object, Serializable> key);

    /**
     * Generates an algorithm profile identifier key.
     * 
     * @param element the holding pipeline element
     * @param key the profile key (may be <b>null</b> if the algorithm part/following parameters are not needed)
     * @param observable the observable to be predicted (may be <b>null</b> if the observable/following parameters
     *    are not needed)
     * @param includeParameters include the parameters into the key
     * 
     * @return The key representing this an algorithm profile instance in its 
     *     current configuration.
     */
    public String generateKey(PipelineElement element, Map<Object, Serializable> key, IObservable observable, 
        boolean includeParameters);

}
