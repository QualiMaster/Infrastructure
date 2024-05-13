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
package eu.qualimaster.observables;

import java.util.HashMap;
import java.util.Map;

import eu.qualimaster.common.QMSupport;

/**
 * The kind of monitoring frequencies supported by the infrastructure.
 * 
 * @author Holger Eichelberger
 */
@QMSupport
public enum MonitoringFrequency {

    /**
     * The global (pipeline-independent) cluster monitoring frequency.
     */
    CLUSTER_MONITORING,

    /**
     * The global pipeline-specific monitoring frequency.
     */
    PIPELINE_MONITORING,

    /**
     * The pipeline node (processing) monitoring frequency.
     */
    PIPELINE_NODE,

    /**
     * The pipeline source volume aggregation monitoring frequency.
     */
    SOURCE_AGGREGATION,
    
    /**
     * The pipeline resource monitoring frequency.
     */
    PIPELINE_NODE_RESOURCES;

    /**
     * Creates a single frequencies map.
     * 
     * @param freq the frequency type
     * @param val the value
     * @return the frequency map
     */
    public static Map<MonitoringFrequency, Integer> createMap(MonitoringFrequency freq, int val) {
        Map<MonitoringFrequency, Integer> result = new HashMap<MonitoringFrequency, Integer>();
        result.put(freq, val);
        return result;
    }

    /**
     * Creates a frequencies map with one value for all frequencies.
     * 
     * @param val the value
     * @return the frequency map
     */
    public static Map<MonitoringFrequency, Integer> createAllMap(int val) {
        Map<MonitoringFrequency, Integer> result = new HashMap<MonitoringFrequency, Integer>();
        for (MonitoringFrequency freq : values()) {
            result.put(freq, val);
        }
        return result;
    }

}
