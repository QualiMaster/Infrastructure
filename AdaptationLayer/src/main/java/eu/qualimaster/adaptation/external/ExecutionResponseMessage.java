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

/**
 * Implements a response message on execution a (command) message.
 * 
 * @author Holger Eichelberger
 */
public class ExecutionResponseMessage extends ResponseMessage {

    private static final long serialVersionUID = -4926942582857767232L;
    private ResultType result;
    private String description;
    
    /**
     * Defines the execution status.
     * 
     * @author Holger Eichelberger
     */
    public enum ResultType {
        SUCCESSFUL,
        FAILED
    }
    
    /**
     * Creates a response message.
     * 
     * @param request the request causing this response
     * @param result the execution resule
     * @param description an optional description of the execution (may be <b>null</b> or empty)
     */
    public ExecutionResponseMessage(RequestMessage request, ResultType result, String description) {
        super(request);
        this.result = result;
        this.description = description;
    }

    /**
     * Returns the state of the execution.
     * 
     * @return the state
     */
    public ResultType getResult() {
        return result;
    }
    
    /**
     * Returns the description of the execution result.
     * 
     * @return the description (may be <b>null</b> or empty)
     */
    public String getDescription() {
        return description;
    }

    @Override
    public void dispatch(IDispatcher dispatcher) {
        dispatcher.handleExecutionResponseMessage(this);
    }
    
    @Override
    public int hashCode() {
        return super.hashCode() + Utils.hashCode(getResult()) + Utils.hashCode(getDescription());
    }
    
    @Override
    public boolean equals(Object obj) {
        boolean equals = super.equals(obj);
        if (obj instanceof ExecutionResponseMessage) {
            ExecutionResponseMessage msg = (ExecutionResponseMessage) obj;
            equals &= Utils.equals(getResult(), msg.getResult());
            equals &= Utils.equals(getDescription(), msg.getDescription());
        }
        return equals;
    }

    @Override
    public Message toInformation() {
        return new InformationMessage(null, null, description, result);
    }

    @Override
    public String toString() {
        return "ExecutionResponseMessage " + result + " " + description;
    }
    
}
