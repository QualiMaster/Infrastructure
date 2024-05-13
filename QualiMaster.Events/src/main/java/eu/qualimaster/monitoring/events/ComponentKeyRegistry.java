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
package eu.qualimaster.monitoring.events;

import java.util.HashMap;
import java.util.Map;

/**
 * A registry for component keys. Assumes that each JVM (except for those running the infrastructure) 
 * is dedicated 
 * @author Holger Eichelberger
 *
 */
public class ComponentKeyRegistry {

    private static String pipelineName = "";
    private static final Map<Long, ComponentKey> COMPONENTS = new HashMap<Long, ComponentKey>();

    /**
     * Registers the given component. Call in component prepare/open.
     * 
     * @param pipeline the actual pipeline name (one pipeline per JVM)
     * @param component the component to unregister (ignored if <b>null</b>)
     * @param key the component key (ignored if <b>null</b>)
     */
    public static void register(String pipeline, Object component, ComponentKey key) {
        pipelineName = pipeline;
        if (null != component && null != key) {
            COMPONENTS.put((long) System.identityHashCode(component), key);
        }
    }
    
    /**
     * Unregisters the given component. Call in component shutdown
     * 
     * @param component the component to unregister (ignored if <b>null</b>)
     */
    public static void unregister(Object component) {
        if (null != component) {
            COMPONENTS.remove((long) System.identityHashCode(component));
        }        
    }
    
    /**
     * Returns the registered component key for the given identity hash code.
     * 
     * @param instanceKey the identifying identity key (shall be unique within this JVM)
     * @return the registered component key (may be <b>null</b>)
     */
    public static ComponentKey getRegisteredComponentKey(long instanceKey) {
        return COMPONENTS.get(instanceKey);
    }

    /**
     * Returns the name of the running pipeline name.
     * 
     * @return the name of the pipeline
     */
    public static String getPipelineName() {
        return pipelineName;
    }

}
