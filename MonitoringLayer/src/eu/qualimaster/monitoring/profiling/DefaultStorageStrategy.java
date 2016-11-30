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
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import eu.qualimaster.monitoring.profiling.approximation.IStorageStrategy;
import eu.qualimaster.observables.IObservable;

/**
 * Implements the default storage strategy using hierarchical paths based on the profile key.
 * 
 * @author Christopher Voges
 * @author Holger Eichelberger
 */
class DefaultStorageStrategy implements IStorageStrategy {

    public static final IStorageStrategy INSTANCE = new DefaultStorageStrategy();
    
    /**
     * Prevents external instantiation.
     */
    private DefaultStorageStrategy() {
    }
    
    @Override
    public File getPredictorPath(PipelineElement element, String path, Map<Object, Serializable> key, 
        IObservable observable) {
        String identifier = generateKey(element, key, observable, false);
        return getPredictorPath(identifier, path, element.getProfileCreator());
    }

    /**
     * Returns the path including the predictor folder.
     * 
     * @param identifier the internal profile identifier
     * @param path the base path
     * @param creator the creator carrying the predictor storage sub folder (ignored if <b>null</b>)
     * @return the path to the predictor folder
     */
    private File getPredictorPath(String identifier, String path, IAlgorithmProfileCreator creator) {
        File folder = new File(path);
        // Get subfolder from nesting information 
        String[] nesting = identifier.split(":");
        for (String string : nesting) {
            folder = new File(folder, string);
        }
        File result = folder;
        if (null != creator) {
            result = new File(result, creator.getStorageSubFolder());
        }
        return result;
    }

    @Override
    public File getApproximatorsPath(PipelineElement element, String path, Map<Object, Serializable> key) {
        String identifier = generateKey(element, key, null, false);
        File folder = new File(path);
        // Get subfolder from nesting information 
        String[] nesting = identifier.split(":");
        for (String string : nesting) {
            folder = new File(folder, string);
        }
        return new File(folder, "approximators");
    }

    @Override
    public String generateKey(PipelineElement element, Map<Object, Serializable> key, IObservable observable, 
        boolean includeParameters) {
        boolean profiling = element.isInProfilingMode();
        String pipelineName = element.getPipeline().getName();
        String elementName = element.getName();
        return generateKey(pipelineName, elementName, key, observable, profiling, includeParameters);
    }

    @Override
    public File getPredictorPath(String pipeline, String element, String algorithm, String path, IObservable observable,
        IAlgorithmProfileCreator creator) {
        Map<Object, Serializable> key = new HashMap<Object, Serializable>();
        key.put(Constants.KEY_ALGORITHM, algorithm);
        String identifier = generateKey(pipeline, element, key, observable, null == pipeline || null == element, false);
        return getPredictorPath(identifier, path, null == observable ? null : creator);
    }

    // checkstyle: stop parameter number check

    /**
     * Generates a string key (identifier).
     * 
     * @param pipelineName the pipeline name 
     * @param elementName the pipeline element name
     * @param key the profile key (may be <b>null</b> if the algorithm part/following parameters are not needed)
     * @param observable the observable to be predicted (may be <b>null</b> if the observable/following parameters
     *    are not needed)
     * @param profiling are we in profile mode
     * @param includeParameters include the parameters into the key
     * 
     * @return The key representing this {@link SeparateObservableAlgorithmProfile} instance in its 
     *     current configuration.
     */
    private String generateKey(String pipelineName, String elementName, Map<Object, Serializable> key, 
        IObservable observable, boolean profiling, boolean includeParameters) {
        String result;
        if (profiling) {
            result = "";
        } else {
            result = "pipeline=" + pipelineName + ":element=" + elementName + ":";
        }
        if (null != key) {
            String algorithm = keyToString(key, Constants.KEY_ALGORITHM);
            result += "algorithm=" + algorithm;
        }
        if (null != observable) {
            result += ":predicted=" + observable.name();
        }
        if (includeParameters) {
            TreeMap<String, String> sorted = new TreeMap<>();
            for (Map.Entry<Object, Serializable> ent : key.entrySet()) {
                String k = ent.getKey().toString();
                if (!Constants.KEY_ALGORITHM.equals(k)) {
                    sorted.put(k, ent.getValue().toString());
                }
            }
            result += ";parameters=" + sorted;    
        }
        return result;
    }

    // checkstyle: resume parameter number check

    /**
     * Turns a key part into a string.
     * 
     * @param key the key
     * @param part the key part
     * @return the string representation
     */
    private static String keyToString(Map<Object, Serializable> key, Object part) {
        Serializable tmp = key.get(part);
        return null == tmp ? "" : tmp.toString();
    }

}
