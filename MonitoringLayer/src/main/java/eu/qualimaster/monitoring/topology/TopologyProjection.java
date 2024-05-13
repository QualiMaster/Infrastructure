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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.qualimaster.monitoring.topology.PipelineTopology.Processor;
import eu.qualimaster.monitoring.topology.PipelineTopology.Stream;

/**
 * Implements a topology projection. This class shall not be used for topologies
 * having just a single node.
 * 
 * @author Holger Eichelberger
 */
public class TopologyProjection implements ITopologyProjection {
    
    private List<Processor> start;
    private Set<Processor> end;
    private Map<Stream, Processor> next;

    /**
     * Creates a topology projection for the full topology.
     * 
     * @param topology the topology to visit
     */
    public TopologyProjection(PipelineTopology topology) {
        this(fillStart(null, topology), fillEnd(null, topology), null);
    }
    
    /**
     * Creates a topology projection.
     * 
     * @param start the the start nodes
     * @param end the end nodes
     * @param next the next enabled node for the given streams (may be <b>null</b> for no limitation)
     */
    public TopologyProjection(List<Processor> start, List<Processor> end, Map<Stream, Processor> next) {
        this.start = start;
        this.next = next;
        if (null != end) {
            this.end = new HashSet<Processor>();
            this.end.addAll(end);
        }
    }
    
    @Override
    public Processor getStart(int index) {
        return start.get(index);
    }
    
    @Override
    public int getStartCount() {
        return start.size();
    }
    
    @Override
    public boolean isEnd(Processor processor) {
        return end.contains(processor);
    }

    @Override
    public Processor getNext(Stream stream) {
        Processor result;
        if (null == next || !next.containsKey(stream)) {
            result = stream.getTarget();
        } else {
            result = next.get(stream);
        }
        return result;
    }

    /**
     * Fills the given start nodes.
     * 
     * @param start the start nodes (may be <b>null</b>)
     * @param topology the topology used for validation
     * @return <code>end</code> if nodes are given, the soruce nodes from the topology else
     */
    public static List<Processor> fillStart(List<Processor> start, PipelineTopology topology) {
        if (null == start || start.isEmpty()) {
            start = new ArrayList<Processor>();
            for (int s = 0; s < topology.getSourceCount(); s++) {
                start.add(topology.getSource(s));
            }
        }
        return start;
    }

    /**
     * Fills the given end nodes.
     * 
     * @param end the end nodes (may be <b>null</b>)
     * @param topology the topology used for validation
     * @return <code>end</code> if nodes are given, the sink nodes from the topology else
     */
    public static List<Processor> fillEnd(List<Processor> end, PipelineTopology topology) {
        if (null == end || end.isEmpty()) {
            end = new ArrayList<Processor>();
            for (int s = 0; s < topology.getSinkCount(); s++) {
                end.add(topology.getSink(s));
            }
        }
        return end;
    }

    @Override
    public boolean isSimpleTopology() {
        return false;
    }

    @Override
    public String toString() {
        return "Topology projection start " + PipelineTopology.namesToString(start) + " end " 
            + PipelineTopology.namesToString(end) + " next " + next;
    }

}
