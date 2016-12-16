package eu.qualimaster.monitoring.tracing;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import eu.qualimaster.monitoring.parts.PartType;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineSystemPart;
import eu.qualimaster.monitoring.systemState.PlatformSystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;

/**
 * Represents a trace storing the information required for reflective adaptation.
 * 
 * @author Andrea Ceroni
 */
public class ReflectiveFileTrace extends AbstractFileTrace {
    
    /**
     * Creates a new trace.
     * 
     * @param name the name of the trace (for {link {@link #toString()}}, shall be the file name
     * @param out the output stream to trace to
     */
    public ReflectiveFileTrace(String name, PrintStream out) {
        super(name, out);
    }
    
    /**
     * Traces the platform.
     * 
     * @param state the system state
     */
    private void tracePlatform(SystemState state) {
        PlatformSystemPart platform = state.getPlatform();
        print("platform:");
        printSeparator();
        print(platform.getName());
        printSeparator();
        trace(platform, PlatformSystemPart.class, null, null, null);
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
        if (null != pipeline) { // unsure how this shall look for the reflective trace, pls check TracingTest
            print("pipeline:");
            printSeparator();
            print(pipeline.getName());
            printSeparator();
            trace(pipeline, PipelineSystemPart.class, null, null, null);
            
            Set<String> done = new HashSet<String>();
            for (String nodeName : info.nodes) {
                PipelineNodeSystemPart node = pipeline.getNode(nodeName);
                tracePipelineNode(node);
                done.add(nodeName);
            }
            for (PipelineNodeSystemPart node : pipeline.getNodes()) {
                String name = node.getName();
                if (!done.contains(name)) {
                    tracePipelineNode(node);
                    info.nodes.add(name);
                }
            }
        }
        //print("pipeline/");
    }
    
    /**
     * Traces a single pipeline node.
     * 
     * @param node the node to be traced
     */
    private void tracePipelineNode(PipelineNodeSystemPart node) {
        if (null != node) {
            print("node:");
            printSeparator();
            print(node.getName());
            printSeparator();
            trace(node, PipelineNodeSystemPart.class, null, null, null);
        }
    }

    @Override
    public void traceInfrastructure(SystemState state, IParameterProvider parameters) {
        SystemState copy = new SystemState(state);
        
        if (!isInitialized() && null == pipelines) {
            pipelines = new ArrayList<PipelineTraceInfo>(); 
            
            printFormat(PlatformSystemPart.class, PartType.PLATFORM, "platform format:\t");
            printFormat(PipelineSystemPart.class, PartType.PIPELINE, "pipeline format:\t");
            printFormat(PipelineNodeSystemPart.class, PartType.PIPELINE_NODE, "pipeline node format:\t");
            println();
            initialized = true;
        }
        
        print(copy.getTimestamp());
        printSeparator();
        
        tracePlatform(copy);
        
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