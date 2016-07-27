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

import eu.qualimaster.monitoring.topology.PipelineTopology.Processor;
import eu.qualimaster.monitoring.topology.PipelineTopology.Stream;

/**
 * Defines a topology projection.
 * 
 * @author Holger Eichelberger
 */
public interface ITopologyProjection {
    
    /**
     * Returns the specified start node.
     * 
     * @param index the 0-based index
     * @return the start node
     * @throws IndexOutOfBoundsException if <code>index &lt; 0 || index &gt;= {@link #getStartCount()}</code>
     */
    public Processor getStart(int index);
    
    /**
     * Returns the number of start nodes.
     * 
     * @return the number of start nodes
     */
    public int getStartCount();
    
    /**
     * Returns whether <code>processor</code> is an end node.
     * 
     * @param processor the processor to check
     * @return <code>true</code> if <code>processor</code> is a start node, <code>false</code> else
     */
    public boolean isEnd(Processor processor);

    /**
     * Returns the next node when traversing <code>stream</code>.
     * 
     * @param stream the stream to be tested
     * @return <b>null</b> if <code>stream</code> shall not be traversed at all, the target of <code>stream</code> if 
     *   the stream shall be traversed normally, another node if a subgraph shall be left out
     */
    public Processor getNext(Stream stream);

    /**
     * Returns whether this topology projection is just a simple topology consisting
     * of a single node.
     * 
     * @return <code>true</code> for single node topology, <code>false</code> else
     */
    public boolean isSimpleTopology();
    
}
