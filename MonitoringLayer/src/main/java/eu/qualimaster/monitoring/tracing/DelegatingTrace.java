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
package eu.qualimaster.monitoring.tracing;

import java.io.Serializable;
import java.util.Map;

import eu.qualimaster.coordination.events.AlgorithmProfilingEvent.DetailMode;
import eu.qualimaster.monitoring.systemState.NodeImplementationSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;

/**
 * Delegates tracing to multiple individual traces.
 * 
 * @author Holger Eichelberger
 */
public class DelegatingTrace implements ITrace {

    private ITrace[] traces;
    
    /**
     * Creates a delegating trace.
     * 
     * @param traces the traces to delegate to
     */
    public DelegatingTrace(ITrace... traces) {
        this.traces = traces;
    }
    
    @Override
    public boolean isInitialized() {
        boolean initialized = true;
        for (ITrace t: traces) {
            initialized &= t.isInitialized();
        }
        return initialized;
    }

    @Override
    public void close() {
        for (ITrace t: traces) {
            t.close();
        }
    }

    @Override
    public void traceAlgorithm(PipelineNodeSystemPart node, NodeImplementationSystemPart alg,
        IParameterProvider parameters) {
        for (ITrace t: traces) {
            t.traceAlgorithm(node, alg, parameters);
        }
    }

    @Override
    public void traceInfrastructure(SystemState state, IParameterProvider parameters) {
        for (ITrace t: traces) {
            t.traceInfrastructure(state, parameters);
        }
    }

    @Override
    public void notifyNewSubTrace(Map<String, Serializable> settings) {
        for (ITrace t: traces) {
            t.notifyNewSubTrace(settings);
        }
    }

    @Override
    public void setTraceMode(DetailMode mode) {
        for (ITrace t: traces) {
            t.setTraceMode(mode);
        }
    }

}
