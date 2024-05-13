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
package eu.qualimaster.pipeline;

import eu.qualimaster.common.QMInternal;
import eu.qualimaster.monitoring.events.AbstractPipelineElementMonitoringEvent;

/**
 * An event to be issued by a pipeline if processing switched in there to default mode 
 * (actually caused by a {@link DefaultModeException}.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public class DefaultModeMonitoringEvent extends AbstractPipelineElementMonitoringEvent {

    private static final long serialVersionUID = 3914209306134354301L;
    private String cause;

    /**
     * Creates a default mode monitoring event for a given cause.
     * 
     * @param pipeline the pipeline switching to default mode
     * @param pipelineElement the pipeline element within <code>pipeline</code> switch to default mode
     * @param cause the cause for the switch
     */
    public DefaultModeMonitoringEvent(String pipeline, String pipelineElement, String cause) {
        super(pipeline, null, pipelineElement);
        this.cause = cause;
    }

    /**
     * Creates a default mode monitoring event from a {@link DefaultModeException}.
     * 
     * @param pipeline the pipeline switching to default mode
     * @param pipelineElement the pipeline element within <code>pipeline</code> switch to default mode
     * @param cause the exception causing the switch
     * 
     * @see #DefaultModeMonitoringEvent(String, String, String)
     */
    public DefaultModeMonitoringEvent(String pipeline, String pipelineElement, DefaultModeException cause) {
        this(pipeline, pipelineElement, cause.getMessage());
    }

    /**
     * Returns the cause for the default mode event.
     * 
     * @return the cause
     */
    public String getCause() {
        return cause;
    }

}
