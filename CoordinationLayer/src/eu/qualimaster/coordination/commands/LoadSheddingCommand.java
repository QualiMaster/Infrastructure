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
package eu.qualimaster.coordination.commands;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import eu.qualimaster.common.shedding.ILoadSheddingParameter;
import eu.qualimaster.common.shedding.ILoadShedderDescriptor;

/**
 * Causes / stops load shedding on a certain pipeline element.
 * 
 * @author Holger Eichelberger
 */
public class LoadSheddingCommand extends AbstractPipelineElementCommand {

    private static final long serialVersionUID = -7530049965391304989L;
    private String shedder;
    private Map<String, Serializable> parameter = new HashMap<String, Serializable>();

    /**
     * Creates a load shedding command.
     * 
     * @param pipeline the pipeline to affect
     * @param pipelineElement the pipeline element to affect
     * @param descriptor the load shedder, if <b>null</b> or empty active shedding is disabled
     */
    public LoadSheddingCommand(String pipeline, String pipelineElement, ILoadShedderDescriptor descriptor) {
        this(pipeline, pipelineElement, null == descriptor ? null : descriptor.getIdentifier());
    }

    /**
     * Creates a load shedding command.
     * 
     * @param pipeline the pipeline to affect
     * @param pipelineElement the pipeline element to affect
     * @param shedder the load shedder, if <b>null</b> or empty active shedding is disabled
     */
    public LoadSheddingCommand(String pipeline, String pipelineElement, String shedder) {
        super(pipeline, pipelineElement);
    }
    
    /**
     * Sets an integer parameter.
     * 
     * @param param the parameter identifier
     * @param value the value
     * @throws IllegalArgumentException in case that the parameter does not accept an integer (or a string as fallback)
     */
    public void setIntParameter(ILoadSheddingParameter param, int value) {
        setIntParameter(param.name(), value);
    }

    /**
     * Sets an integer parameter.
     * 
     * @param param the parameter identifier
     * @param value the value
     * @throws IllegalArgumentException in case that the parameter does not accept an integer (or a string as fallback)
     */
    public void setIntParameter(String param, int value) {
        if (null != param) {
            parameter.put(param, value);
        }
    }
    
    /**
     * Sets a generic parameter value (if not specific to the parameter type, String parsing will be used
     * as fallback).
     * 
     * @param param the parameter name
     * @param value the value
     */
    public void setParameter(String param, Serializable value) {
        if (null != param) {
            parameter.put(param, value);
        }
    }

    @Override
    public CoordinationExecutionResult accept(ICoordinationCommandVisitor visitor) {
        return visitor.visitLoadScheddingCommand(this);
    }
    
    /**
     * Returns the load shedder.
     * 
     * @return the load shedder, empty/null indicates no shedding
     */
    public String getShedder() {
        return shedder;
    }
    
    /**
     * Returns a copy of all parameters. Values may be serializables in general, so 
     * reading out parameters shall be done using strings for values as fallback.
     * 
     * @return the parameters
     */
    public Map<String, Serializable> parameters() {
        Map<String, Serializable> result = new HashMap<String, Serializable>();
        result.putAll(parameter);
        return result;
    }

}
