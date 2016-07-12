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
package eu.qualimaster.coordination.events;

import java.io.Serializable;
import java.util.Map;

import eu.qualimaster.common.QMInternal;

/**
 * Informs upper layers about the status of algorithm profiling.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public class AlgorithmProfilingEvent extends CoordinationEvent {


    /**
     * Defines the status of the profiling.
     * 
     * @author Holger Eichelberger
     */
    public enum Status {
        
        /**
         * Start of first profiling run. May occur before the actual pipeline started.
         */
        START,
        
        /**
         * Start of a subsequent profiling run.
         */
        NEXT,
        
        /**
         * The final profiling run has completed.
         */
        END
    }

    /**
     * Defines the detail mode for profiling.
     * 
     * @author Holger Eichelberger
     */
    public enum DetailMode {
        
        /**
         * No details.
         */
        FALSE(false, false),
        
        /**
         * Algorithm level details (legacy). Corresponds to {@link #ALGORITHMS}.
         */
        TRUE(true, false),
        
        /**
         * Algorithm level details.
         */
        ALGORITHMS(true, false),
        
        /**
         * Task level details. Implies {@link #ALGORITHMS}.
         */
        TASKS(true, true);
        
        private boolean traceAlgorithms;
        private boolean traceTasks;
        
        /**
         * Creates a mode object.
         * 
         * @param traceAlgorithms trace algorithms
         * @param traceTasks trace tasks
         */
        private DetailMode(boolean traceAlgorithms, boolean traceTasks) {
            this.traceAlgorithms = traceAlgorithms;
            this.traceTasks = traceTasks;
        }
        
        /**
         * Returns whether algorithms shall be traced.
         * 
         * @return <code>true</code> for tracing, <code>false</code> else
         */
        public boolean traceAlgorithms() {
            return traceAlgorithms;
        }
        
        /**
         * Returns whether tasks shall be traced.
         * 
         * @return <code>true</code> for tracing, <code>false</code> else
         */
        public boolean traceTasks() {
            return traceTasks;
        }

    }
    
    private static final long serialVersionUID = -5596469942608086092L;
    private String pipeline;
    private String family;
    private String algorithm;
    private Status status;
    private Map<String, Serializable> settings;
    private DetailMode detailMode = DetailMode.FALSE;
    
    /**
     * Creates an event.
     * 
     * @param pipeline the pipeline name
     * @param family the family name
     * @param algorithm the algorithm name
     * @param status the status
     * @param settings the actual settings for profiling (just as information, may be <b>null</b>, 
     *     usually not for {@link Status#END})
     */
    public AlgorithmProfilingEvent(String pipeline, String family, String algorithm, Status status, 
        Map<String, Serializable> settings) {
        this.pipeline = pipeline;
        this.family = family;
        this.algorithm = algorithm;
        this.status = status;
        this.settings = settings;
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
     * Returns the family name.
     * 
     * @return the family name
     */
    public String getFamily() {
        return family;
    }

    /**
     * Returns the algorithm name.
     * 
     * @return the algorithm name
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * Returns the status.
     * 
     * @return the status
     */
    public Status getStatus() {
        return status;
    }
    
    /**
     * Returns the actual settings for this profiling run.
     * 
     * @return the actual settings (for information only, may be <b>null</b> if unspecified, 
     *     usually not for {@link Status#END})
     */
    public Map<String, Serializable> getSettings() {
        return settings;
    }
    
    /**
     * Returns whether tracing shall happen in detail mode.
     * 
     * @return <code>true</code> for detail mode, <code>false</code> else
     */
    public DetailMode getDetailMode() {
        return detailMode;
    }
    
    /**
     * Changes the tracing detail mode. Can be changed only once from {@link DetailMode#FALSE}.
     * 
     * @param mode the mode to use (<b>null</b> is ignored)
     */
    public void setDetailMode(DetailMode mode) {
        if (null != mode && DetailMode.FALSE == detailMode) {
            detailMode = mode;
        }
    }
    
}
