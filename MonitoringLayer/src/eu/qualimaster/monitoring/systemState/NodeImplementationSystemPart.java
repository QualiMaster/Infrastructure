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
package eu.qualimaster.monitoring.systemState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.qualimaster.coordination.INameMapping;
import eu.qualimaster.coordination.INameMapping.Component;
import eu.qualimaster.coordination.INameMapping.Component.Type;
import eu.qualimaster.monitoring.MonitoringManager;
import eu.qualimaster.monitoring.parts.IPartType;
import eu.qualimaster.monitoring.topology.ITopologyProvider;
import eu.qualimaster.monitoring.topology.PipelineTopology;
import eu.qualimaster.monitoring.topology.PipelineTopology.Processor;
import eu.qualimaster.monitoring.topology.ITopologyProjection;
import eu.qualimaster.monitoring.topology.TopologyProjection;
import eu.qualimaster.monitoring.tracing.ITrace;
import eu.qualimaster.observables.IObservable;

/**
 * Represents an implementation of a pipeline node as a system part. An implementation may be an algorithm, a source or 
 * a sink.
 * 
 * @author Holger Eichelberger
 */
public class NodeImplementationSystemPart extends SystemPart implements ITopologyProvider {

    private static final long serialVersionUID = -5259771855502524419L;
    private transient ITrace trace;
    private PipelineSystemPart pipeline;
    private Map<String, PipelineNodeSystemPart> elements = new HashMap<String, PipelineNodeSystemPart>();

    /**
     * Creates an implementation system part.
     * 
     * @param type the name of the part
     * @param name the name of the part
     * @param pipeline the containing pipeline
     * @param trace the log trace (may be <b>null</b> for no tracing)
     */
    NodeImplementationSystemPart(IPartType type, String name, PipelineSystemPart pipeline, ITrace trace) {
        super(type, name);
        this.trace = trace;
        this.pipeline = pipeline;
    }

    /**
     * Creates a copy of this system part. Call {@link #adjustReferences(NodeImplementationSystemPart)} after copying 
     * all elements.
     * 
     * @param source the source system part
     * @param pipeline the containing pipeline
     * @param state the parent system state for crosslinks
     */
    protected NodeImplementationSystemPart(NodeImplementationSystemPart source, PipelineSystemPart pipeline, 
        SystemState state) {
        super(source, state);
        this.pipeline = pipeline; // check!!
        this.trace = source.trace; // take over for copying the state and writing traces on it
        synchronized (elements) {
            for (Map.Entry<String, PipelineNodeSystemPart> entry : source.elements.entrySet()) {
                this.elements.put(entry.getKey(), new PipelineNodeSystemPart(entry.getValue(), state, this));
            }
        }
    }
    
    /**
     * Adjusts potential references.
     * 
     * @param source the source system part
     */
    protected void adjustReferences(NodeImplementationSystemPart source) {
        synchronized (elements) {
            for (Map.Entry<String, PipelineNodeSystemPart> entry : source.elements.entrySet()) {
                getNode(entry.getKey()).adjustCurrent(entry.getValue());
            }
        }
    }

    /**
     * Returns the actual trace.
     * 
     * @return the trace (may be <b>null</b> for no tracing)
     */
    public ITrace getTrace() {
        return trace;
    }
    
    /**
     * Closes the (trace of) this part.
     */
    public void close() {
        if (null != trace) {
            trace.close();
        }
    }
    
    /**
     * Returns the parent pipeline.
     * 
     * @return the parent pipeline
     */
    public PipelineSystemPart getPipeline() {
        return pipeline;
    }

    @Override
    public PipelineTopology getTopology() {
        return pipeline.getTopology();
    }
    
    /**
     * Returns the stored sub-topology nodes.
     * 
     * @return the pipeline nodes
     */
    public Collection<PipelineNodeSystemPart> getNodes() {
        synchronized (elements) {
            return elements.values();
        }
    }
    
    /**
     * Returns the number of sub-topology nodes.
     * 
     * @return the number of sub-topology nodes
     */
    public int getNodeCount() {
        synchronized (elements) {
            return elements.size();
        }
    }
    
    /**
     * Returns whether this implementation has a given pipeline node as sub-implementation.
     * 
     * @param nodeName the node name
     * @return <code>true</code> if the node is known, <code>false</code> else
     */
    public boolean hasNode(String nodeName) {
        synchronized (elements) {
            return elements.containsKey(nodeName);
        }    
    }

    @Override
    public PipelineNodeSystemPart getNode(String nodeName) {
        synchronized (elements) {
            return elements.get(nodeName);
        }        
    }

    /**
     * Returns a sub-pipeline node via its node name or creates it if it does not exist.
     * 
     * @param nodeName the name of the pipeline element
     * @return the related system part 
     */
    public PipelineNodeSystemPart obtainPipelineNode(String nodeName) {
        synchronized (elements) {
            PipelineNodeSystemPart result = elements.get(nodeName);
            if (null == result) {
                INameMapping mapping = MonitoringManager.getNameMapping(pipeline.getName());
                Component component = mapping.getComponentByImplName(nodeName);
                Type type;
                boolean useThrift;
                if (null == component) { // assume legacy sub-topology
                    type = Type.UNKNOWN;
                    useThrift = true;
                } else {
                    type = component.getType();
                    useThrift = component.useThrift();
                }
                synchronized (elements) {
                    result = new PipelineNodeSystemPart(nodeName, type, useThrift, this); 
                    elements.put(nodeName, result);
                }
            }
            return result;
        }
    }

    /**
     * Creates a textual representation but in contrast to {@link #toString()} in this case performs pretty printing.
     * 
     * @param indent the indentation
     * @return the textual representation
     */
    public String format(String indent) {
        synchronized (elements) {
            return super.toString() + "\n   elements: " + format(elements, indent);
        }
    }
    
    @Override
    public String toString() {
        synchronized (elements) {
            return super.toString() + " elements: " + elements;
        }
    }

    @Override
    public ITopologyProjection getTopologyProjection() {
        return new TopologyProjection(projectTopologyNodes(true), projectTopologyNodes(false), null);
    }

    /**
     * Projects the topology (start or end) nodes constituting the sub-topology implementing this implementation part.
     * 
     * @param start return the start nodes or the end nodes
     * @return the nodes if the respective nodes from the overall topology shall be taken
     */
    List<Processor> projectTopologyNodes(boolean start) {
        List<Processor> result = new ArrayList<Processor>();
        PipelineTopology topo = getTopology(); // use actual
        if (null != topo) {
            for (String nodeName : elements.keySet()) {
                Processor proc = topo.getProcessor(nodeName);
                if (null != proc) {
                    int nodes = 0;
                    if (start) {
                        for (int i = 0; i < proc.getInputCount(); i++) {
                            if (hasNode(proc.getInput(i).getOrigin().getName())) {
                                nodes++;
                            }
                        }
                    } else {
                        for (int o = 0; o < proc.getOutputCount(); o++) {
                            if (hasNode(proc.getOutput(o).getTarget().getName())) {
                                nodes++;
                            }
                        }
                    }
                    if (0 == nodes) {
                        result.add(proc);
                    }
                }
            }
        }
        return result;
    }
    
    @Override
    public void replaceComponentKeys(Object oldKey, Object newKey, IObservable... observables) {
        super.replaceComponentKeys(oldKey, newKey, observables);
        for (PipelineNodeSystemPart node : elements.values()) {
            node.replaceComponentKeys(oldKey, newKey, observables);
        }
    }
    
}
