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
package eu.qualimaster.adaptation.external;

import java.io.Serializable;

/**
 * Requests a parameter change of a certain pipeline element. This message can be sent without authentication, but leads
 * then to an adaptation of lower priority, potentially a rejection. This message can be elevated, but is then processed
 * only if authenticated. Leads to an {@link ExecutionResponseMessage}.
 * 
 * @param <V> the value type
 * @author Holger Eichelberger
 */
public class ChangeParameterRequest <V extends Serializable> extends RequestMessage {
    
    private static final long serialVersionUID = -3619453843277478293L;
    private String pipeline;
    private String pipelineElement;
    private String parameter;
    private V value;

    /**
     * Create a parameter change request message.
     * 
     * @param pipeline the pipeline name
     * @param pipelineElement the pipeline element name to change the request for
     * @param parameter the parameter name
     * @param value the new value
     */
    public ChangeParameterRequest(String pipeline, String pipelineElement, String parameter, V value) {
        this.pipeline = pipeline;
        this.pipelineElement = pipelineElement;
        this.parameter = parameter;
        this.value = value;
    }
    
    
    @Override
    public void dispatch(IDispatcher dispatcher) {
        dispatcher.handleChangeParameterRequest(this);
    }

    /**
     * Returns the pipeline name.
     * 
     * @return the pipeline name
     */
    public String getPipeline() {
        return pipeline;
    }

    /**
     * Returns the name of the pipeline element to apply the parameter change for.
     * 
     * @return the name of the pipeline element
     */
    public String getPipelineElement() {
        return pipelineElement;
    }

    /**
     * Returns the name of the parameter to change.
     * 
     * @return the parameter name
     */
    public String getParameter() {
        return parameter;
    }

    /**
     * Returns the new value for the parameter.
     * 
     * @return the value
     */
    public V getValue() {
        return value;
    }
    
    @Override
    public int hashCode() {
        return super.hashCode() + Utils.hashCode(getPipeline()) + Utils.hashCode(getPipelineElement()) 
            + Utils.hashCode(getParameter()) + Utils.hashCode(getValue()) + Utils.hashCode(requiresAuthentication());
    }
    
    @Override
    public boolean equals(Object obj) {
        boolean equals = super.equals(obj);
        if (obj instanceof ChangeParameterRequest) {
            ChangeParameterRequest<?> msg = (ChangeParameterRequest<?>) obj;
            equals &= Utils.equals(getPipeline(), msg.getPipeline());
            equals &= Utils.equals(getPipelineElement(), msg.getPipelineElement());
            equals &= Utils.equals(getParameter(), msg.getParameter());
            equals &= Utils.equals(getValue(), msg.getValue());
            equals &= requiresAuthentication() == msg.requiresAuthentication(); // avoid elevated equality
        }
        return equals;
    }

    /**
     * Implements an elevated change parameter request.
     * 
     * @param <V> the value type
     * @author Holger Eichelberger
     */
    private static class ElevatedChangeParameterRequest <V extends Serializable> extends ChangeParameterRequest<V> {

        private static final long serialVersionUID = 5826274162904202726L;

        /**
         * Creates an elevated change parameter request.
         * 
         * @param request the original request
         */
        private ElevatedChangeParameterRequest(ChangeParameterRequest<V> request) {
            super(request.getPipeline(), request.getPipelineElement(), request.getParameter(), request.getValue());
            setMessageId(request.getMessageId());
            setClientId(request.getClientId());
        }
        
        @Override
        public final boolean requiresAuthentication() {
            return true; // privileged messages require always an authenticated connection, no reduction possible
        }

        @Override
        public final boolean passToUnauthenticatedClient() {
            return false; // pass never
        }
        
        @Override
        public final Message elevate() {
            return this; // we are already elevated
        }

    }
    
    @Override
    public Message elevate() {
        return new ElevatedChangeParameterRequest<V>(this);
    }

    @Override
    public Message toInformation() {
        return new InformationMessage(pipeline, pipelineElement, "set parameter '" + parameter + "' to value '" 
            + value + "'", null);
    }

    @Override
    public String toString() {
        return "ParameterChangeRequest " + pipeline + " " + pipelineElement + " " + parameter + " " + value;
    }
    
}
