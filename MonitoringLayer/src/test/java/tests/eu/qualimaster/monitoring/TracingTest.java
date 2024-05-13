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
package tests.eu.qualimaster.monitoring;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import eu.qualimaster.coordination.CoordinationManager;
import eu.qualimaster.coordination.INameMapping;
import eu.qualimaster.coordination.IdentityMapping;
import eu.qualimaster.monitoring.systemState.AlgorithmParameter;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineSystemPart;
import eu.qualimaster.monitoring.systemState.StateUtils;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.monitoring.tracing.FileTrace;
import eu.qualimaster.monitoring.tracing.IParameterProvider;
import eu.qualimaster.monitoring.tracing.ITrace;
import eu.qualimaster.monitoring.tracing.ReflectiveFileTrace;
import eu.qualimaster.observables.TimeBehavior;

/**
 * Tests the tracers.
 * 
 * @author Holger Eichelberger
 */
public class TracingTest {

    /**
     * Tests the file tracer directly with one dynamically removed pipeline (case from loose sub-pipeline integration).
     */
    @Test
    public void testFileTracer() {
        INameMapping mainMapping = new IdentityMapping("mainPip");
        CoordinationManager.registerTestMapping(mainMapping); // avoid full startup
        INameMapping sub1Mapping = new IdentityMapping("subPip1");
        CoordinationManager.registerTestMapping(sub1Mapping); // avoid full startup
        INameMapping sub2Mapping = new IdentityMapping("subPip2");
        CoordinationManager.registerTestMapping(sub2Mapping); // avoid full startup

        IParameterProvider paramProvider = new IParameterProvider() {
            
            @Override
            public Map<String, List<AlgorithmParameter>> getAlgorithmParameters() {
                return null;
            }
        };
        
        ITrace[] tracers = {new FileTrace("test", System.out), new ReflectiveFileTrace("test", System.out)};
        
        SystemState state = new SystemState();
        PipelineSystemPart main = state.obtainPipeline(mainMapping.getPipelineName());
        PipelineNodeSystemPart mainNode = main.obtainPipelineNode("mainNode");
        StateUtils.setValue(mainNode, TimeBehavior.LATENCY, 25, 0);
        
        traceInfrastructure(tracers, state, paramProvider);
        
        PipelineSystemPart sub1 = state.obtainPipeline(sub1Mapping.getPipelineName());
        PipelineNodeSystemPart sub1Node = sub1.obtainPipelineNode("subNode1");
        StateUtils.setValue(sub1Node, TimeBehavior.LATENCY, 30, 0);
        
        traceInfrastructure(tracers, state, paramProvider);
        
        PipelineSystemPart sub2 = state.obtainPipeline(sub2Mapping.getPipelineName());
        PipelineNodeSystemPart sub2Node = sub2.obtainPipelineNode("subNode2");
        StateUtils.setValue(sub2Node, TimeBehavior.LATENCY, 35, 0);
        
        traceInfrastructure(tracers, state, paramProvider);
        
        state.removePipeline(sub1Mapping.getPipelineName());
        
        traceInfrastructure(tracers, state, paramProvider);

        CoordinationManager.unregisterNameMapping(mainMapping);
        CoordinationManager.unregisterNameMapping(sub1Mapping);
        CoordinationManager.unregisterNameMapping(sub2Mapping);
        
        // there is no real assert as checking the output of the tracer is tedious
        // it's more important that there is no exception
    }

    /**
     * Applies the tracers to trace the infrastructure.
     * 
     * @param tracers the tracers
     * @param state the system state
     * @param parameters the parameter provider
     */
    private void traceInfrastructure(ITrace[] tracers, SystemState state, IParameterProvider parameters) {
        for (ITrace trace : tracers) {
            trace.traceInfrastructure(state, parameters);
        }
    }
    
}
