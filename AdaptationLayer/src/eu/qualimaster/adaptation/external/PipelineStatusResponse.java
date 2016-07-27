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
package eu.qualimaster.adaptation.external;

/**
 * Server sided pipeline status reponse for a {@link PipelineStatusRequest}.
 * 
 * @author Holger Eichelberger
 */
public class PipelineStatusResponse extends ResponseMessage {

    private static final long serialVersionUID = 47822457200938145L;
    private String[] activePipelines;
    
    /**
     * Creates a pipeline status response.
     * 
     * @param request the actual request
     * @param activePipelines the names of the active pipelines (may be <b>null</b>)
     */
    public PipelineStatusResponse(PipelineStatusRequest request, String[] activePipelines) {
        super(request);
        this.activePipelines = activePipelines;
    }
    
    /**
     * Returns the number of active pipelines.
     * 
     * @return the number of active pipelines
     */
    public int getActivePipelinesCount() {
        return null == activePipelines ? 0 : activePipelines.length;
    }
    
    /**
     * Returns the name of the active pipeline at <code>index</code>.
     * 
     * @param index the index of the pipeline to return the name for
     * @return the name of the pipeline at <code>index</code>
     * @throws IllegalArgumentException if there are no pipelines or 
     *     <code>index &lt; 0 || index &gt;={@link #getActivePipelinesCount()}</code>
     */
    public String getActivePipelineName(int index) {
        if (null == activePipelines || index < activePipelines.length) {
            throw new IllegalArgumentException("no pipelines");
        }
        return activePipelines[index];
    }
    
    @Override
    public void dispatch(IDispatcher dispatcher) {
        dispatcher.handlePipelineStatusResponse(this);
    }

    @Override
    public Message toInformation() {
        return null; // do not dispatch
    }

    @Override
    public int hashCode() {
        return Utils.hashCode(activePipelines) + super.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        boolean equals = super.equals(obj);
        if (obj instanceof PipelineStatusResponse) {
            PipelineStatusResponse msg = (PipelineStatusResponse) obj;
            equals = getActivePipelinesCount() == msg.getActivePipelinesCount();
            for (int p = 0; equals && p < getActivePipelinesCount(); p++) {
                equals = Utils.equals(getActivePipelineName(p), msg.getActivePipelineName(p));
            }
        }
        return equals;
    }

}
