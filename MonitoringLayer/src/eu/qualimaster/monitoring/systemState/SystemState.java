package eu.qualimaster.monitoring.systemState;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import eu.qualimaster.coordination.INameMapping;
import eu.qualimaster.coordination.INameMapping.Algorithm;
import eu.qualimaster.coordination.INameMapping.Component;
import eu.qualimaster.coordination.INameMapping.Component.Type;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.monitoring.events.FrozenSystemState;
import eu.qualimaster.monitoring.observations.ISystemStateConfigurer;

/**
 * Stores the current system state. The observation instances as well as the default observables per part
 * are provided by the {@link eu.qualimaster.monitoring.observations.ObservationFactory}. This structure aims at 
 * being thread-safe.
 * 
 * @author Holger Eichelberger
 */
public class SystemState implements Serializable {

    private static final long serialVersionUID = 1988917631202866652L;
    private static ISystemStateConfigurer configurer;
    
    private PlatformSystemPart platform; 
    private final Map<String, PipelineSystemPart> pipelines = Collections.synchronizedMap(
        new HashMap<String, PipelineSystemPart>());
    private boolean enableAlgorithmTracing = false;
    private long timeStamp = System.currentTimeMillis();

    /**
     * Creates a new system state.
     */
    public SystemState() {
        platform = new PlatformSystemPart();
    }

    /**
     * Creates a new system state by copying the <code>source</code>.
     * 
     * @param source the source system state to copy
     */
    public SystemState(SystemState source) {
        this.platform = new PlatformSystemPart(source.platform, this); // no algorithms needed
        synchronized (pipelines) {
            for (Map.Entry<String, PipelineSystemPart> entry : source.pipelines.entrySet()) {
                pipelines.put(entry.getKey(), new PipelineSystemPart(entry.getValue(), this));
            }
        }
        // no enable tracing to avoid accidents
    }
    
    /**
     * Returns the timestamp of creation.
     * 
     * @return the timestamp
     */
    public long getTimestamp() {
        return timeStamp;
    }
    
    /**
     * Returns the system part representing the (runtime) platform.
     * 
     * @return the platform system part
     */
    public PlatformSystemPart getPlatform() {
        return platform;
    }
    
    /**
     * Closes a platform trace if existent.
     */
    public void closePlatformTrace() {
        PlatformSystemPart.closeTrace();
    }

    /**
     * Closes all algorithm traces.
     */
    public void closeAlgorithmTraces() {
        synchronized (pipelines) {
            for (PipelineSystemPart pip : pipelines.values()) {
                pip.closeAlgorithmTraces();
            }
        }
    }
    
    /**
     * Enables algorithm tracing for new pipelines. Disabling the traces calls {@link #closeAlgorithmTraces()}.
     * 
     * @param enableAlgorithmTracing <code>true</code> if algorithm tracing shall be enabled, <code>false</code> else
     */
    public void enableAlgorithmTracing(boolean enableAlgorithmTracing) {
        this.enableAlgorithmTracing = enableAlgorithmTracing;
        if (!enableAlgorithmTracing) {
            closeAlgorithmTraces();
        }
    }
    
    /**
     * Returns whether algorithm tracing shall be enabled.
     * 
     * @return <code>true</code> if algorithm tracing shall be enabled, <code>false</code> else
     */
    public boolean doAlgorithmTracing() {
        return enableAlgorithmTracing;
    }

    /**
     * Returns whether there is already a pipeline system part for <code>pipelineName</code>.
     * 
     * @param pipelineName the name of the pipeline
     * @return <code>true</code> of there is a pipeline system part, <code>false</code> else
     */
    public boolean hasPipeline(String pipelineName) {
        synchronized (pipelines) {
            return pipelines.containsKey(pipelineName);
        }
    }

    /**
     * Returns a pipeline system part for <code>pipelineName</code>.
     * 
     * @param pipelineName the name of the pipeline
     * @return the system state monitored for the pipeline (may be <b>null</b> if not found)
     */
    public PipelineSystemPart getPipeline(String pipelineName) {
        synchronized (pipelines) {
            return pipelines.get(pipelineName);
        }
    }
    
    /**
     * Returns a pipeline system part for <code>pipelineName</code> or creates the pipeline node.
     * 
     * @param pipelineName the name of the pipeline
     * @return the system state monitored for the pipeline
     */
    public PipelineSystemPart obtainPipeline(String pipelineName) {
        synchronized (pipelines) {
            PipelineSystemPart part = pipelines.get(pipelineName);
            if (null == part) {
                synchronized (pipelines) {
                    part = new PipelineSystemPart(pipelineName, this);
                    pipelines.put(pipelineName, part);
                }
            }
            return part;
        }
    }
    
    /**
     * Returns an unmodified copy of the actual known pipelines.
     * 
     * @return the pipelines
     */
    public Collection<PipelineSystemPart> getPipelines() {
        synchronized (pipelines) {
            return Collections.unmodifiableCollection(pipelines.values());
        }
    }
    
    /**
     * Removes the pipeline from the system state.
     * 
     * @param pipelineName the pipeline to be removed
     * @return the removed pipeline, may be <b>null</b> if unknown
     */
    public PipelineSystemPart removePipeline(String pipelineName) {
        synchronized (pipelines) {
            return pipelines.remove(pipelineName);
        }
    }
    
    /**
     * Sets the configurer for additional observables.
     * 
     * @param sysConfigurer the configurer
     */
    public static void setConfigurer(ISystemStateConfigurer sysConfigurer) {
        configurer = sysConfigurer;
    }

    /**
     * Returns the configurer for additional observables.
     * 
     * @return the configurer
     */
    public static ISystemStateConfigurer getConfigurer() {
        return configurer;
    }
    
    /**
     * Clears the system state (for testing).
     */
    public void clear() {
        platform.clear();
        synchronized (pipelines) {
            for (PipelineSystemPart pip : pipelines.values()) {
                pip.clear(); // includes closeAlgorithmTraces();
            }
            pipelines.clear();
        }
    }
    
    /**
     * Returns the status of all pipelines.
     * 
     * @return the status of all pipelines
     */
    public synchronized Map<String, PipelineLifecycleEvent.Status> getPipelinesStatus() {
        Map<String, PipelineLifecycleEvent.Status> result = new HashMap<String, PipelineLifecycleEvent.Status>();
        synchronized (pipelines) {
            for (Map.Entry<String, PipelineSystemPart> entry : pipelines.entrySet()) {
                PipelineSystemPart pip = entry.getValue();
                result.put(entry.getKey(), pip.getStatus());
            }
        }
        return result;
    }

    /**
     * Freezes the system state.
     * 
     * @return the frozen system state
     */
    public synchronized FrozenSystemState freeze() {
        FrozenSystemState result = new FrozenSystemState();
        platform.fill(FrozenSystemState.INFRASTRUCTURE, FrozenSystemState.INFRASTRUCTURE_NAME, result, null);
        synchronized (pipelines) {
            for (Map.Entry<String, PipelineSystemPart> entry : pipelines.entrySet()) {
                PipelineSystemPart pip = entry.getValue();
                pip.fill(FrozenSystemState.PIPELINE, entry.getKey(), result, pip.getOverloadModifiers());
            }
        }
        return result;
    }

    /**
     * Freezes the system state of the specified <code>pipeline</code> and the overall infrastructure. 
     * information. No pipeline information is added, if <code>pipeline</code> is <b>null</b> or it cannot
     * be found.
     * 
     * @param pipeline the name of the pipeline to return the state for
     * @return the frozen system state projected to <code>pipeline</code>
     */
    public synchronized FrozenSystemState freeze(String pipeline) {
        FrozenSystemState result = new FrozenSystemState();
        platform.fill(FrozenSystemState.INFRASTRUCTURE, FrozenSystemState.INFRASTRUCTURE_NAME, result, null);
        if (null != pipeline) {
            synchronized (pipelines) {
                PipelineSystemPart pip = pipelines.get(pipeline);
                if (null != pip) {
                    pip.fill(FrozenSystemState.PIPELINE, pipeline, result, pip.getOverloadModifiers());
                }
            }
        }
        return result;
    }
    
    
    /**
     * Returns the pipeline node part for a given implementation name.
     * 
     * @param mapping the pipeline mapping
     * @param part the pipeline system state part
     * @param implName the node name
     * @return the pipeline node part
     */
    public static PipelineNodeSystemPart getNodePart(INameMapping mapping, PipelineSystemPart part, String implName) {
        PipelineNodeSystemPart result = null;
        String pipElementName = null;
        Component subC = mapping.getComponentByImplName(implName);
        if (null != subC && (Type.UNKNOWN == subC.getType() || Type.HARDWARE == subC.getType())) {
            Algorithm alg = mapping.getAlgorithmByImplName(subC.getContainer());
            if (null == alg) {
                alg = mapping.getAlgorithm(subC.getContainer()); // fallback
            }
            if (null != alg) {
                NodeImplementationSystemPart algPart = part.getAlgorithm(alg.getName());
                if (null != algPart) {
                    result = algPart.obtainPipelineNode(subC.getName());
                }
            }
        }
        if (null == result) {
            String tmp = mapping.getPipelineNodeByImplName(implName);
            if (null == tmp) {
                pipElementName = implName;
            } else {
                pipElementName = tmp;
            }
        }
        if (null == result) {
            result = part.obtainPipelineNode(pipElementName); // exists or creates
        }
        return result;
    }

    /**
     * Creates a set with entries.
     * 
     * @param <T> the entry type
     * @param cls the class to create the set for
     * @param values the values
     * @return the set with values
     */
    @SafeVarargs
    static <T> Set<T> createSet(Class<T> cls, T... values) {
        Set<T> result = new HashSet<T>();
        for (T t : values) {
            result.add(t);
        }
        return result;
    }
    
    @Override
    public String toString() {
        return "platform " + platform + "; pipelines " + pipelines;
    }

    /**
     * Creates a textual representation but in contrast to {@link #toString()} in this case performs pretty printing.
     * 
     * @return the textual representation
     */
    public String format() {
        return "platform " + platform.format("") + "\npipelines " + SystemPart.format(pipelines, " ");
    }

}
