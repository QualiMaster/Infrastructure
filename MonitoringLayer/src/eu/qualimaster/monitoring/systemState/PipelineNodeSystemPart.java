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
import java.util.Set;

import eu.qualimaster.coordination.INameMapping.Component.Type;
import eu.qualimaster.monitoring.events.ComponentKey;
import eu.qualimaster.monitoring.events.FrozenSystemState;
import eu.qualimaster.monitoring.parts.PartType;
import eu.qualimaster.monitoring.topology.ITopologyProvider;
import eu.qualimaster.monitoring.topology.PipelineTopology;
import eu.qualimaster.monitoring.topology.PipelineTopology.Stream;
import eu.qualimaster.monitoring.topology.SingleNodeTopologyProjection;
import eu.qualimaster.monitoring.topology.PipelineTopology.Processor;
import eu.qualimaster.monitoring.topology.ITopologyProjection;
import eu.qualimaster.monitoring.topology.TopologyProjection;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.ResourceUsage;

/**
 * Represents a pipeline node system part.
 * 
 * @author Holger Eichelberger
 */
public class PipelineNodeSystemPart extends SystemPart implements ITopologyProvider {

    private static final long serialVersionUID = -2401755797978421980L;
    private static final Set<IObservable> VALIDATE = SystemState.createSet(IObservable.class, 
        ResourceUsage.EXECUTORS, ResourceUsage.TASKS);
    private NodeImplementationSystemPart current;
    private boolean useThrift;
    private PipelineSystemPart pipeline;
    private NodeImplementationSystemPart parent; // parent
    private int currentCount = 0;
    
    /**
     * Creates a pipeline node system part.
     * 
     * @param name the name of the pipeline node
     * @param type the node type
     * @param useThrift shall thrift be used
     * @param pipeline the parent pipeline
     */
    PipelineNodeSystemPart(String name, Type type, boolean useThrift, PipelineSystemPart pipeline) {
        super(PartType.PIPELINE_NODE, type, name);
        this.pipeline = pipeline;
        this.useThrift = useThrift;
        pipeline.registerNode(this);
    }
    
    /**
     * Creates a pipeline node system part.
     * 
     * @param name the name of the pipeline node
     * @param type the node type
     * @param useThrift shall thrift be used
     * @param parent the parent algorithm
     */
    PipelineNodeSystemPart(String name, Type type, boolean useThrift, NodeImplementationSystemPart parent) {
        // currently, this mixes source, sink and element -> mapping?
        super(PartType.PIPELINE_NODE, type, name);
        this.parent = parent;
        this.useThrift = useThrift;
        if (null != parent) {
            parent.getPipeline().registerNode(this);
        }
    }
    
    /**
     * Creates a copy of this system part. Please call {@link #adjustCurrent(PipelineNodeSystemPart)} after all
     * nodes have been copied.
     * 
     * @param source the source system part
     * @param state to obtain crosslinks (algorithms must be present)
     * @param pipeline the parent pipeline
     */
    protected PipelineNodeSystemPart(PipelineNodeSystemPart source, SystemState state, PipelineSystemPart pipeline) {
        super(source, state);
        this.pipeline = pipeline;
        this.useThrift = source.useThrift;
        pipeline.registerNode(this);
    }
    
    /**
     * Creates a copy of this system part. Please call {@link #adjustCurrent(PipelineNodeSystemPart)} after all
     * nodes have been copied.
     * 
     * @param source the source system part
     * @param state to obtain crosslinks (algorithms must be present)
     * @param algorithm the parent algorithm
     */
    protected PipelineNodeSystemPart(PipelineNodeSystemPart source, SystemState state, 
        NodeImplementationSystemPart algorithm) {
        super(source, state);
        this.parent = algorithm;
        this.useThrift = source.useThrift;
        algorithm.getPipeline().registerNode(this);
    }
    
    /**
     * Adjusts the current node after copying.
     * 
     * @param source the source node to adjust from
     */
    protected void adjustCurrent(PipelineNodeSystemPart source) {
        if (null != source.current && null != pipeline) {
            setCurrent(pipeline.getPart(getType(), source.current.getName()), false);
        }
    }
    
    @Override
    protected void fill(String prefix, String name, FrozenSystemState state) {
        super.fill(prefix, name, state);
        if (null != current) {
            // let's leave the active flag for all
            state.setActiveAlgorithm(getPipeline().getName(), current.getName());
        }
    }
    
    /**
     * Returns the parent pipeline.
     * 
     * @return the parent pipeline
     */
    public PipelineSystemPart getPipeline() {
        return null == pipeline ? parent.getPipeline() : pipeline;
    }

    /**
     * Returns whether this node system part is ready for execution. This method relies on assumptions from the 
     * pipeline generation, i.e., families are created uninitialized, receive their initial algorithm and notify the
     * infrastructure about that while sources and sinks just have their algorithms and may need to be connected.
     * 
     * @return <code>true</code> if it is ready for execution, <code>false</code> else
     */
    public boolean isInitialized() {
        boolean ready;
        Type type = getComponentType();
        if (null != type) {
            if (Type.FAMILY == type) {
                ready = current != null;
            } else {
                ready = true;
            }
        } else {
            ready = false;
        }
        return ready;
    }

    /**
     * Changes the current algorithm.
     * 
     * @param current the current algorithm
     */
    public void setCurrent(NodeImplementationSystemPart current) {
        setCurrent(current, true);
    }
    
    /**
     * Changes the current algorithm.
     * 
     * @param current the current algorithm
     * @param switchedTo whether this is a call to indicate a switch or whether it its used for
     *     internal purposes, e.g., copying
     */
    private void setCurrent(NodeImplementationSystemPart current, boolean switchedTo) {
        if (current != this.current) {
            //unlink(this.current, ILinkSelector.ALL); // no links, happens by sub-topology aggregation
            this.current = current;
            //link(this.current, ILinkSelector.ALL); // no links, happens by ub-topology aggregation
            currentCount = 1;
            if (switchedTo) {
                switchedTo();
            }
        } else {
            currentCount++;
        }
    }
    
    /**
     * Unlinks the observables of <code>part</code> from into observable. Uses a set-based link selector 
     * over {@link #VALIDATE}.
     * 
     * @param part the part to take the observables to unlink from (may be <b>null</b>)
     */
    protected void link(NodeImplementationSystemPart part) {
        ILinkSelector selector = new SetLinkSelector(VALIDATE);
        super.link(part, selector);
        if (null != part) {
            for (PipelineNodeSystemPart comp : part.getNodes()) {
                link(comp, selector);
            }
        }
    }
    
    /**
     * Unlinks the observables of <code>part</code> from this observable. Uses a set-based link selector 
     * over {@link #VALIDATE}.
     * 
     * @param part the part to take the observables to unlink from (may be <b>null</b>)
     */
    protected void unlink(NodeImplementationSystemPart part) {
        ILinkSelector selector = new SetLinkSelector(VALIDATE);
        super.unlink(part, selector);
        if (null != part) {
            for (PipelineNodeSystemPart comp : part.getNodes()) {
                unlink(comp, selector);
            }            
        }
    }
    
    /**
     * Returns the current algorithm.
     * 
     * @return the current algorithm (<b>null</b> if unused)
     */
    public NodeImplementationSystemPart getCurrent() {
        return current;
    }

    /**
     * Returns how often the current value was set with the same instance indicating the number of tasks acknowledging
     * the change.
     * 
     * @return the number of current changes to the same value
     */
    public int getCurrentCount() {
        return currentCount;
    }

    // no direct access to observable needed here

    @Override
    public void setValue(IObservable observable, double value, Object key) {
        boolean doit = true;
        if (VALIDATE.contains(observable) && key instanceof ComponentKey) {
            Double val = getPipeline().validateComponent((ComponentKey) key, observable, value, false, this);
            if (null != val) {
                value = val;
            } else {
                doit = false;
            }
        } 
        if (doit) {
            doSetValue(observable, value, key);
        }            
    }
    
    /**
     * Sets the value of the given <code>observable</code> by replacing the existing one.
     * 
     * @param observable the observable to change
     * @param value the new value (may be <b>null</b> but then this method will perform an update of the data)
     * @param key the key representing the compound in a composite observation,
     *   may be <b>null</b>
     */
    private void doSetValue(IObservable observable, double value, Object key) { 
        super.setValue(observable, value, key);
        if (null != current) {
            current.setValue(observable, value, key);
        }
        if (null != parent) {
            parent.setValue(observable, value, key);
        }
    }
    
    @Override
    public void setValue(IObservable observable, Double value, Object key) {
        boolean doit = true;
        if (null != value && VALIDATE.contains(observable) && key instanceof ComponentKey) {
            Double val = getPipeline().validateComponent((ComponentKey) key, observable, value, false, this);
            if (null != val) {
                value = val;
            } else {
                doit = false;
            }
        }
        if (doit) {
            doSetValue(observable, value, key);
        }
    }

    /**
     * Sets the value of the given <code>observable</code> by replacing the existing one.
     * 
     * @param observable the observable to be modified
     * @param value the new value (may be <b>null</b> but then this method will perform an update of the data)
     * @param key the key representing the compound in a composite observation,
     *   may be <b>null</b>
     */
    private void doSetValue(IObservable observable, Double value, Object key) {
        super.setValue(observable, value, key);
        if (null != current) {
            current.setValue(observable, value, key);
        }            
        if (null != parent) {
            parent.setValue(observable, value, key);
        }
    }

    @Override
    public void incrementValue(IObservable observable, double value, Object key) {
        boolean doit = true;
        if (VALIDATE.contains(observable) && key instanceof ComponentKey) {
            Double val = getPipeline().validateComponent((ComponentKey) key, observable, value, true, this);
            if (val == 0.0) {
                doSetValue(observable, 0.0, key);
                doit = false;
            } else if (null != val) {
                value = val;
            } else {
                doit = false;
            }
        }
        if (doit) {
            super.incrementValue(observable, value, key);
            if (null != current) {
                current.incrementValue(observable, value, key);
            }
            if (null != parent) {
                parent.incrementValue(observable, value, key);
            }
        }
    }
    
    @Override
    public void incrementValue(IObservable observable, Double value, Object key) {
        boolean doit = true;
        if (null != value && VALIDATE.contains(observable) && key instanceof ComponentKey) {
            Double val = getPipeline().validateComponent((ComponentKey) key, observable, value, true, this);
            if (val == 0.0) {
                doSetValue(observable, 0.0, key);
                doit = false;
            } else if (null != val) {
                value = val;
            } else {
                doit = false;
            }
        } 
        if (doit) {
            super.incrementValue(observable, value, key);
            if (null != current) {
                current.incrementValue(observable, value, key);
            }
            if (null != parent) {
                parent.incrementValue(observable, value, key);
            }
        }
    }

    @Override
    public void clearComponents(IObservable observable, Collection<Object> keys) {
        super.clearComponents(observable, keys);
        if (null != parent) {
            parent.clearComponents(observable, keys);
        }
    }

    
    /**
     * Whether thrift shall be used for monitoring. This is intended as a hint to the Monitoring layer.
     * 
     * @return <code>true</code> for thrift, <code>false</code> else
     */
    public boolean useThrift() {
        return useThrift;
    }

    /**
     * Returns the topology provider for this node.
     * 
     * @return the topology provider
     */
    private ITopologyProvider getParentTopologyProvider() {
        return null == pipeline ? parent : pipeline;
    }
    
    @Override
    public PipelineTopology getTopology() {
        return getParentTopologyProvider().getTopology();
    }

    @Override
    public ITopologyProjection getTopologyProjection() {
        ITopologyProjection result = null;
        ITopologyProvider provider = getParentTopologyProvider();
        PipelineTopology topology = provider.getTopology();
        if (null != topology) {
            Processor proc = topology.getProcessor(getName());
            if (null != pipeline && null != current && null != proc) {
                List<Processor> start = new ArrayList<Processor>();
                List<Processor> end = new ArrayList<Processor>();
                Map<Stream, Processor> next = new HashMap<Stream, Processor>();
                start.add(proc);
                List<Processor> enableNext = current.projectTopologyNodes(true);
                for (int o = 0; o < proc.getOutputCount(); o++) {
                    Stream out = proc.getOutput(o);
                    Processor target = out.getTarget();
                    if (enableNext.contains(target)) {
                        next.put(out, target);
                    } else {
                        next.put(out, null);
                    }
                }
                end.addAll(current.projectTopologyNodes(false));
                result = new TopologyProjection(start, end, next);
            } else { // algorithm situation / fallback
                result = new SingleNodeTopologyProjection(proc);
            }
        }
        return result;
    }

    @Override
    public PipelineNodeSystemPart getNode(String name) {
        return getParentTopologyProvider().getNode(name);
    }
    
}