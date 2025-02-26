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
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import eu.qualimaster.monitoring.profiling.approximation.IStorageStrategy;
import eu.qualimaster.monitoring.profiling.predictors.IAlgorithmProfilePredictor;
import eu.qualimaster.observables.IObservable;

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
    
    /**
     * Returns the sub-folder within the storage path for this combination of predictor and profile.
     * 
     * @return the sub-folder
     */
    public String getStorageSubFolder();

    /**
     * Returns all known parameter values.
     * 
     * @param element the element to return the values for
     * @param key the profile key
     * @param observable the observable
     * @param parameter the parameter name
     * @return the parameter values (here as strings regardless of type)
     * @throws IOException in case that the map file of the profile cannot be loaded
     */
    public List<String> getKnownParameterValues(PipelineElement element, Map<Object, Serializable> key, 
        IObservable observable, String parameter) throws IOException;

    /**
     * Returns the storage strategy for elements within a profile.
     * 
     * @return the storage strategy
     */
    public IStorageStrategy getStorageStrategy();
    
    /**
     * Returns the path including the predictor folder.
     *
     * @param pipeline the pipeline name
     * @param element the pipeline element name
     * @param algorithm the algorithm name
     * @param path the base path
     * @param observable the observable to be predicted (may be <b>null</b> then the path stops there)
     * @return the path to the predictor folder
     */
    public File getPredictorPath(String pipeline, String element, String algorithm, String path, 
        IObservable observable);
    
    /**
     * Returns the (profiling) path including the predictor folder.
     *
     * @param algorithm the algorithm name
     * @param path the base path
     * @param observable the observable to be predicted (may be <b>null</b> then the path stops there)
     * @return the path to the predictor folder
     */
    public File getPredictorPath(String algorithm, String path, IObservable observable);
    
}
