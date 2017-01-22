/*
 * Copyright 2009-2016 University of Hildesheim, Software Systems Engineering
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

import eu.qualimaster.monitoring.topology.ITopologyVisitor;
import eu.qualimaster.monitoring.topology.PipelineTopology.Processor;
import eu.qualimaster.monitoring.topology.PipelineTopology.Stream;

/**
 * A walker for finding a node in a topology.
 * 
 * @author Holger Eichelberger
 */
public class NodeFindingWalker implements ITopologyVisitor {

    private String nodeName;
    private boolean found;

    /**
     * Creates a node finding walker.
     * 
     * @param nodeName the node name to find
     * @see #setNodeName(String)
     */
    public NodeFindingWalker(String nodeName) {
        setNodeName(nodeName);
    }
    
    /**
     * Changes the node name to search for. [instance reuse]
     * 
     * @param nodeName the node name
     */
    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }
    
    /**
     * Clears this walker for reuse. [instance reuse]
     * 
     * @see #setNodeName(String)
     */
    public void clear() {
        found = false;
    }
    
    /**
     * Returns whether the given name was found.
     * 
     * @return <code>true</code> for found, <code>false</code> else
     */
    public boolean wasFound() {
        return found;
    }
    
    @Override
    public boolean enter(Processor node, boolean isEnd, boolean isLoop) {
        found = node.getName().equals(nodeName);
        return found;
    }

    @Override
    public boolean visit(Stream stream) {
        return false;
    }

    @Override
    public void exit(Processor node, boolean isEnd, boolean isLoop) {
    }

}
