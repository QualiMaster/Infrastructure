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
 * Implements a topology projection for a single node. [PRELIMINARY]
 * 
 * @author Holger Eichelberger
 */
public class SingleNodeTopologyProjection implements ITopologyProjection {

    private Processor node;
    
    /**
     * Creates a single node topology.
     * 
     * @param node the single node
     */
    public SingleNodeTopologyProjection(Processor node) {
        this.node = node;
    }
    
    @Override
    public Processor getStart(int index) {
        if (index != 0) {
            throw new IndexOutOfBoundsException();
        }
        return node;
    }

    @Override
    public int getStartCount() {
        return 1;
    }

    @Override
    public boolean isEnd(Processor processor) {
        return node.equals(processor);
    }

    @Override
    public Processor getNext(Stream stream) {
        return null; // no streams
    }

    @Override
    public boolean isSimpleTopology() {
        return true;
    }
    
    @Override
    public String toString() {
        return "Simple node topology " + node;
    }

}
