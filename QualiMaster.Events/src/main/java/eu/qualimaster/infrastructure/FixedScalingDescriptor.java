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
package eu.qualimaster.infrastructure;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of a fixed scaling descriptor, i.e., no scaling will happen. Same tasks as executors are assumed.
 * 
 * @author Holger Eichelberger
 */
public class FixedScalingDescriptor implements IScalingDescriptor {

    private static final long serialVersionUID = 5683966695501335998L;
    private Map<String, Integer> executors = new HashMap<String, Integer>();
    
    /**
     * Creates an empty scaling descriptor.
     * 
     * @see #setExecutor(String, int)
     */
    public FixedScalingDescriptor() {
    }

    /**
     * Creates a scaling descriptor with given component-executor mapping.
     * 
     * @param executors the executors
     */
    public FixedScalingDescriptor(Map<String, Integer> executors) {
        if (null != executors) {
            this.executors.putAll(executors);
        }
    }
    
    /**
     * Defines/changes the executors for a given component.
     * 
     * @param name the component name
     * @param executors the number of executors
     */
    public void setExecutor(String name, int executors) {
        if (null != name) {
            this.executors.put(name, executors);
        }
    }

    @Override
    public Map<String, Integer> getScalingResult(double factor, boolean executors) {
        return this.executors; // no scaling, just ignore; assuming tasks=executors
    }

    @Override
    public Map<String, Integer> getScalingResult(int oldExecutors, int newExecutors, boolean diffs) {
        Map<String, Integer> result;
        if (diffs) {
            result = new HashMap<String, Integer>(); // no changes
        } else {
            result = this.executors; // no scaling, just ignore
        }
        return result; 
    }

}
