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

/**
 * Implements a topology visiting strategy. Strategies have to take responsibility for cycle control.
 * 
 * @author Holger Eichelberger
 */
public interface ITopologyVisitingStrategy {

    /**
     * Visits <code>node</code>.
     * 
     * @param node the (processor) node
     * @param projection the topology projection
     * @param visitor the visitor to perform actions
     */
    public void visit(Processor node, ITopologyProjection projection, ITopologyVisitor visitor);

    /**
     * Clears the internal state for reuse.
     */
    public void clear();
    
}
