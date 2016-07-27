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

import eu.qualimaster.adaptation.external.ExecutionResponseMessage.ResultType;

/**
 * An information message dispatched to authenticated clients about messages caused by other clients.
 * 
 * @author Holger Eichelberger
 */
public class InformationMessage extends UsualMessage {

    private static final long serialVersionUID = 4413019457217586168L;
    private String pipeline;
    private String pipelineElement;
    private String description;
    private ResultType type;

    /**
     * Creates an information message.
     * 
     * @param pipeline the pipeline (may be <b>null</b> if unknown)
     * @param pipelineElement the pipeline element (may be <b>null</b> if unknown)
     * @param description the description
     */
    public InformationMessage(String pipeline, String pipelineElement, String description) {
        this(pipeline, pipelineElement, description, null);
    }

    /**
     * Creates an information message.
     * 
     * @param pipeline the pipeline (may be <b>null</b> if unknown)
     * @param pipelineElement the pipeline element (may be <b>null</b> if unknown)
     * @param description the description
     * @param type the result type if derived from {@link ExecutionResponseMessage}, (may be <b>null</b> if null)
     */
    public InformationMessage(String pipeline, String pipelineElement, String description, ResultType type) {
        this.pipeline = pipeline;
        this.pipelineElement = pipelineElement;
        this.description = description;
        this.type = type;
    }
    
    @Override
    public void dispatch(IDispatcher dispatcher) {
        if (dispatcher instanceof IInformationDispatcher) {
            ((IInformationDispatcher) dispatcher).handleInformationMessage(this);
        }
    }

    /**
     * Returns the pipeline name.
     * 
     * @return the pipeline name (may be <b>null</b> if unknown)
     */
    public String getPipeline() {
        return pipeline;
    }

    /**
     * Returns the pipeline element name.
     * 
     * @return the pipeline element name (may be <b>null</b> if unknown)
     */
    public String getPipelineElement() {
        return pipelineElement;
    }

    /**
     * Returns the description.
     * 
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the response type.
     * 
     * @return the response type (may be <b>null</b> if unknown)
     */
    public ResultType getType() {
        return type;
    }

    @Override
    public Message toInformation() {
        return null; // not recursive
    }
    
    @Override
    public String toString() {
        return "InformationMessage " + pipeline + " " + pipelineElement + " " + description + " " + type;
    }

}
