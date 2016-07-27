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

import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;

/**
 * A topology provider for observations to perform correct aggregations. This allows
 * delayed initialization of the topology as soon as it becomes available.
 * 
 * @author Holger Eichelberger
 */
public interface ITopologyProvider {
    
    /**
     * Returns the pipeline topology to perform correct aggregations.
     * 
     * @return the topology description (may be <b>null</b> if there is (currently) none)
     */
    public PipelineTopology getTopology();

    /**
     * Returns a projection of {@link #getTopology()} in case that sub-topologies shall be considered only.
     * 
     * @return the projection or <b>null</b> for the entire topology
     */
    public ITopologyProjection getTopologyProjection();

    /**
     * Returns the specified node within the context of this provider.
     * 
     * @param name the name of the node (logical name as used in the topology)
     * @return the node or <b>null</b> if not found
     */
    public PipelineNodeSystemPart getNode(String name);
    
    /**
     * Returns the name of the provider (for debugging).
     * 
     * @return the name of the provider
     */
    public String getName();
    
}
