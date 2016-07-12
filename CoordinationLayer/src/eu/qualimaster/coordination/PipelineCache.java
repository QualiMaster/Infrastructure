/*
 * Copyright 2009-2015 University of Hildesheim, Software Systems Engineering
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
package eu.qualimaster.coordination;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.qualimaster.common.signal.ParameterChange;
import eu.qualimaster.coordination.commands.AbstractPipelineElementCommand;

/**
 * Stores runtime information about a pipeline to ease command handling.
 * 
 * @author Holger Eichelberger
 */
public class PipelineCache {

    private static final Map<String, PipelineCache> CACHES = Collections.synchronizedMap(
        new HashMap<String, PipelineCache>());
    private Map<String, PipelineElementCache> elements = new HashMap<String, PipelineElementCache>();
    
    /**
     * Implements a cache for a pipeline element.
     * 
     * @author Holger Eichelberger
     */
    public static class PipelineElementCache {
        
        private Map<String, Serializable> parameters = new HashMap<String, Serializable>();
        private String algorithm;
    
        /**
         * Creates a pipeline cache.
         */
        private PipelineElementCache() {
        }
        
        /**
         * Changes the cached algorithm.
         * 
         * @param algorithm the algorithm name
         */
        public void setAlgorithm(String algorithm) {
            setAlgorithm(algorithm, null);
        }
        
        /**
         * Changes the cached algorithm.
         * 
         * @param algorithm the algorithm name
         * @param parameters the parameters enacted with the algorithm (existing will be discarded, may be <b>null</b>)
         * 
         * @see #setParameters(List, boolean)
         */
        public void setAlgorithm(String algorithm, List<ParameterChange> parameters) {
            this.algorithm = algorithm;
            setParameters(parameters, true);
        }

        
        /**
         * Returns the cached algorithm name.
         * 
         * @return the algorithm name
         */
        public String getAlgorithm() {
            return algorithm;
        }
        
        /**
         * Returns whether this cache knows <code>parameter</code>.
         * 
         * @param parameter the parameter name to search for
         * @return <code>true</code> if the parameter exists, <code>false</code> else
         */
        public boolean hasParameter(String parameter) {
            return parameters.containsKey(parameter);
        }
        
        /**
         * Returns the parameter value.
         * 
         * @param parameter the parameter name
         * @return the value (<b>null</b> if the entry does not exist, see {@link #hasParameter(String)})
         */
        public Serializable getParameterValue(String parameter) {
            return parameters.get(parameter);
        }
        
        /**
         * Changes the actual parameter value.
         * 
         * @param parameter the parameter
         * @param value the value
         */
        public void setParameter(String parameter, Serializable value) {
            parameters.put(parameter, value);
        }

        /**
         * Changes all parameters.
         * 
         * @param parameters the parameters to set (may be <b>null</b>, ignored)
         * @param merge if the given values shall be just stored (<code>false</code>) or replace existing values
         */
        public void setParameters(List<ParameterChange> parameters, boolean merge) {
            if (null != parameters) {
                if (!merge) {
                    this.parameters.clear();
                }
                for (ParameterChange change : parameters) {
                    this.parameters.put(change.getName(), change.getValue());
                }
            }
        }

        /**
         * Returns the actual parameter settings as changes.
         * 
         * @return the actual parameter settings
         */
        public List<ParameterChange> parameters() {
            List<ParameterChange> result = new ArrayList<ParameterChange>();
            for (Map.Entry<String, Serializable> entry : parameters.entrySet()) {
                result.add(new ParameterChange(entry.getKey(), entry.getValue()));
            }
            return result;
        }
        
    }

    /**
     * Returns the requested cache element.
     *  
     * @param element the element to return the cache element for
     * @return the cache element
     */
    public synchronized PipelineElementCache getElement(String element) {
        PipelineElementCache result = elements.get(element);
        if (null == result) {
            result = new PipelineElementCache();
            elements.put(element, result);
        }
        return result;
    }
    
    /**
     * Returns a pipeline cache.
     * 
     * @param name the name of the pipeline
     * @return the cache
     */
    static PipelineCache getCache(String name) {
        PipelineCache cache = CACHES.get(name);
        if (null == cache) {
            cache = new PipelineCache();
            CACHES.put(name, cache);
        }
        return cache;
    }
    
    /**
     * Returns a pipeline element cache for the given <code>command</code>.
     * 
     * @param command the command
     * @return the pipeline element cache
     */
    static PipelineElementCache getCache(AbstractPipelineElementCommand command) {
        return getCache(command.getPipeline()).getElement(command.getPipelineElement());
    }
    
    /**
     * Removes the given pipeline cache.
     * 
     * @param name the name of the pipeline
     */
    static void removeCache(String name) {
        CACHES.remove(name);
    }

}
