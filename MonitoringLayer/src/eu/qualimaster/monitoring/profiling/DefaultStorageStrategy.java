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
        File folder = new File(path);
        // Get subfolder from nesting information 
        String[] nesting = identifier.split(":");
        for (String string : nesting) {
            folder = new File(folder, string);
        }
        // set kind of the predictor as subfolder
        String subfolder = element.getProfileCreator().getStorageSubFolder();
        return new File(folder, subfolder);
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

    /**
     * Generates a string key (identifier).
     * 
     * @param element the holding pipeline element
     * @param key the profile key (may be <b>null</b> if the algorithm part/following parameters are not needed)
     * @param observable the observable to be predicted (may be <b>null</b> if the observable/following parameters
     *    are not needed)
     * @param includeParameters include the parameters into the key
     * 
     * @return The key representing this {@link SeparateObservableAlgorithmProfile} instance in its 
     *     current configuration.
     */
    @Override
    public String generateKey(PipelineElement element, Map<Object, Serializable> key, IObservable observable, 
        boolean includeParameters) {
        boolean profiling = element.isInProfilingMode();
        String pipelineName = element.getPipeline().getName();
        String elementName = element.getName();

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
