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

import java.util.Map;

import eu.qualimaster.common.QMInternal;

/**
 * A specialized event to be sent only by sources informing the infrastructure about the aggregated occurrences of
 * source-specific keys.
 * 
 * @author Holger Eichelberger
 * @author Andrea Ceroni
 */
@QMInternal
public class SourceVolumeMonitoringEvent extends AbstractPipelineElementMonitoringEvent {

    private static final long serialVersionUID = 1510429874478741585L;
    private Map<String, Integer> observations;

    /**
     * Creates a source volume monitoring event.
     * 
     * @param pipeline the pipeline name
     * @param sourceElement the name of the sending source 
     * @param observations the observations as a mapping of keys to the number of occurrences.
     */
    public SourceVolumeMonitoringEvent(String pipeline, String sourceElement, Map<String, Integer> observations) {
        super(pipeline, sourceElement, null);
        this.observations = observations;
    }
    
    /**
     * Returns the observations in this event. Contains a mapping of source-specific keys to number of occurrences.
     * 
     * @return the observations (the direct reference!)
     */
    public Map<String, Integer> getObservations() {
        return observations;
    }

}
