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
package eu.qualimaster.coordination;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.infrastructure.PipelineOptions;

/**
 * Stores runtime information about a pipeline.
 * 
 * @author Holger Eichelberger
 */
class PipelineInfo {
    
    private PipelineLifecycleEvent.Status status;
    private Map<PipelineLifecycleEvent.Status, List<IAction>> actions = new HashMap<>();
    private PipelineOptions options;

    /**
     * Returns the current status of the pipeline.
     * 
     * @return the status, may be <b>null</b> if 
     *     {@link #changeStatus(eu.qualimaster.infrastructure.PipelineLifecycleEvent.Status)} was not called before
     */
    PipelineLifecycleEvent.Status getStatus() {
        return status;
    }
    
    /**
     * Returns the pipeline (startup) options.
     * 
     * @return the pipeline options (may be <b>null</b>)
     */
    public PipelineOptions getOptions() {
        return options;
    }

    /**
     * Changes the pipeline (startup) options.
     * 
     * @param options the pipeline options (may be <b>null</b>)
     */
    public void setOptions(PipelineOptions options) {
        this.options = options;
    }
    
    /**
     * Returns the main pipeline if there is one.
     * 
     * @return the name of the main pipeline, <b>null</b> if there is no main pipeline
     */
    public String getMainPipeline() {
        return null != options ? options.getMainPipeline() : null;
    }
    
    /**
     * Changes the status and executes registered actions. Registered actions will be removed from {@link #actions}.
     * 
     * @param status the new status (ignored if <b>null</b>)
     */
    void changeStatus(PipelineLifecycleEvent.Status status) {
        if (null != status) {
            this.status = status;
            List<IAction> a = actions.remove(this.status);
            if (null != a) {
                for (int i = 0; i < a.size(); i++) {
                    a.get(i).execute();
                }
            }
        }
    }
    
    /**
     * Adds an action for a certain status.
     * 
     * @param status the status (may be <b>null</b>)
     * @param action the action
     */
    void addAction(PipelineLifecycleEvent.Status status, IAction action) {
        if (null != status && null != action) {
            List<IAction> a = actions.get(status);
            if (null == a) {
                a = new ArrayList<IAction>();
                actions.put(status, a);
            }
            a.add(action);
        }
    }
    
}
