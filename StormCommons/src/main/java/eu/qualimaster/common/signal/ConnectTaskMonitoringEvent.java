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
package eu.qualimaster.common.signal;

import java.io.Serializable;

import eu.qualimaster.common.QMInternal;
import eu.qualimaster.monitoring.events.AbstractPipelineElementMonitoringEvent;

/**
 * An event sent to indicate the creation/connect of a task to the infrastructure. The monitoring layer can use
 * this event to detect whether a task/worker was restarted (new key with same task id is being connected).
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public class ConnectTaskMonitoringEvent extends AbstractPipelineElementMonitoringEvent {

    private static final long serialVersionUID = -7128804008518291862L;

    /**
     * Creates a connect event with an id to be printed out.
     * 
     * @param pipeline pipeline the pipeline name
     * @param pipelineElement pipeline element the name of the pipeline element
     * @param key the aggregation component key (may be <b>null</b>)
     */
    public ConnectTaskMonitoringEvent(String pipeline, String pipelineElement, Serializable key) {
        super(pipeline, pipelineElement, key);
    }

}
