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
package eu.qualimaster.monitoring.events;

import java.util.Map;

import eu.qualimaster.adaptation.events.AdaptationEvent;
import eu.qualimaster.common.QMInternal;
import eu.qualimaster.infrastructure.InfrastructurePart;

/**
 * Implements an adaptation event indicating a change of (available) resources.
 * 
 * @author Holger Eichelberger
 */
public class ResourceChangedAdaptationEvent extends AdaptationEvent {
    
    private static final long serialVersionUID = 5211369499337791593L;
    private InfrastructurePart part;
    private String name;
    private Map<String, Double> oldValues;
    private Map<String, Double> newValues;
    
    /**
     * Implements an adaptation event indicating a change of (available) resources.
     * 
     * @param part the affected infrastructure part
     * @param name the name of the resource
     * @param oldValues the old values characterizing the resource before the change
     * @param newValues the new values characterizing the resource after the change
     */
    @QMInternal
    public ResourceChangedAdaptationEvent(InfrastructurePart part, String name, Map<String, Double> oldValues, 
        Map<String, Double> newValues) {
        this.part = part;
        this.name = name;
        this.oldValues = oldValues;
        this.newValues = newValues;
    }

    /**
     * Returns the infrastructure part make the resource name unique.
     * 
     * @return the infrastructure part
     */
    public InfrastructurePart getPart() {
        return part;
    }

    /**
     * Returns the resource name.
     * 
     * @return the resource name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the old value registered for <code>key</code> before the change.
     * 
     * @param key a key representing the value
     * @return the old value (may be <b>null</b> if undefined)
     */
    public Double getOldValue(String key) {
        return null == oldValues ? null : oldValues.get(key);
    }

    /**
     * Returns the new value registered for <code>key</code> after the change.
     * 
     * @param key a key representing the value
     * @return the new value (may be <b>null</b> if undefined)
     */
    public Double getNewValue(String key) {
        return null == newValues ? null : newValues.get(key);
    }

}
