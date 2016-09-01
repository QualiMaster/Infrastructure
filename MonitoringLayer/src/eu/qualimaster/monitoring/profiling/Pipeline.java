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

import java.util.Map;

import eu.qualimaster.monitoring.MonitoringConfiguration;

/**
 * Captures the actual state of the pipeline.
 * 
 * @author Holger Eichelberger
 */
class Pipeline {
    
    private Map<String, PipelineElement> elements;
    private String name;
    private String path = MonitoringConfiguration.getProfileLocation();
    
    /**
     * Creates a new pipeline instance.
     * 
     * @param name the name of the pipeline
     */
    Pipeline(String name) {
        this.name = name;
    }
    
    /**
     * Returns a known pipeline element with given <code>name</code>.
     * 
     * @param name the name of the pipeline element
     * @return the pipeline element (may be <b>null</b> if it does not exist)
     */
    PipelineElement getElement(String name) {
        return elements.get(name);
    }
    
    /**
     * Returns the name of the pipeline.
     * 
     * @return the name
     */
    String getName() {
        return name;
    }
    
    /**
     * Obtains a pipeline element, i.e., creates a new one if there is none.
     * 
     * @param name the name of the pipeline element
     * @return the pipeline element
     */
    PipelineElement obtainElement(String name) {
        PipelineElement result = elements.get(name);
        if (null == result) {
            result = new PipelineElement(this, name);
            elements.put(name, result);
        }
        return result;
    }
    
    /**
     * Clears this instance.
     */
    void clear()  {
        for (PipelineElement elt : elements.values()) {
            elt.clear(path);
        }
        elements.clear();
    }

    /**
     * Stores this instance.
     */
    void store() {
        for (PipelineElement elt : elements.values()) {
            elt.store(path);
        }
    }
    
    /**
     * Explicitly sets the profile path.
     * 
     * @param path the path
     */
    void setPath(String path) {
        this.path = path;
    }
}