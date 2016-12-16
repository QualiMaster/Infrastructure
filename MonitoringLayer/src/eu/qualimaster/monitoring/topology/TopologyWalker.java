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
 * Methods for generically visiting a {@link PipelineTopology} acoording to a {@link ITopologyVisitingStrategy} and to 
 * collect results using a {@link ITopologyVisitor}.
 * 
 * @author Holger Eichelberger
 */
public class TopologyWalker {

    /**
     * A depth-first visiting strategy. Nodes that can be reached over multiple paths will be visited multiple times.
     * 
     * @author Holger Eichelberger
     */
    public static class DepthFirstVisitingStrategy extends AbstractMarkingTopologyVisitingStrategy {

        @Override
        public void visit(Processor node, ITopologyProjection projection, ITopologyVisitor visitor) {
            boolean isEnd = projection.isEnd(node) || 0 == node.getOutputCount();
            if (!isEnd) {
                boolean foundEdge = false;
                for (int o = 0; !foundEdge && o < node.getOutputCount(); o++) {
                    Processor next = projection.getNext(node.getOutput(o));
                    foundEdge = (null != next && !isMarked(next) && next.isActive());
                }
                isEnd = !foundEdge;
            }
            boolean isLoop = isMarked(node);
            visitor.enter(node, isEnd, isLoop);
            if (!isMarked(node) && !isEnd && node.isActive()) {
                mark(node);
                for (int o = 0; o < node.getOutputCount(); o++) {
                    Stream out = node.getOutput(o);
                    Processor next = projection.getNext(out);
                    if (null != next && !isMarked(next)) {
                        if (next.equals(out.getTarget())) {
                            visitor.visit(out);
                        }
                        visit(next, projection, visitor);
                    }
                }
                unmark(node);
            }
            visitor.exit(node, isEnd, isLoop);
        }
        
    }

    /**
     * A depth-first visiting strategy. Nodes that can be reached over multiple paths will be visited only once.
     * 
     * @author Holger Eichelberger
     */
    public static class DepthFirstOnceVisitingStrategy extends DepthFirstVisitingStrategy {

        @Override
        public void visit(Processor node, ITopologyProjection projection, ITopologyVisitor visitor) {
            if (!wasVisited(node)) {
                markAsVisited(node);
                super.visit(node, projection, visitor);
            }
        }

    }
    
    private ITopologyVisitingStrategy strategy;
    private ITopologyVisitor visitor;
    
    /**
     * Creates a topology walker.
     * 
     * @param strategy the visiting strategy to apply
     * @param visitor the visitor to call for aggregating the results
     */
    public TopologyWalker(ITopologyVisitingStrategy strategy, ITopologyVisitor visitor) {
        this.strategy = strategy;
        this.visitor = visitor;
    }
    
    /**
     * Visits the complete <code>topology</code>.
     * 
     * @param topology the topology to visit
     */
    public void visit(PipelineTopology topology) {
        int srcCount = topology.getSourceCount();
        if (srcCount > 0) {
            visit(new TopologyProjection(topology));
        }
    }


    /**
     * Visits some nodes determined by <code>projection</code>.
     * 
     * @param projection the topology projection to visit
     */
    public void visit(ITopologyProjection projection) {
        strategy.clear();
        for (int s = 0; s < projection.getStartCount(); s++) {
            Processor proc = projection.getStart(s);
            strategy.visit(proc, projection, visitor);
        }
        strategy.clear();
    }
    
}
