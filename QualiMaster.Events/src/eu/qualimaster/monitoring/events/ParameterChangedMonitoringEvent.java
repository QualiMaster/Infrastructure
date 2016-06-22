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

import java.io.Serializable;

import eu.qualimaster.common.QMInternal;

/**
 * Informs about the actual change of a parameter.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public class ParameterChangedMonitoringEvent extends AbstractPipelineElementEnactmentCompletedMonitoringEvent {

    private static final long serialVersionUID = 2425161115555074978L;
    private String parameter;
    private Serializable value;
    
    /**
     * Creates a parameter changed event.
     * 
     * @param pipeline the pipeline
     * @param pipelineElement the pipeline element
     * @param parameter the changed parameter
     * @param value the enacted value
     * @param causeMsgId the causing message id
     */
    public ParameterChangedMonitoringEvent(String pipeline, String pipelineElement, String parameter, 
        Serializable value, String causeMsgId) {
        super(pipeline, pipelineElement, null, causeMsgId);
        this.parameter = parameter;
        this.value = value;
    }
    
    /**
     * Returns the changed parameter.
     * 
     * @return the changed parameter
     */
    public String getParameter() {
        return parameter;
    }
    
    /**
     * Returns the enacted value.
     * @return the enacted value
     */
    public Serializable getValue() {
        return value;
    }

}
