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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;

import eu.qualimaster.adaptation.events.SourceVolumeAdaptationEvent;
import eu.qualimaster.coordination.INameMapping;
import eu.qualimaster.coordination.INameMapping.Algorithm;
import eu.qualimaster.coordination.INameMapping.Component;
import eu.qualimaster.coordination.INameMapping.Component.Type;
import eu.qualimaster.events.AbstractReturnableEvent;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.events.IReturnableEvent;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.infrastructure.PipelineOptions;
import eu.qualimaster.monitoring.MonitoringManager;
import eu.qualimaster.monitoring.MonitoringManager.PipelineInfo;
import eu.qualimaster.monitoring.events.ComponentKey;
import eu.qualimaster.monitoring.events.FrozenSystemState;
import eu.qualimaster.monitoring.parts.IPartType;
import eu.qualimaster.monitoring.parts.PartType;
import eu.qualimaster.monitoring.topology.ITopologyProvider;
import eu.qualimaster.monitoring.topology.PipelineTopology;
import eu.qualimaster.monitoring.topology.ITopologyProjection;
import eu.qualimaster.monitoring.topology.PipelineTopology.Processor;
import eu.qualimaster.monitoring.topology.PipelineTopology.Stream;
import eu.qualimaster.monitoring.topology.TopologyProjection;
import eu.qualimaster.monitoring.tracing.ITrace;
import eu.qualimaster.monitoring.tracing.Tracing;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.ResourceUsage;
import eu.qualimaster.observables.Scalability;

/**
 * Stores monitoring information for a pipeline system part.
 * 
 * @author Holger Eichelberger
 */
public class PipelineSystemPart extends SystemPart implements ITopologyProvider {

    private static final long serialVersionUID = -174499344220659669L;
    private Map<String, PipelineNodeSystemPart> elements = new HashMap<String, PipelineNodeSystemPart>();
    private Map<String, PipelineNodeSystemPart> algElements = new HashMap<String, PipelineNodeSystemPart>();
    private Map<String, PipelineNodeSystemPart> allElements = new HashMap<String, PipelineNodeSystemPart>();
    private Map<String, NodeImplementationSystemPart> algorithms = 
        Collections.synchronizedMap(new HashMap<String, NodeImplementationSystemPart>());
    private Map<String, NodeImplementationSystemPart> sources = 
        Collections.synchronizedMap(new HashMap<String, NodeImplementationSystemPart>());
    private Map<String, NodeImplementationSystemPart> sinks = 
        Collections.synchronizedMap(new HashMap<String, NodeImplementationSystemPart>());
    private PipelineLifecycleEvent.Status status = PipelineLifecycleEvent.Status.UNKNOWN;
    private long lastStateChange;
    private transient PipelineTopology topology;
    private transient ITopologyProjection topologyProjection;
    private SystemState state;
    private transient SourceVolumeAdaptationEvent overloadEvent;
    
    @SuppressWarnings("serial")
    private IReturnableEvent currentOrigin = new AbstractReturnableEvent() { };

    /**
     * Creates a pipeline system part without attached topology information.
     * 
     * @param name the descriptive name of the pipeline
     * @param state the parent state
     */
    public PipelineSystemPart(String name, SystemState state) {
        super(PartType.PIPELINE, name);
        this.state = state;
    }
    
    /**
     * Creates a copy of this system part.
     * 
     * @param source the source system part
     * @param state the parent system state for crosslinks
     */
    protected PipelineSystemPart(PipelineSystemPart source, SystemState state) {
        super(source, state);
        this.state = state;
        this.lastStateChange = source.lastStateChange;
        this.status = source.status;
        synchronized (elements) {
            for (Map.Entry<String, PipelineNodeSystemPart> entry : source.elements.entrySet()) {
                this.elements.put(entry.getKey(), new PipelineNodeSystemPart(entry.getValue(), state, this));
            }
        }
        synchronized (algorithms) {
            for (Map.Entry<String, NodeImplementationSystemPart> entry : source.algorithms.entrySet()) {
                NodeImplementationSystemPart impl = new NodeImplementationSystemPart(entry.getValue(), this, state); 
                algorithms.put(entry.getKey(), impl);
                // don't fill algElements here, done below in establishAlgNodesLinks
            }
        }
        synchronized (sources) {
            for (Map.Entry<String, NodeImplementationSystemPart> entry : source.sources.entrySet()) {
                sources.put(entry.getKey(), new NodeImplementationSystemPart(entry.getValue(), this, state));
            }
        }
        synchronized (sinks) {
            for (Map.Entry<String, NodeImplementationSystemPart> entry : source.sinks.entrySet()) {
                sinks.put(entry.getKey(), new NodeImplementationSystemPart(entry.getValue(), this, state));
            }
        }
        this.currentOrigin.setSenderId(source.currentOrigin.getSenderId());
        this.currentOrigin.setMessageId(source.currentOrigin.getMessageId());
        synchronized (elements) {
            for (Map.Entry<String, PipelineNodeSystemPart> entry : source.elements.entrySet()) {
                getNode(entry.getKey()).adjustCurrent(entry.getValue());
            }
            for (Map.Entry<String, PipelineNodeSystemPart> entry : elements.entrySet()) {
                establishAlgNodesLinks(entry.getValue());
            }
        }
        synchronized (algorithms) {
            for (Map.Entry<String, NodeImplementationSystemPart> entry : source.algorithms.entrySet()) {
                getAlgorithm(entry.getKey()).adjustReferences(entry.getValue());
            }
        }
        synchronized (sources) {
            for (Map.Entry<String, NodeImplementationSystemPart> entry : source.sources.entrySet()) {
                getSource(entry.getKey()).adjustReferences(entry.getValue());
            }
        }
        synchronized (sinks) {
            for (Map.Entry<String, NodeImplementationSystemPart> entry : source.sinks.entrySet()) {
                getSink(entry.getKey()).adjustReferences(entry.getValue());
            }
        }
        setTopology(source.topology); // readonly, can be copied
        // all elements happens through copying / register node
    }
    
    /**
     * Assigns an overload event affecting the monitored values passed to a frozen system state.
     * 
     * @param overloadEvent the overload event (<b>null</b> for none)
     */
    public void setOverloadEvent(SourceVolumeAdaptationEvent overloadEvent) {
        this.overloadEvent = overloadEvent;
    }
    
    /**
     * Returns the overload event.
     * 
     * @return the overload event (<b>null</b> for none)
     */
    public SourceVolumeAdaptationEvent getOverloadEvent() {
        return overloadEvent;
    }
    
    /**
     * Returns observable adjustment factors for freezing.
     * 
     * @return the adjustment factors, may be <b>null</b> for none
     */
    protected Map<IObservable, Double> getAdjustmentFactors() {
        Map<IObservable, Double> factors = null;
        if (null != overloadEvent) {
            double dev = overloadEvent.getAverageDeviations();
            if (dev > 0) {
                double factor = 1 + dev;
                factors = new HashMap<IObservable, Double>();
                factors.put(Scalability.ITEMS, factor);
                factors.put(Scalability.PREDECESSOR_ITEMS, factor);
                factors.put(ResourceUsage.CAPACITY, factor);
                // throughput/latency shall follow
                LogManager.getLogger(getClass()).info("Current adjustment factors: " + factors);
            }
        }
        return factors;
    }

    
    @Override
    public PipelineTopology getTopology() {
        return topology;
    }
    
    /**
     * Defines the pipeline topology description for this pipeline.
     * 
     * @param topology the topology description (may be <b>null</b> if the pipeline is not fully initialized)
     */
    public void setTopology(PipelineTopology topology) {
        this.topology = topology;
        if (null != topology) {
            List<Processor> start = TopologyProjection.fillStart(null, topology);
            List<Processor> end = TopologyProjection.fillEnd(null, topology);
            topologyProjection = new TopologyProjection(start, end, bypassAlgorithms());
        } else {
            topologyProjection = null;
        }
    }
    
    /**
     * Projects the algorithms, i.e., returns a stream-next mapping to bypass them.
     * 
     * @return the mapping
     */
    private Map<Stream, Processor> bypassAlgorithms() {
        Map<Stream, Processor> next = new HashMap<Stream, Processor>();
        INameMapping mapping = getNameMapping();
        Map<Processor, List<Processor>> done = new HashMap<Processor, List<Processor>>();
        for (Algorithm alg : mapping.getAlgorithms()) { // stable information
            List<Component> comps = alg.getComponents();
            if (null != comps && !comps.isEmpty()) {
                Set<String> compsSet = new HashSet<String>();
                for (int c = 0; c < comps.size(); c++) {
                    compsSet.add(comps.get(c).getName());
                }
                // bypass them
                List<Stream> sourceStreams = new ArrayList<Stream>();
                List<Stream> sinkStreams = new ArrayList<Stream>();
                for (int c = 0; c < comps.size(); c++) {
                    Component comp = comps.get(c);
                    Processor proc = topology.getProcessor(comp.getName());
                    if (null != proc) {
                        for (int i = 0; i < proc.getInputCount(); i++) { // target is in alg
                            Stream in = proc.getInput(i);
                            if (compsSet.contains(in.getOrigin().getName())) {
                                next.put(in, null); // origin and target in alg -> do not traverse
                            } else {
                                sourceStreams.add(in);
                            }
                        }
                        for (int o = 0; o < proc.getOutputCount(); o++) { // origin is in alg
                            Stream out = proc.getOutput(o);
                            if (compsSet.contains(out.getTarget().getName())) {
                                next.put(out, null); // origin and target in alg -> do not traverse
                            } else {
                                sinkStreams.add(out);
                            }
                        }
                    }
                }
                for (int src = 0; src < sourceStreams.size(); src++) {
                    Stream source = sourceStreams.get(src);
                    Processor origin = source.getOrigin();
                    List<Processor> doneTargets = done.get(origin);
                    if (sinkStreams.isEmpty()) { // profiling: edge goes in but not out, exclude also those edges
                        next.put(source, null);
                    } else {
                        for (int snk = 0; snk < sinkStreams.size(); snk++) {
                            Processor target = sinkStreams.get(snk).getTarget();
                            if (null == doneTargets) {
                                doneTargets = new ArrayList<Processor>();
                                done.put(origin, doneTargets);
                            }
                            if (!doneTargets.contains(target)) {
                                doneTargets.add(target);
                                next.put(source, target);
                            } else {
                                next.put(source, null);
                            }
                        }
                    }
                }
            }
        }
        return next;
    }
    
    /**
     * Returns the time stamp of the last (effective) state change.
     * 
     * @return the timestamp
     */
    public long getLastStateChange() {
        return lastStateChange;
    }

    /**
     * Changes the status of a pipeline. Shall be called by monitoring only. Please use with care.
     * 
     * @param status the new status
     * @param checkNotify whether the infrastructure shall be notified about this new state; for received 
     *     infrastructure events this method must be called with <code>false</code> 
     * @return <code>true</code> if the new status was accepted and the pipeline status was changed, 
     *   <code>false</code> else
     */
    public boolean changeStatus(PipelineLifecycleEvent.Status status, boolean checkNotify) {
        return changeStatus(status, checkNotify, null);
    }
    
    /**
     * Changes the status of a pipeline. Shall be called by monitoring only. Please use with care.
     * 
     * @param status the new status
     * @param checkNotify whether the infrastructure shall be notified about this new state; for received 
     *     infrastructure events this method must be called with <code>false</code> 
     * @param cause optional cause to set the current sender/messageId, may be <b>null</b>
     * @return <code>true</code> if the new status was accepted and the pipeline status was changed, 
     *   <code>false</code> else
     */
    public boolean changeStatus(PipelineLifecycleEvent.Status status, boolean checkNotify, 
        PipelineLifecycleEvent cause) {
        if (null != cause) {
            currentOrigin.setMessageId(cause.getCauseMessageId());
            currentOrigin.setSenderId(cause.getCauseSenderId());
        }
        PipelineLifecycleEvent.Status oldStatus = this.status;
        if (PipelineLifecycleEvent.Status.UNKNOWN != status) {
            boolean notifyNewStatus = false;
            if (checkNotify && oldStatus != status) {
                // notify only if the status is changing to a status maintained by monitoring
                notifyNewStatus = PipelineLifecycleEvent.Status.DISAPPEARED == status
                    || PipelineLifecycleEvent.Status.CREATED == status
                    || PipelineLifecycleEvent.Status.INITIALIZED == status
                    || PipelineLifecycleEvent.Status.STARTED == status;
            }
            this.status = status;
            if (oldStatus != status) {
                lastStateChange = System.currentTimeMillis();
            }
            if (notifyNewStatus) {
                PipelineOptions opts = new PipelineOptions();
                PipelineInfo info = MonitoringManager.getPipelineInfo(getName());
                if (null != info) {
                    opts.merge(info.getOptions());
                }
                EventManager.send(new PipelineLifecycleEvent(getName(), this.status, opts, currentOrigin));
            }
        }
        return oldStatus != this.status;
    }
    
    /**
     * Returns the actual status of the pipeline.
     * 
     * @return the status
     */
    public PipelineLifecycleEvent.Status getStatus() {
        return status;
    }
    
    /**
     * Returns whether a pipeline is shutting down / was shut down.
     * 
     * @return <code>true</code> for shutting down, <code>false</code> else
     */
    public boolean isShuttingDown() {
        return null != status && status.isShuttingDown();
    }
    
    /**
     * Returns the stored pipeline nodes.
     * 
     * @return the pipeline nodes
     */
    public Collection<PipelineNodeSystemPart> getNodes() {
        synchronized (elements) {
            return elements.values();
        }
    }

    /**
     * Returns a node of a pipeline via its node name.
     * 
     * @param nodeName the name of the pipeline element
     * @return the related system part (may be <b>null</b> if not found)
     */
    public PipelineNodeSystemPart getPipelineNode(String nodeName) {
        synchronized (elements) {
            return getPipelineNodeImpl(nodeName);
        }
    }
    
    /**
     * Returns the name mapping for this pipeline.
     * 
     * @return the name mapping
     */
    public INameMapping getNameMapping() {
        return MonitoringManager.getNameMapping(getName());        
    }
    
    /**
     * Returns a pipeline node without synchronization, i.e., assuming that we are in a thread-safe code part.
     * 
     * @param nodeName the name of the pipeline element
     * @return the related system part (may be <b>null</b> if not found)
     */
    private PipelineNodeSystemPart getPipelineNodeImpl(String nodeName) {
        PipelineNodeSystemPart result = elements.get(nodeName);
        if (null == result) {
            result = algElements.get(nodeName);
        }
        return result;
    }
    
    /**
     * Returns a node of a pipeline via its node name or creates it if it does not exist.
     * 
     * @param nodeName the name of the pipeline element
     * @return the related system part 
     */
    public PipelineNodeSystemPart obtainPipelineNode(String nodeName) {
        synchronized (elements) {
            PipelineNodeSystemPart result = getPipelineNodeImpl(nodeName);
            if (null == result) {
                INameMapping mapping = getNameMapping();
                if (elements.isEmpty()) {
                    LogManager.getLogger(PipelineSystemPart.class).info("Initializing pipeline state for nodes " 
                        + mapping.getPipelineNodeNames());
                    // pre-initialize top-level nodes to avoid sequence dependency of events
                    for (String node : mapping.getPipelineNodeNames()) {
                        obtainPipelineNode(mapping, node);
                    }
                    LogManager.getLogger(PipelineSystemPart.class).info("Initialized pipeline state for nodes " 
                            + mapping.getPipelineNodeNames());
                    result = getPipelineNodeImpl(nodeName);
                } 
                if (null == result) {
                    result = obtainPipelineNode(mapping, nodeName);
                }
            }
            return result;
        }
    }

    /**
     * creates a node of a pipeline via its node name.
     * 
     * @param mapping the name mapping
     * @param nodeName the name of the pipeline element
     * @return the created system part 
     */
    private PipelineNodeSystemPart obtainPipelineNode(INameMapping mapping, String nodeName) {
        Component component = mapping.getPipelineNodeComponent(nodeName);
        Type type;
        boolean useThrift;
        if (null == component) { // assume legacy sub-topology
            type = Type.UNKNOWN;
            useThrift = true;
        } else {
            type = component.getType();
            useThrift = component.useThrift();
        }
        LogManager.getLogger(PipelineSystemPart.class).info("Creating pipeline state node: " + nodeName + " type " 
            + type + " useThrift " + useThrift + " component " + component + " from mapping " + mapping);
        PipelineNodeSystemPart result = new PipelineNodeSystemPart(nodeName, type, useThrift, this); 
        elements.put(nodeName, result);
        establishAlgNodesLinks(result);
        return result;
    }

    /**
     * Establishes the links between node, its alternative algorithms and their implementation nodes if existent.
     * 
     * @param node the node to establish the links for
     */
    private void establishAlgNodesLinks(PipelineNodeSystemPart node) {
        INameMapping mapping = getNameMapping();
        Component component = mapping.getPipelineNodeComponent(node.getName());
        if (null != component) {
            for (String alt : component.getAlternatives()) {
                NodeImplementationSystemPart algPart = getAlgorithm(alt);
                if (null != algPart) {
                    Algorithm alg = mapping.getAlgorithm(alt);
                    if (null != alg) {
                        for (Component comp : alg.getComponents()) {
                            PipelineNodeSystemPart compNode 
                                = algPart.obtainPipelineNode(comp.getName()); // force creation
                            algPart.link(compNode, ILinkSelector.ALL_EXTERNAL);
                            // elements may cause endless recursions in aggregation
                            algElements.put(comp.getName(), compNode);
                        }
                    }
                    node.link(algPart);
                }
            }
        }
    }

    /**
     * Clears this system part.
     */
    protected void clear() {
        super.clear();
        synchronized (elements) {
            elements.clear();
        }
        closeAlgorithmTraces();
        synchronized (algorithms) {
            algorithms.clear();
        }
        synchronized (sources) {
            sources.clear();
        }
        synchronized (sinks) {
            sinks.clear();
        }
    }

    @Override
    protected void fill(String prefix, String name, FrozenSystemState state, Map<IObservable, Double> factors) {
        super.fill(prefix, name, state, factors);
        synchronized (elements) {
            for (Map.Entry<String, PipelineNodeSystemPart> entry : elements.entrySet()) {
                PipelineNodeSystemPart part = entry.getValue();
                if (!part.isInternal()) {
                    part.fill(FrozenSystemState.PIPELINE_ELEMENT, 
                        FrozenSystemState.obtainPipelineElementSubkey(name, entry.getKey()), state, factors);
                }
            }
        }
        synchronized (algorithms) {
            for (Map.Entry<String, NodeImplementationSystemPart> entry : algorithms.entrySet()) {
                entry.getValue().fill(FrozenSystemState.ALGORITHM, 
                    FrozenSystemState.obtainPipelineElementSubkey(name, entry.getKey()), state, factors);
            }
        }
        synchronized (sources) {
            for (Map.Entry<String, NodeImplementationSystemPart> entry : sources.entrySet()) {
                entry.getValue().fill(FrozenSystemState.DATASOURCE, 
                    FrozenSystemState.obtainPipelineElementSubkey(name, entry.getKey()), state, factors);
            }
        }
        synchronized (sinks) {
            for (Map.Entry<String, NodeImplementationSystemPart> entry : sources.entrySet()) {
                entry.getValue().fill(FrozenSystemState.DATASINK, 
                    FrozenSystemState.obtainPipelineElementSubkey(name, entry.getKey()), state, factors);
            }
        }
    }
    
    /**
     * Returns whether the thread ids of <code>key1</code> and <code>key2</code> are valid but not the same.
     * 
     * @param key1 the first key
     * @param key2 the second key
     * @return <code>true</code> if the thread ids are valid but not the same, <code>false</code> else
     */
    private boolean threadsValidAndNotSame(ComponentKey key1, ComponentKey key2) {
        boolean result = false;
        long threadId1 = key1.getThreadId();
        long threadId2 = key2.getThreadId();
        if (threadId1 >= 0 && threadId2 >= 0) {
            result = threadId1 != threadId2;
        }
        return result;
    }

    /**
     * Returns whether the thread ids of <code>key1</code> and <code>key2</code> are valid and the same.
     * 
     * @param key1 the first key
     * @param key2 the second key
     * @return <code>true</code> if the thread ids are valid but not the same, <code>false</code> else
     */
    private boolean threadsValidAndSame(ComponentKey key1, ComponentKey key2) {
        boolean result = false;
        long threadId1 = key1.getThreadId();
        long threadId2 = key2.getThreadId();
        if (threadId1 >= 0 && threadId2 >= 0) {
            result = threadId1 == threadId2;
        }
        return result;
    }
    
    /** 
     * Validates the given key with the given observable and value and adjusts the internal structures if needed.
     * 
     * @param key the key to look for
     * @param observable the observable to apply the validation for
     * @param value the value
     * @param increment set or increment <code>value</code>
     * @param caller the calling system part for co-key removal
     * @return <code>value</code> if key is valid (new), <code>0.0</code> if there is an overlapping thread (do not 
     *     increment), <code>null</code> else
     */
    public Double validateComponent(ComponentKey key, IObservable observable, double value, boolean increment, 
        SystemPart caller) {
        boolean considerThreads = ResourceUsage.EXECUTORS == observable;
        Double result = null;
        boolean found = false;
        ComponentKey removeKey = null;
        Set<Object> keys = getComponentKeys(observable);
        for (Object k : keys) {
            if (k instanceof ComponentKey) {
                ComponentKey exKey = (ComponentKey) k;
                if (exKey.getTaskId() == key.getTaskId()) {
                    found = true;
                    if (ComponentKey.tasksSame(exKey, key)) { // no migration
                        if (considerThreads && threadsValidAndNotSame(exKey, key)) { // splitted, merged
                            result = null == result ? value : result; // only if not done by considerThreads
                            removeKey = exKey;
                        } // one thread is invalid, consider the same, ignore input
                    } else { // other worker
                        result = null == result ? value : result; // only if not done by considerThreads
                        removeKey = exKey;
                    }
                } 
                if (considerThreads) {
                    if (threadsValidAndSame(exKey, key) && exKey.getHostName().equals(key.getHostName()) 
                        && exKey.getPort() == key.getPort() && exKey.getTaskId() != key.getTaskId()) { 
                        result = 0.0; // different tasks in same thread
                        increment = false;
                    }
                }
            }
        }
        if (null == result && !found) {
            result = value;
        }
        if (null != result && null != removeKey) {
            List<Object> remove = new ArrayList<Object>();
            remove.add(removeKey);
            clearComponents(observable, remove);
            caller.clearComponents(observable, remove);
        }
        if (null != result) {
            if (increment) {
                incrementValue(observable, result, key);
            } else {
                setValue(observable, result, key);
            }
        }
        return result;
    }
    
    /**
     * Returns a node implementation part for the given <code>type</code> and <code>name</code>.
     * 
     * @param type the part type
     * @param name the name of the part
     * @return the corresponding system (may be <b>null</b>)
     */
    public NodeImplementationSystemPart getPart(IPartType type, String name) {
        NodeImplementationSystemPart result = null;
        if (PartType.ALGORITHM == type || PartType.PIPELINE_NODE == type) {
            result = getAlgorithm(name);
        } else if (PartType.SINK == type) {
            result = getSink(name);
        } else if (PartType.SOURCE == type) {
            result = getSource(name);
        }
        return result;
    }
    
    /**
     * Returns an algorithm used within a pipeline.
     * 
     * @param algorithmName the name of the algorithm
     * @return the system part representing the algorithm
     */
    public synchronized NodeImplementationSystemPart getAlgorithm(String algorithmName) {
        synchronized (algorithms) {
            NodeImplementationSystemPart result = algorithms.get(algorithmName);
            if (null == result) {
                ITrace trace;
                if (state.doAlgorithmTracing()) {
                    trace = Tracing.createAlgorithmTrace(algorithmName);
                } else {
                    trace = null;
                }
                result = new NodeImplementationSystemPart(PartType.ALGORITHM, algorithmName, this, trace);
                algorithms.put(algorithmName, result);
            }
            return result;
        }
    }

    /**
     * Returns a source implementation used within a pipeline.
     * 
     * @param sourceName the name of the source
     * @return the system part representing the source
     */
    public synchronized NodeImplementationSystemPart getSource(String sourceName) {
        synchronized (sources) {
            NodeImplementationSystemPart result = sources.get(sourceName);
            if (null == result) {
                result = new NodeImplementationSystemPart(PartType.SOURCE, sourceName, this, null);
                sources.put(sourceName, result);
            }
            return result;
        }
    }

    /**
     * Returns a sink implementation used within a pipeline.
     * 
     * @param sinkName the name of the source
     * @return the system part representing the source
     */
    public synchronized NodeImplementationSystemPart getSink(String sinkName) {
        synchronized (sinks) {
            NodeImplementationSystemPart result = sinks.get(sinkName);
            if (null == result) {
                result = new NodeImplementationSystemPart(PartType.SINK, sinkName, this, null);
                sources.put(sinkName, result);
            }
            return result;
        }
    }

    /**
     * Returns all algorithms.
     * 
     * @return the algorithms
     */
    public Collection<NodeImplementationSystemPart> algorithms() {
        return algorithms.values();
    }

    /**
     * Returns all sources.
     * 
     * @return the sources
     */
    public Collection<NodeImplementationSystemPart> sources() {
        return sources.values();
    }

    /**
     * Returns all sinks.
     * 
     * @return the sinks
     */
    public Collection<NodeImplementationSystemPart> sinks() {
        return sinks.values();
    }

    /**
     * Closes all algorithm traces.
     */
    public void closeAlgorithmTraces() {
        synchronized (algorithms) {
            for (NodeImplementationSystemPart algorithm : algorithms.values()) {
                algorithm.close();
            }
        }
    }

    @Override
    public String format(String indent) {
        synchronized (elements) {
            return super.format(indent) + "\n  elements: " + format(elements, indent + " ") + "\n sources" 
                + format(sources, indent + " ") + "\n algorithms" + format(algorithms, indent + " ") + "\n sinks" 
                + format(sinks, indent + " ");
        }
    }

    @Override
    public String toString() {
        synchronized (elements) {
            return super.toString() + " elements: " + elements + "; sources" + sources + "; algorithms" + algorithms 
                + "; sinks" + sinks;
        }
    }

    @Override
    public ITopologyProjection getTopologyProjection() {
        return topologyProjection;
    }
    
    /**
     * Registers the given node as a (sub-)element of this pipeline.
     * 
     * @param part the part to be registered
     */
    void registerNode(PipelineNodeSystemPart part) {
        synchronized (allElements) {
            allElements.put(part.getName(), part);
        }
    }

    @Override
    public PipelineNodeSystemPart getNode(String name) {
        synchronized (allElements) {
            return allElements.get(name);
        }
    }

}