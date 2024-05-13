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
 * Used to visit individual topology nodes via their connecting stream. The call sequence is determined by 
 * {@link ITopologyVisitingStrategy}.
 * 
 * @author Holger Eichelberger
 */
public interface ITopologyVisitor {
    
    /**
     * Starts visiting a processor.
     * 
     * @param node the (processor) node
     * @param isEnd whether <code>node</code> is an end of the visit (either a sink or as requested by
     *   the visit strategy)
     * @param isLoop whether <code>node</code> is part of a loop
     * @return <code>true</code> for stop, <code>false</code> else
     */
    public boolean enter(Processor node, boolean isEnd, boolean isLoop);

    /**
     * Visits a stream (only if the stream is not bypassed by a topology projection).
     * 
     * @param stream the stream to be visited
     * @return <code>true</code> for stop, <code>false</code> else
     */
    public boolean visit(Stream stream);
    
    /**
     * Ends visiting a processor.
     * 
     * @param node the (processor) node
     * @param isEnd whether <code>node</code> is an end of the visit (either a sink or as requested by
     *   the visit strategy)
     * @param isLoop whether <code>node</code> is part of a loop
     */
    public void exit(Processor node, boolean isEnd, boolean isLoop);

}
