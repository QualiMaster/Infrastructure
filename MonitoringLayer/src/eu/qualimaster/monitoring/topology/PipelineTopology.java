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
package eu.qualimaster.monitoring.topology;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Represents the full topology structure of a pipeline (including TCP connections invisible for Storm or sub-topologies
 * invisible for the infrastructure configuration). The aim is to provide fast access to the connections among the
 * elements, potentially with additional information. Please note that this class is (as far as possible) independent 
 * from the underlying data processing framework, the names are logical pipeline names and that pipeline topologies 
 * shall not be modifiable.
 * 
 * @author Holger Eichelberger
 */
public class PipelineTopology {

    /**
     * The interface for a basic topology element.
     * 
     * @author Holger Eichelberger
     */
    public interface ITopologyElement {
        
        /**
         * The name of the element.
         * 
         * @return the name
         */
        public String getName();
        
        /**
         * Whether this element is active in case of alternative paths.
         * 
         * @return <code>true</code> for active, <code>false</code> else
         */
        public boolean isActive();
        
        /**
         * Changes the active flag.
         * 
         * @param active the new active flag
         */
        public void setActive(boolean active);
    }
    
    /**
     * Represents a basic topology element. Topology elements are active by default.
     * 
     * @author Holger Eichelberger
     */
    public abstract static class TopologyElement implements ITopologyElement {
        
        private String name;
        private boolean active = true;
        
        /**
         * Creates a topology element.
         * 
         * @param name the name of the element
         */
        protected TopologyElement(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public boolean isActive() {
            return active;
        }
        
        @Override
        public void setActive(boolean active) {
            this.active = active;
        }

    }
    
    /**
     * Represents a processing component, regardless whether it is a source, a sink or a processor.
     * This class provides methods for incrementally creating a topology. Therefore, a concrete topology
     * creator must subclass this class and provide access to the respective methods and the constructor 
     * (marked by [incremental]).
     * 
     * @author Holger Eichelberger
     */
    public static class Processor extends TopologyElement {
        private int parallelization;
        private List<Stream> inputs;
        private List<Stream> outputs;
        private TreeSet<Integer> tasks; // Storm always allocates continuous ranges, but others, if at all

        /**
         * Creates a processor without streams. [incremental]
         * 
         * @param name the name of the processor
         * @param parallelization the configured thread parallelization
         * @param tasks the identifiers for the logical parallelization (may be <b>null</b> if not present)
         */
        protected Processor(String name, int parallelization, int[] tasks) {
            super(name);
            this.parallelization = parallelization;
            addTasks(tasks);
        }
        
        /**
         * Creates a processor component.
         * 
         * @param name the name of the component
         * @param parallelization the configured thread parallelization
         * @param tasks the identifiers for the logical parallelization (may be <b>null</b> if not present)
         * @param inputs the input streams (may be <b>null</b> for none, <b>null</b> entries will be filtered out)
         * @param outputs the output streams (may be <b>null</b> for none, <b>null</b> entries will be filtered out)
         */
        public Processor(String name, int parallelization, int[] tasks, List<Stream> inputs, List<Stream> outputs) {
            this(name, parallelization, tasks);
            setInputs(inputs);
            setOutputs(outputs);
        }

        /**
         * Adds tasks (for incremental definition). Each task id is added only once.
         * 
         * @param tasks the new task ids (may be <b>null</b>)
         */
        protected void addTasks(int[] tasks) {
            if (null != tasks) {
                if (null == this.tasks) {
                    this.tasks = new TreeSet<Integer>();
                    for (int t : tasks) {
                        this.tasks.add(t);
                    }
                }
            }
        }

        /**
         * Defines the input streams. [incremental]
         * 
         * @param inputs the input streams (may be <b>null</b> for none, <b>null</b> entries will be filtered out)
         */
        protected void setInputs(List<Stream> inputs) {
            this.inputs = addAllSafe(this.inputs, inputs);
        }

        /**
         * Defines the input streams. [incremental]
         * 
         * @param outputs the output streams (may be <b>null</b> for none, <b>null</b> entries will be filtered out)
         */
        protected void setOutputs(List<Stream> outputs) {
            this.outputs = addAllSafe(this.outputs, outputs);
        }

        /**
         * Adds an input stream. [incremental]
         * 
         * @param input the input stream (may be <b>null</b>, ignored then)
         */
        protected void addInput(Stream input) {
            this.inputs = addSafe(this.inputs, input);
        }

        /**
         * Adds an output stream. [incremental]
         * 
         * @param output the output stream (may be <b>null</b>, ignored then)
         */
        protected void addOutput(Stream output) {
            this.outputs = addSafe(this.outputs, output);
        }

        /**
         * Adds <code>stream</code> to <code>result</code>.
         * 
         * @param result the list to be modified as a side effect (may be <b>null</b>, then a new list may be created
         *   if <code>from</code> contains non-null streams)
         * @param stream the stream to be added (may be <b>null</b>, ignored then)
         * @return the list of all non-null streams, may be <b>null</b> if there were no streams in <code>from</code> 
         *   and <code>result</code> was <b>null</b>
         */
        private List<Stream> addSafe(List<Stream> result, Stream stream) {
            if (null != stream) {
                if (null == result) {
                    result = new ArrayList<Stream>();
                }
                result.add(stream);
            }
            return result;
        }
        
        /**
         * Returns a list of all non-null streams in <code>from</code> to <code>result</code>.
         * 
         * @param result the list to be modified as a side effect (may be <b>null</b>, then a new list may be created
         *   if <code>from</code> contains non-null streams)
         * @param from the list to take the streams over (may be <b>null</b> for none)
         * @return the list of all non-null streams, may be <b>null</b> if there were no streams in <code>from</code> 
         *   and <code>result</code> was <b>null</b>
         */
        private List<Stream> addAllSafe(List<Stream> result, List<Stream> from) {
            if (null != from) {
                for (int f = 0; f < from.size(); f++) {
                    result = addSafe(result, from.get(f));
                }
            }
            return result;
        }

        /**
         * Returns the configured number of parallel executions.
         * 
         * @return the number of parallel executions
         */
        public int getParallelization() {
            return parallelization;
        }
        
        /**
         * Returns the specified input stream.
         * 
         * @param index the 0-based index of the input stream
         * @return the specified input stream
         * @throws IndexOutOfBoundsException if <code>index &lt;0 || index&gt;={@link #getInputCount()}</code>
         */
        public Stream getInput(int index) {
            if (null == inputs) {
                throw new IndexOutOfBoundsException();
            }
            return inputs.get(index);
        }
        
        /**
         * Returns whether this processor is supposed to be some kind of source.
         * 
         * @return <code>true</code> in case of a source, <code>false</code> else
         */
        public boolean isSource() {
            return getInputCount() == 0 && getOutputCount() > 0;
        }

        /**
         * Returns whether this processor is a double-sided connected processor
         * supposed to process data.
         * 
         * @return <code>true</code> in case of a double-sided connected processor, <code>false</code> else
         */
        public boolean isDataProcessor() {
            return getInputCount() > 0 && getOutputCount() > 0;
        }

        /**
         * Returns whether this processor is supposed to be some kind of sink.
         * 
         * @return <code>true</code> in case of a sink, <code>false</code> else
         */
        public boolean isSink() {
            return getInputCount() > 0 && getOutputCount() == 0;
        }

        /**
         * Returns the number of input streams.
         * 
         * @return the number of input streams
         */
        public int getInputCount() {
            return null == inputs ? 0 : inputs.size();
        }

        /**
         * Returns the specified output stream.
         * 
         * @param index the 0-based index of the output stream
         * @return the specified output stream
         * @throws IndexOutOfBoundsException if <code>index &lt;0 || index&gt;={@link #getOutputCount()}</code>
         */
        public Stream getOutput(int index) {
            if (null == outputs) {
                throw new IndexOutOfBoundsException();
            }
            return outputs.get(index);
        }

        /**
         * Returns the number of output streams.
         * 
         * @return the number of output streams
         */
        public int getOutputCount() {
            return null == outputs ? 0 : outputs.size();
        }

        /**
         * Returns the number of (input + output) streams.
         * 
         * @return the number of (input + output) streams
         */
        public int getStreamCount() {
            return getInputCount() + getOutputCount();
        }
        
        /**
         * Returns whether this processor produces output for <code>target</code>.
         * 
         * @param target the potential output node
         * @return <code>true</code> if there is a stream to <code>target</code>, <code>false</code> else
         */
        public boolean hasOutputTo(Processor target) {
            return search(outputs, target, false);
        }

        /**
         * Returns whether this processor receives input from <code>origin</code>.
         * 
         * @param origin the potential input node
         * @return <code>true</code> if there is a stream from <code>origin</code>, <code>false</code> else
         */
        public boolean hasInputFrom(Processor origin) {
            return search(inputs, origin, true);
        }
        
        /**
         * Returns the number of tasks handled.
         * 
         * @return the number of tasks
         */
        public int getTaskCount() {
            return null == tasks ? 0 : tasks.size();
        }
        
        /**
         * Returns the tasks.
         * 
         * @return the tasks (sorted in ascending sequence)
         */
        public Collection<Integer> tasks() {
            return tasks;
        }
        
        /**
         * Returns whether this processor handles the given task.
         * 
         * @param task the task to look for
         * @return <code>true</code> if <code>task</code> is handled, <code>false</code> else (also if unknown)
         */
        public boolean handlesTask(int task) {
            boolean found = false;
            if (null != tasks) {
                found = tasks.contains(task);
            }
            return found;
        }
        
        /**
         * Searches <code>streams</code> for <code>node</code> as <code>origin</code> (or target).
         * 
         * @param streams the streams to search
         * @param node the node to search for
         * @param origin whether origin or target shall be considered
         * @return <code>true</code> if found, <code>false</code> else
         */
        private boolean search(List<Stream> streams, Processor node, boolean origin) {
            boolean found = false;
            if (null != streams) {
                for (int s = 0, n = streams.size(); !found && s < n; s++) {
                    Stream stream = streams.get(s);
                    Processor comp = origin ? stream.origin : stream.target;
                    found = comp == node;
                }
            }
            return found;
        }
        
        @Override
        public String toString() {
            String tasksString = null == tasks ? "{}" : tasks.toString();
            return "Processor " + getName() + " #" + parallelization  + " " + tasksString + " in: " + inputs + " out: " 
                + outputs;
        }

    }

    /**
     * Represents a data stream within the processing, i.e., between two {@link Processor processors}.
     * 
     * @author Holger Eichelberger
     */
    public static class Stream extends TopologyElement {
        private String name;
        private Processor origin;
        private Processor target;

        /**
         * Creates a stream instance.
         * 
         * @param name the name of the stream
         * @param origin the origin
         * @param target the target
         */
        public Stream(String name, Processor origin, Processor target) {
            super(name);
            if (null == origin) {
                throw new IllegalArgumentException("origin is null");
            }
            if (null == target) {
                throw new IllegalArgumentException("target is null");
            }
            this.origin = origin;
            this.target = target;
        }

        /**
         * Returns the origin of the stream.
         * 
         * @return the origin
         */
        public Processor getOrigin() {
            return origin;
        }
        
        /**
         * Returns the target of the stream.
         * 
         * @return the target
         */
        public Processor getTarget() {
            return target;
        }

        @Override
        public String toString() {
            return "Stream (" + name + " " + origin.getName() + " -> " + target.getName() + ")";
        }

    }
    
    private Map<String, Processor> processors = new HashMap<String, Processor>();
    private List<Processor> sources = new ArrayList<Processor>();
    private List<Processor> sinks = new ArrayList<Processor>();

    /**
     * Creates a pipeline topology from a given set of (consistently linked) processors.
     * 
     * @param procs the processors
     */
    public PipelineTopology(Collection<? extends Processor> procs) {
        for (Processor component : procs) {
            processors.put(component.getName(), component);
            if (component.isSource()) {
                sources.add(component);
            } else if (component.isSink()) {
                sinks.add(component);
            }
        }
    }
    
    /**
     * Returns the number of processors.
     * 
     * @return the number of processors
     */
    public int getProcessorCount() {
        return processors.size();
    }
    
    /**
     * Returns a processor for a given processor name.
     * 
     * @param name the name to search for
     * @return the processor (<b>null</b> if unknown)
     */
    public Processor getProcessor(String name) {
        return processors.get(name);
    }
    
    /**
     * Returns the number of sources of the topology.
     * 
     * @return the number of sources
     */
    public int getSourceCount() {
        return sources.size();
    }

    /**
     * Returns the specified source.
     * 
     * @param index the 0-based index of the source
     * @return the source
     * @throws IndexOutOfBoundsException in case that <code>index &lt; 0 || index &gt;={@link #getSourceCount()}</code>
     */
    public Processor getSource(int index) {
        return sources.get(index);
    }

    /**
     * Returns the number of sinks of the topology.
     * 
     * @return the number of sinks
     */
    public int getSinkCount() {
        return sinks.size();
    }
    
    /**
     * Returns the specified sink.
     * 
     * @param index the 0-based index of the sink
     * @return the sink
     * @throws IndexOutOfBoundsException in case that <code>index &lt; 0 || index &gt;={@link #getSinkCount()}</code>
     */
    public Processor getSink(int index) {
        return sinks.get(index);
    }
    
    /**
     * Turns a list of processors into a string of names. [util]
     * 
     * @param processors the processors
     * @return the related string
     */
    public static String namesToString(Collection<? extends ITopologyElement> processors) {
        String result = "[";
        if (null != processors) {
            Iterator<? extends ITopologyElement> iter = processors.iterator();
            while (iter.hasNext()) {
                ITopologyElement elt = iter.next();
                result += elt.getName();
                if (iter.hasNext()) {
                    result += ", ";
                }
            }
        }
        result += "]";
        return result;
    }
    
    @Override
    public String toString() {
        return "PipelineTopology " + processors;
    }
    
}
