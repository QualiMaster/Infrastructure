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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.xtext.util.Arrays;

import eu.qualimaster.coordination.CoordinationManager;
import eu.qualimaster.coordination.INameMapping;
import eu.qualimaster.coordination.INameMapping.Algorithm;
import eu.qualimaster.coordination.INameMapping.Component;
import eu.qualimaster.coordination.events.AlgorithmProfilingEvent;
import eu.qualimaster.coordination.events.AlgorithmProfilingEvent.DetailMode;
import eu.qualimaster.monitoring.MonitoringConfiguration;
import eu.qualimaster.monitoring.events.FrozenSystemState;
import eu.qualimaster.monitoring.profiling.AlgorithmProfilePredictionManager;
import eu.qualimaster.monitoring.systemState.NodeImplementationSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineSystemPart;
import eu.qualimaster.monitoring.systemState.SystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.monitoring.topology.PipelineTopology;
import eu.qualimaster.monitoring.topology.PipelineTopology.Processor;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.ObservableComparator;
import eu.qualimaster.observables.ResourceUsage;
import eu.qualimaster.observables.Scalability;
import eu.qualimaster.observables.TimeBehavior;

/**
 * Implements tracing methods.
 * 
 * @author Holger Eichelberger
 */
public class Tracing {
    
    public static final String PREFIX_INFA = "qmInfra";
    public static final String TRACE_FILE_SUFFIX = ".csv";
    public static final SimpleDateFormat LOG_TAG_FORMAT = new SimpleDateFormat("yyyyMMdd-HHmmss");

    private static final Map<Class<?>, IObservable[]> LIMIT = new HashMap<>();
    private static final Map<Class<?>, IObservable[]> EXCLUDE = new HashMap<>();
    private static final Map<Class<?>, IObservable[]> SEQUENCES = new HashMap<>();

    private static boolean inTesting = false;
    private static Map<String, ITrace> profilingTraces = new HashMap<String, ITrace>();
    
    static {
        LIMIT.put(PipelineNodeSystemPart.class, 
            new IObservable[] {ResourceUsage.EXECUTORS, ResourceUsage.TASKS, Scalability.ITEMS});
        EXCLUDE.put(NodeImplementationSystemPart.class, 
            new IObservable[] {TimeBehavior.THROUGHPUT_ITEMS});
    }
    
    /**
     * Sets this class to testing mode (fixed file names, e.g., {@link #PREFIX_INFA}).
     * 
     * @param testing whether testing mode shall be activated or deactivated
     * @return the old testing mode
     */
    public static boolean inTesting(boolean testing) {
        boolean before = inTesting;
        inTesting = testing;
        return before;
    }

    /**
     * Creates a log tag (from the current time).
     * 
     * @param name the name of the element to log
     * @return the log tag
     */
    private static String getLogTag(String name) {
        return name + (inTesting ? "" : "-" + LOG_TAG_FORMAT.format(new Date()));
    }
    
    /**
     * Returns a trace for <code>algorithm</code>.
     * 
     * @param algorithm the algorithm to receive the trace for
     * @return the trace or <b>null</b> if no tracing is enabled
     */
    public static ITrace createAlgorithmTrace(String algorithm) {
        return createTrace(algorithm, MonitoringConfiguration.getMonitoringLogLocation());
    }
    
    /**
     * Returns the internal key for obtaining the related trace.
     * 
     * @param pipeline the pipeline
     * @param family the family
     * @param algorithm the algorithm
     * @return the key
     */
    private static String getAlgorithmProfileKey(String pipeline, String family, String algorithm) {
        return pipeline + "_" + family + "_" + algorithm;
    }

    /**
     * Handles an algorithm profiling event, i.e., creates or closes respective traces.
     * 
     * @param event the event
     */
    public static void handleEvent(AlgorithmProfilingEvent event) {
        String pipName = event.getPipeline();
        String famName = event.getFamily();
        String algName = event.getAlgorithm();
        
        INameMapping nameMapping = CoordinationManager.getNameMapping(pipName);
        Component famComp = nameMapping.getPipelineNodeComponent(famName);
        if (null != famComp) {
            famName = famComp.getName();
        }
        Algorithm algComp = nameMapping.getAlgorithm(algName);
        if (null != algComp) {
            algName = algComp.getImplName();
        }
        String key = getAlgorithmProfileKey(pipName, famName, algName);
        ITrace trace;
        switch (event.getStatus()) {
        case START:
            String logLocation = MonitoringConfiguration.getProfilingLogLocation();
            trace = createTrace(key + "-profile", logLocation);
            trace.setTraceMode(event.getDetailMode());
            trace.notifyNewSubTrace(event.getSettings());
            profilingTraces.put(key, trace);
            LogManager.getLogger(Tracing.class).info("Created monitoring trace: " + key + " " + trace);
            break;
        case NEXT:
            trace = profilingTraces.get(key);
            if (null != trace) {
                trace.notifyNewSubTrace(event.getSettings());
                LogManager.getLogger(Tracing.class).info("Notifying about subtrace: " + key + " " + trace);
            }
            break;
        case END:
            closeAlgorithmProfilingTrace(key);
            LogManager.getLogger(Tracing.class).info("Closed trace for: " + key);
            break;
        default:
            break;
        }
    }
    
    /**
     * Creates a test profiling trace on <code>out</code>. For testing only!
     * 
     * @param pipName the pipeline name
     * @param famName the family name
     * @param algName the algorithm name
     * @param out the output stream
     * @param mode the trace mode
     */
    public static final void test(String pipName, String famName, String algName, PrintStream out, DetailMode mode) {
        String key = getAlgorithmProfileKey(pipName, famName, algName);
        ITrace trace = new FileTrace(out);
        trace.setTraceMode(mode);
        profilingTraces.put(key, trace);
    }
    
    /**
     * Closes an algorithm profiling trace.
     * 
     * @param key the trace key ({@link #getAlgorithmProfileKey(String, String, String)})
     */
    private static void closeAlgorithmProfilingTrace(String key) {
        if (null != key) {
            ITrace trace = profilingTraces.remove(key);
            if (null != trace) {
                trace.close();
            }
        }
    }

    /**
     * Closes open traces.
     */
    public static void close() {
        List<String> keys = new ArrayList<String>();
        keys.addAll(profilingTraces.keySet());
        for (String key : keys) {
            closeAlgorithmProfilingTrace(key);
        }
    }
    
    /**
     * Returns a trace for the whole infrastructure.
     * 
     * @return the trace or <b>null</b> if no tracing is enabled
     */
    public static ITrace createInfrastructureTrace() {
        return createTrace(PREFIX_INFA, MonitoringConfiguration.getMonitoringLogInfraLocation());
    }

    /**
     * Returns a trace for the specified prefix.
     * 
     * @param prefix the prefix
     * @param logLocation the log location (from the configuration)
     * @return the trace or <b>null</b> if no tracing is enabled
     */
    public static ITrace createTrace(String prefix, String logLocation) {
        // reminder: profile traces shall be file traces
        ITrace result = null;
        if (!MonitoringConfiguration.isEmpty(logLocation)) {
            File logFile = new File(logLocation, getLogTag(prefix) + TRACE_FILE_SUFFIX);
            try {
                FileOutputStream out = new FileOutputStream(logFile);
                result = new FileTrace(new PrintStream(out));
            } catch (IOException e) {
                getLogger().error("cannot open output stream for trace " 
                    + prefix + ": " + e.getMessage());
            }
        }
        return result;
    }
    
    /**
     * Traces the whole infrastructure.
     * 
     * @param state the current system state
     * @param parameters the parameter (provider)
     */
    public static void traceInfrastructure(SystemState state, IParameterProvider parameters) {
        ITrace trace = state.getPlatform().getTrace();
        if (null != trace) {
            trace.traceInfrastructure(state, parameters);
        }
    }
    
    /**
     * Traces the algorithms of <code>state</code> with the given <code>parameters</code>.
     * 
     * @param state the state to trace the parameters for
     * @param parameters the parameter (provider)
     */
    public static void traceAlgorithms(SystemState state, IParameterProvider parameters) {
        for (PipelineSystemPart pip : state.getPipelines()) {
            traceAlgorithms(pip, parameters);
        }
    }

    /**
     * Traces the algorithms of <code>pipeline</code> with the given <code>parameters</code>.
     * 
     * @param pipeline the pipeline to trace
     * @param parameters the parameter (provider)
     */
    public static void traceAlgorithms(PipelineSystemPart pipeline, IParameterProvider parameters) {
        for (PipelineNodeSystemPart node : pipeline.getNodes()) {
            NodeImplementationSystemPart current = node.getCurrent();
            if (null != current) {
                ITrace trace = current.getTrace();
                if (null != trace) {
                    trace.traceAlgorithm(node, current, parameters);
                }
                String key = getAlgorithmProfileKey(pipeline.getName(), node.getName(), current.getName());
                trace = profilingTraces.get(key);
                if (null == trace) { // fallback - for subtopologies
                    Algorithm alg = pipeline.getNameMapping().getAlgorithm(current.getName());
                    if (null != alg) {
                        key = getAlgorithmProfileKey(pipeline.getName(), node.getName(), alg.getImplName());
                        trace = profilingTraces.get(key);
                    }
                }
                if (null != trace) {
                    trace.traceAlgorithm(node, current, parameters);
                }
                AlgorithmProfilePredictionManager.update(pipeline.getName(), node.getName(), node);
            }
        }
    }
    
    /**
     * Returns the observables in output sequence. Considers {@link #LIMIT} and {@link #EXCLUDE}.
     * 
     * @param part the part to sort the observables (and to cache them for)
     * @return the sorted observables
     * @see #getObservableSequence(Class, Collection)
     */
    static IObservable[] getObservableSequence(SystemPart part) {
        return getObservableSequence(part.getClass(), part.observables());
    }

    /**
     * Returns the observables in output sequence. Considers {@link #LIMIT} and {@link #EXCLUDE}.
     * 
     * @param cls the part class to sort the observables (and to cache them for)
     * @param observables the observables related to <code>cls</code>
     * @return the sorted observables
     */
    static IObservable[] getObservableSequence(Class<?> cls, Collection<IObservable> observables) {
        IObservable[] result = SEQUENCES.get(cls);
        if (null == result) {
            TreeSet<IObservable> tmp = new TreeSet<IObservable>(ObservableComparator.INSTANCE);
            IObservable[] limit = LIMIT.get(cls.getClass());
            IObservable[] exclude = EXCLUDE.get(cls.getClass());
            for (IObservable observable : observables) {
                if ((null == limit || Arrays.contains(limit, observable)) 
                    && (null == exclude || !Arrays.contains(exclude, observable))) {
                    tmp.add(observable);
                }
            }
            result = new IObservable[tmp.size()];
            SEQUENCES.put(cls, tmp.toArray(result));
        }
        return result;
    }
    
    /**
     * Logs the monitoring state of <code>pipeline</code>.
     * 
     * @param state the state to log
     * @param pipeline the name of the pipeline to be logged
     */
    public static void logMonitoringData(SystemState state, String pipeline) {
        if (null != pipeline) {
            String logLocation = MonitoringConfiguration.getMonitoringLogLocation();
            if (!MonitoringConfiguration.isEmpty(logLocation) && state.hasPipeline(pipeline)) {
                PipelineSystemPart pip = state.obtainPipeline(pipeline);
                File file = new File(logLocation, getLogTag(pipeline) + ".summary");
                FrozenSystemState fState = state.freeze(pipeline);
                Properties prop = fState.toProperties();
                prop.put("full.state", state.toString()); // debugging only
                PipelineTopology topology = pip.getTopology();
                if (null != topology) {
                    prop.put("full.topology", topology.toString()); // debugging only
                }
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    prop.store(fos, "pipeline state for '" + pipeline + "' saved by QM Monitoring Layer");
                    fos.close();
                } catch (IOException e) {
                    getLogger().error("while saving the monitored state of '" + pipeline + "': " + e.getMessage());
                } 
            }
        }
    }

    /**
     * Returns the predecessors of <code>part</code>.
     * 
     * @param part the pipeline node part to return the predecessors for
     * @return the predecessors or <b>null</b> if there are none
     */
    public static List<PipelineNodeSystemPart> getPredecessors(PipelineNodeSystemPart part) {
        List<PipelineNodeSystemPart> result = null;
        PipelineSystemPart pipeline = part.getPipeline();
        PipelineTopology topology = pipeline.getTopology();
        Processor topoProcessor = topology.getProcessor(part.getName());
        if (null != topoProcessor) {
            for (int i = 0; i < topoProcessor.getInputCount(); i++) {
                Processor predecessor = topoProcessor.getInput(i).getOrigin();
                if (null != predecessor) {
                    PipelineNodeSystemPart predecessorNode = pipeline.obtainPipelineNode(predecessor.getName());
                    if (null != predecessorNode) {
                        if (null == result) {
                            result = new ArrayList<PipelineNodeSystemPart>();
                        }
                        result.add(predecessorNode);
                    }
                }
            }
        }
        return result;
    }
    
    /**
     * Returns the logger for this class.
     * 
     * @return the logger
     */
    private static Logger getLogger() {
        return LogManager.getLogger(Tracing.class);
    }
    
}
