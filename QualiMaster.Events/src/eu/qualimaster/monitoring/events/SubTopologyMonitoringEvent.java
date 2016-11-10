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

import java.util.List;
import java.util.Map;

import eu.qualimaster.common.QMInternal;
import eu.qualimaster.infrastructure.IScalingDescriptor;

/**
 * Allows to send the monitoring layer information about substructures and their mapping
 * to processing elements collected at runtime, more precisely while building up a pipeline.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public class SubTopologyMonitoringEvent extends AbstractPipelineMonitoringEvent {

    public static final String SEPARATOR = ";";
    private static final long serialVersionUID = -499709262081421544L;
    private Map<String, List<String>> structure;
    private Map<String, IScalingDescriptor> descriptors;
    
    /**
     * Creates a monitoring event.
     * 
     * @param pipeline the pipeline name
     * @param structure a processing id - sub processing id mapping
     * @param descriptors the scaling descriptors (may be <b>null</b> or contain mapping to <b>null</b>)
     */
    public SubTopologyMonitoringEvent(String pipeline, Map<String, List<String>> structure, 
        Map<String, IScalingDescriptor> descriptors) {
        super(pipeline);
        this.structure = structure;
        this.descriptors = descriptors;
    }
    
    /**
     * Returns the contained structure.
     * 
     * @return the structure (processing id - sub processing id / class name mapping)
     */
    public Map<String, List<String>> getStructure() {
        return structure;
    }
    
    /**
     * Returns the scaling descriptors.
     * 
     * @return the scaling descriptors (may be <b>null</b> or contain mapping to <b>null</b>)
     */
    public Map<String, IScalingDescriptor> getScalingDescriptors() {
        return descriptors;
    }

}
