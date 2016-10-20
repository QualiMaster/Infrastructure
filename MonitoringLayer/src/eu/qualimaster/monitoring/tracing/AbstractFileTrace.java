package eu.qualimaster.monitoring.tracing;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.xtext.util.Arrays;

import eu.qualimaster.coordination.events.AlgorithmProfilingEvent.DetailMode;
import eu.qualimaster.monitoring.events.ComponentKey;
import eu.qualimaster.monitoring.observations.ObservedValue;
import eu.qualimaster.monitoring.parts.IPartType;
import eu.qualimaster.monitoring.systemState.AlgorithmParameter;
import eu.qualimaster.monitoring.systemState.NodeImplementationSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineSystemPart;
import eu.qualimaster.monitoring.systemState.SystemPart;
import eu.qualimaster.monitoring.topology.PipelineTopology;
import eu.qualimaster.monitoring.topology.PipelineTopology.Processor;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.Scalability;

public abstract class AbstractFileTrace implements ITrace {

	/**
     * A comparator for pipeline node system parts.
     */
    protected static final Comparator<PipelineNodeSystemPart> PIPELINE_NODE_SYSTEM_PART_COMPARATOR 
        = new Comparator<PipelineNodeSystemPart>() {

            @Override
            public int compare(PipelineNodeSystemPart o1, PipelineNodeSystemPart o2) {
                return o1.getName().compareTo(o2.getName());
            }
    };
    
    protected static final IObservable[] NODE_MEASURES = new IObservable[] {Scalability.ITEMS};
    protected PrintStream out;
    protected String name;
    protected boolean initialized;
    protected List<PipelineTraceInfo> pipelines; 
    protected Map<String, Serializable> settings;
    protected DetailMode mode = DetailMode.FALSE;
    
    /**
     * Information about an already traced pipeline.
     * 
     * @author Holger Eichelberger
     */
    protected class PipelineTraceInfo {
        protected String name;
        protected List<String> nodes = new ArrayList<String>();

        /**
         * Creates an instance.
         * 
         * @param name the name of the pipeline
         */
        protected PipelineTraceInfo(String name) {
            this.name = name;
        }
    }
    
    /**
     * Creates a new trace.
     * 
     * @param name the name of the trace (for {link {@link #toString()}}, shall be the file name
     * @param out the output stream to trace to
     */
    public AbstractFileTrace(String name, PrintStream out) {
        this.name = name;
        this.out = out;
    }
    
    @Override
    public void traceAlgorithm(PipelineNodeSystemPart node, NodeImplementationSystemPart alg, 
        IParameterProvider parameters) {
        List<PipelineNodeSystemPart> predecessors = Tracing.getPredecessors(node);
        Map<String, List<AlgorithmParameter>> tmp = parameters.getAlgorithmParameters();
        List<AlgorithmParameter> param = null == tmp ? null : tmp.get(alg.getName());
        if (!isInitialized()) {
            print("timestamp");
            printSeparator();
            // predecessor settings
            tracePredecessorHeaders();
            // node settings
            traceHeader(node, "param.", NODE_MEASURES, null);
            // parameter
            if (null != param) {
                for (AlgorithmParameter p : param) {
                    print("param." + p.getName());
                }
            }
            // extra separator
            printSeparator();
            // observation headers
            traceHeader(node, "measure.", null, NODE_MEASURES);
            traceHeader(alg, "measure.", null, null);
            if (mode.traceAlgorithms()) {
                tracePartsHeader(alg, null, null);
            }
            println();
            setInitialized();
            if (null != settings) {
                printSubTrace(settings);
                settings = null;
            }
        }
        print(System.currentTimeMillis());
        printSeparator();
        //predecessor settings
        tracePredecessors(predecessors);
        // first the settings
        trace(node, NODE_MEASURES, null, null);
        // parameter
        if (null != param) {
            for (AlgorithmParameter p : param) {
                print(p.getValue());
            }
        }
        // extra separator
        printSeparator(); 
        // then the dependent parameters
        trace(node, null, NODE_MEASURES, null);
        trace(alg, null, null, null);
        if (mode.traceAlgorithms()) {
            traceParts(alg, null, null);
        }
        println();
    }
    
    /**
     * Determines the output node sequence for the parts of <code>alg</code>.
     * 
     * @param alg the algorithm to determine the output sequence for
     * @return the the sorted nodes
     */
    protected Set<PipelineNodeSystemPart> nodeSequence(NodeImplementationSystemPart alg) {
        TreeSet<PipelineNodeSystemPart> result = new TreeSet<PipelineNodeSystemPart>(
            PIPELINE_NODE_SYSTEM_PART_COMPARATOR);
        result.addAll(alg.getNodes());
        return result;
    }
    
    /**
     * Traces the header for the observables of the parts of <code>alg</code>.
     * 
     * @param alg the algorithm to trace the parts for
     * @param exclude observables to exclude - trace only other others (may be <b>null</b>)
     * @param include observables to include - trace only those (may be <b>null</b>)
     */
    protected void tracePartsHeader(NodeImplementationSystemPart alg, IObservable[] exclude, 
        IObservable[] include) {
        printSeparator();
        for (PipelineNodeSystemPart part : nodeSequence(alg)) {
            String prefix = "part." + part.getName() + ".";
            traceHeader(part, prefix, exclude, include);
            printSeparator();
            if (mode.traceTasks()) {
                Processor proc = getProcessor(part);
                if (null != proc && null != proc.tasks()) {
                    for (Integer taskId : proc.tasks()) {
                        String taskIdPrefix = prefix + taskId + ".";
                        traceHeader(part, taskIdPrefix, exclude, include);
                        print(taskIdPrefix + "host");
                        printSeparator();
                    }
                }
            }
        }
    }
    
    /**
     * Returns the topology processor for <code>node</code>.
     * 
     * @param node the node
     * @return the processor (may be <b>null</b>)
     */
    protected Processor getProcessor(PipelineNodeSystemPart node) {
        Processor result = null;
        PipelineSystemPart pip = node.getPipeline();
        if (null != pip) {
            PipelineTopology topo = pip.getTopology();
            if (null != topo) {
                result = topo.getProcessor(node.getName());
            }
        }
        return result;
    }
    
    /**
     * Traces the observables of the parts of <code>alg</code>.
     * 
     * @param alg the algorithm to trace the parts for
     * @param exclude observables to exclude - trace only other others (may be <b>null</b>)
     * @param include observables to include - trace only those (may be <b>null</b>)
     */
    protected void traceParts(NodeImplementationSystemPart alg, IObservable[] exclude, 
        IObservable[] include) {
        printSeparator();
        for (PipelineNodeSystemPart part : nodeSequence(alg)) {
            trace(part, exclude, include, null);
            printSeparator();
            if (mode.traceTasks()) {
                Processor proc = getProcessor(part);
                if (null != proc && null != proc.tasks()) {
                    for (Integer taskId : proc.tasks()) {
                        trace(part, exclude, include, taskId);            
                    }
                }
            }
        }
    }
    
    /**
     * Prints a long value to the trace.
     * 
     * @param value the value to print
     */
    protected void print(long value) {
        if (null != out) {
            out.print(value);
        }
    }

    /**
     * Prints an observed value to the trace.
     * 
     * @param value the value to print
     */
    protected void print(ObservedValue value) {
        if (null != out) {
            if (null != value) {
                print(value.get());
            } else {
                print("");
            }
        }
    }

    /**
     * Prints a double value to the trace.
     * 
     * @param value the value to print
     */
    protected void print(double value) {
        if (null != out) {
            String tmp = String.format("%.15f", value);
            tmp = tmp.replace(".", ","); // for excel
            out.print(tmp);
        }
    }

    /**
     * Prints a text to the trace.
     * 
     * @param text the text to print
     */
    protected void print(String text) {
        if (null != out) {
            out.print(text);
        }
    }

    /**
     * Prints a CSV separator.
     */
    protected void printSeparator() {
        if (null != out) {
            out.print("\t");
        }
    }
    
    /**
     * Prints a new line.
     */
    protected void println() {
        if (null != out) {
            out.println();
            out.flush();
        }
    }
    
    /**
     * Closes the trace.
     */
    public void close() {
        out.close();
        out = null;
    }
    
    /**
     * Returns whether the trace is already initialized.
     * 
     * @return <code>true</code> if the trace is initialized, <code>false</code> else
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Sets the trace to initialized.
     */
    void setInitialized() {
        initialized = true;
    }
    
    /**
     * Prints a format line.
     * 
     * @param cls the class
     * @param type the correspoinding part type
     * @param text headline text
     */
    protected void printFormat(Class<?> cls, IPartType type, String text) {
        IObservable[] sequence = Tracing.getObservableSequence(cls, SystemPart.getObservables(type));
        print(text);
        for (int s = 0; s < sequence.length; s++) {
            if (s > 0) {
                printSeparator();
            }
            print(sequence[s].name());
        }
        println();
    }
    
    @Override
    public void notifyNewSubTrace(Map<String, Serializable> settings) {
        if (isInitialized()) {
            printSubTrace(settings);
        } else {
            this.settings = settings;
        }
    }

    /**
     * Indicates a new sub-trace.
     * 
     * @param settings the actual settings (may be <b>null</b> if undefined, but anyway a new sub-trace starts)
     */
    protected void printSubTrace(Map<String, Serializable> settings) {
        String text = "New trace:";
        if (null != settings) {
            TreeMap<String, Serializable> tmp = new TreeMap<String, Serializable>();
            tmp.putAll(settings);
            text += " " + tmp;
        }
        print(text);
        println();
    }

    @Override
    public void setTraceMode(DetailMode mode) {
        if (null != mode) {
            this.mode = mode;
        }
    }
    
    @Override
    public String toString() {
        return "FileTrace " + name;
    }
    
    /**
     * Returns the trace info.
     * 
     * @param pName the pipeline name
     * @return the trace info
     */
    protected PipelineTraceInfo getTraceInfo(String pName) {
        PipelineTraceInfo result = null;
        for (int p = 0; p < pipelines.size(); p++) {
            PipelineTraceInfo info = pipelines.get(p);
            if (info.name.equals(pName)) {
                result = info;
            }
        }
        if (null == result) {
            result = new PipelineTraceInfo(pName);
            pipelines.add(result);
        }
        return result;
    }
    
    /**
     * Traces the headers of the values to be emitted for predecessors.
     */
    protected void tracePredecessorHeaders() {
        // currently fixed implementation
        print("pre." + Scalability.ITEMS);
        printSeparator();
    }
    
    /**
     * Traces the values for the predecessors.
     * 
     * @param predecessors the predecessors
     */
    protected void tracePredecessors(List<PipelineNodeSystemPart> predecessors) {
        if (null != predecessors) {
            // currently fixed implementation
            int count = predecessors.size();
            if (count > 0) {
                double items = 0;
                for (int p = 0; p < count; p++) {
                    items = items + predecessors.get(p).getObservedValue(Scalability.ITEMS);
                }
                print(items);
                printSeparator();
            } else {
                print("");
                printSeparator();
            }
        } else {
            print("");
            printSeparator();
        }
    }
    
    /**
     * Trace output for the header line for <code>part</code>.
     * 
     * @param part the part to create the header part for
     * @param prefix additional text to be printed before the name of the columns (may be empty)
     * @param exclude observables to exclude - trace only other others (may be <b>null</b>)
     * @param include observables to include - trace only those (may be <b>null</b>)
     */
    protected void traceHeader(SystemPart part, String prefix, IObservable[] exclude, IObservable[] include) {
        IObservable[] sequence = Tracing.getObservableSequence(part);
        for (int o = 0; o < sequence.length; o++) {
            IObservable observable = sequence[o];
            if ((null == include || Arrays.contains(include, observable)) 
                && (null == exclude || !Arrays.contains(exclude, observable))) {
                print(prefix + observable.name());
                printSeparator();
            }
        }
    }
    
    /**
     * Traces the values observed for <code>part</code>.
     * 
     * @param part the part to trace
     * @param exclude observables to exclude - trace only other others (may be <b>null</b>)
     * @param include observables to include - trace only those (may be <b>null</b>)
     * @param taskId if given, trace only observations for <code>taskId</code>, print <code>part</code>-level 
     *     observables if <b>null</b> 
     */
    protected void trace(SystemPart part, IObservable[] exclude, IObservable[] include, Integer taskId) {
        ComponentKey host = null;
        IObservable[] sequence = Tracing.getObservableSequence(part);
        for (int o = 0; o < sequence.length; o++) {
            IObservable observable = sequence[o];
            if ((null == include || Arrays.contains(include, observable)) 
                && (null == exclude || !Arrays.contains(exclude, observable))) {
                boolean printed = false;
                if (null != taskId) {
                    ComponentKey key = null;
                    Set<Object> keys = part.getComponentKeys(observable);
                    for (Object k : keys) {
                        if (k instanceof ComponentKey && ((ComponentKey) k).getTaskId() == taskId) {
                            key = (ComponentKey) k;
                            break;
                        }
                    }
                    if (null != key) {
                        print(part.getObservedValue(observable, key));
                        printed = true;
                        if (null == host) {
                            host = key;
                        }
                    } 
                } 
                if (!printed) {
                    if (part.hasValue(observable)) {
                        print(part.getObservedValue(observable));
                    } else {
                        print("");
                    }
                }
                printSeparator();
            }
        }
        if (null != host) {
            print(host.getHostName());
            printSeparator();
        }
    }
}
