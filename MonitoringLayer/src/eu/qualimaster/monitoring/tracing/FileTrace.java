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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import eu.qualimaster.monitoring.parts.PartType;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineSystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;

/**
 * Represents a trace. The pipeline/infrastructure trace format is rather preliminary.
 * 
 * @author Holger Eichelberger
 */
public class FileTrace extends AbstractFileTrace {
    
    /**
     * Creates a new trace.
     * 
     * @param name the name of the trace (for {link {@link #toString()}}, shall be the file name
     * @param out the output stream to trace to
     */
    public FileTrace(String name, PrintStream out) {
        super(name, out);
    }
    
    /**
     * Traces a pipeline.
     * 
     * @param state the system state
     * @param info the trace information
     * @param parameters the parameters
     */
    private void tracePipeline(SystemState state, PipelineTraceInfo info, IParameterProvider parameters) {
        PipelineSystemPart pipeline = state.getPipeline(info.name);
        print("pipeline:");
        printSeparator();
        if (null != pipeline) { // may already be gone
            print(pipeline.getName());
        }
        printSeparator();
        trace(pipeline, PipelineSystemPart.class, null, null, null);
        
        Set<String> done = new HashSet<String>();
        for (String nodeName : info.nodes) {
            PipelineNodeSystemPart node = pipeline.getNode(nodeName);
            tracePipelineNode(node);
            done.add(nodeName);
        }
        if (null != pipeline) {
            for (PipelineNodeSystemPart node : pipeline.getNodes()) {
                String name = node.getName();
                if (!done.contains(name)) {
                    tracePipelineNode(node);
                    info.nodes.add(name);
                }
            }
        }
        print("pipeline/");
    }
    
    /**
     * Traces a single pipeline node.
     * 
     * @param node the node to be traced
     */
    private void tracePipelineNode(PipelineNodeSystemPart node) {
        if (null != node) {
            print(node.getName());
        }
        printSeparator();
        trace(node, PipelineNodeSystemPart.class, null, null, null);
    }

    @Override
    public void traceInfrastructure(SystemState state, IParameterProvider parameters) {
        SystemState copy = new SystemState(state);
        
        if (!isInitialized() && null == pipelines) {
            pipelines = new ArrayList<PipelineTraceInfo>(); 
            
            printFormat(PipelineSystemPart.class, PartType.PIPELINE, "pipeline format: ");
            printFormat(PipelineNodeSystemPart.class, PartType.PIPELINE_NODE, "pipeline node format: ");
            println();
            initialized = true;
        }
        
        print(copy.getTimestamp());
        printSeparator();
        Set<String> pipelineDone = new HashSet<String>();
        for (PipelineTraceInfo info : pipelines) {
            tracePipeline(copy, info, parameters);
            pipelineDone.add(info.name);
        }
        Collection<PipelineSystemPart> pipelines = copy.getPipelines();
        for (PipelineSystemPart pipeline : pipelines) {
            String pName = pipeline.getName();
            if (!pipelineDone.contains(pName)) {
                tracePipeline(copy, getTraceInfo(pName), parameters);
            }
        }
        println();
    }
}