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
package eu.qualimaster.monitoring.tracing;

import java.io.Serializable;
import java.util.Map;

import eu.qualimaster.coordination.events.AlgorithmProfilingEvent.DetailMode;
import eu.qualimaster.monitoring.systemState.NodeImplementationSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;

/**
 * The interface for a multi-dimensional timeseries trace.
 * 
 * @author Holger Eichelberger
 */
public interface ITrace {

    /**
     * Returns whether the trace is already initialized.
     * 
     * @return <code>true</code> if the trace is initialized, <code>false</code> else
     */
    public boolean isInitialized();
    
    /**
     * Closes the trace.
     */
    public void close();
    
    /**
     * Traces the algorithm <code>alg</code> used by <code>node</code> with the given <code>parameters</code>.
     * 
     * @param node the pipeline node using the algorithm
     * @param alg the algorithm to trace
     * @param parameters the parameter (provider)
     */
    public void traceAlgorithm(PipelineNodeSystemPart node, NodeImplementationSystemPart alg, 
        IParameterProvider parameters);

    /**
     * Traces the whole infrastructure with the given <code>parameters</code>.
     * 
     * @param state the system state to trace
     * @param parameters the parameter (provider)
     */
    public void traceInfrastructure(SystemState state, IParameterProvider parameters);

    /**
     * Notifies this trace about a new sub trace.
     * 
     * @param settings the trace settings (may be <b>null</b>)
     */
    public void notifyNewSubTrace(Map<String, Serializable> settings);
    
    /**
     * Defines whether algorithm traces shall be done in detailed mode (contained algorithms). By default, detail
     * mode shall be disabled.
     * 
     * @param mode the desired detail mode
     */
    public void setTraceMode(DetailMode mode);
    
}
