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

import eu.qualimaster.monitoring.profiling.IAlgorithmProfileCreator;
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
     * @param key the profile key (may be <b>null</b>
     * @param observable the observable to be predicted
     * @return the path to the predictor folder
     */
    public File getPredictorPath(PipelineElement element, String path, Map<Object, Serializable> key, 
        IObservable observable);
    
    // checkstyle: stop parameter number check
    
    /**
     * Returns the path including the predictor folder.
     *
     * @param pipeline the pipeline name (profiling mode if <b>null</b>)
     * @param element the pipeline element name (profiling mode if <b>null</b>)
     * @param algorithm the algorithm name
     * @param path the base path
     * @param observable the observable to be predicted
     * @param creator the profile creator instance
     * @return the path to the predictor folder
     */
    public File getPredictorPath(String pipeline, String element, String algorithm, String path, IObservable observable,
        IAlgorithmProfileCreator creator);
    
    // checkstyle: resume parameter number check

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

    /**
     * Returns whether <code>folder</code> is an approximators folder.
     * 
     * @param file the file to test
     * @return <code>true</code> if it is an approximators folder, <code>false</code> else
     */
    public boolean isApproximatorsFolder(File file);
    
    /**
     * Returns the name of the map file.
     * 
     * @return the name of the map file
     */
    public String getMapFileName();

    /**
     * Returns the file name for this approximator (without suffix).
     * 
     * @param parameterName the parameter name
     * @param observable the observable
     * @param suffix (may already be prepended by a ".")
     * @return the file name without suffix
     */
    public String getApproximatorFileName(Object parameterName, IObservable observable, String suffix);

    /**
     * Carries information about creating an approximator.
     * 
     * @author Holger Eichelberger
     */
    public class ApproximatorInfo {
        
        private Object parameterName;
        private IObservable observable;
        
        /**
         * Creates an instance.
         * 
         * @param parameterName the parameter name
         * @param observable the observable
         */
        public ApproximatorInfo(Object parameterName, IObservable observable) {
            this.parameterName = parameterName;
            this.observable = observable;
        }
        
        /**
         * Returns the parameter name.
         * 
         * @return the parameter name
         */
        public Object getParameterName() {
            return parameterName;
        }
        
        /**
         * Returns the observable.
         * 
         * @return the observable
         */
        public IObservable getObservable() {
            return observable;
        }
    }
    
    /**
     * Parses an approximator file name.
     * 
     * @param fileName the file name
     * @return the approximator information
     */
    public ApproximatorInfo parseApproximatorFileName(String fileName);
    
}
