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
package eu.qualimaster.monitoring.systemState;

import eu.qualimaster.coordination.INameMapping.Component.Type;
import eu.qualimaster.monitoring.topology.ITopologyProjection;
import eu.qualimaster.monitoring.topology.ITopologyProvider;
import eu.qualimaster.monitoring.topology.ITopologyVisitor;
import eu.qualimaster.monitoring.topology.PipelineTopology;
import eu.qualimaster.monitoring.topology.PipelineTopology.Processor;
import eu.qualimaster.monitoring.topology.PipelineTopology.Stream;
import eu.qualimaster.monitoring.topology.TopologyWalker.DepthFirstOnceVisitingStrategy;
import eu.qualimaster.monitoring.topology.TopologyWalker;
import net.ssehub.easy.basics.pool.IPoolManager;
import net.ssehub.easy.basics.pool.Pool;

/**
 * A pipeline statistics walker. Instances can be reused, calculation is done via {@link #visit(ITopologyProvider)}. 
 * Using the {@link #POOL} is encouraged, but not mandatory.
 * 
 * @author Holger Eichelberger
 */
public class StatisticsWalker implements ITopologyVisitor {


    public static final Pool<StatisticsWalker> POOL = new Pool<StatisticsWalker>(new IPoolManager<StatisticsWalker>() {

        @Override
        public StatisticsWalker create() {
            return new StatisticsWalker();
        }

        @Override
        public void clear(StatisticsWalker instance) {
            instance.clearInstance();
        }
    });
    
    private TopologyWalker walker;
    private transient ITopologyProvider provider;
    private transient ObservationAggregator[] aggregators;

    /**
     * Creates a statistics-collecting topology walker, with {@link TopologyWalker#DEPTH_FIRST_NODE_ONCE} strategy. 
     * This constructor is intentionally accessible although there is a {@link #POOL}.
     */
    public StatisticsWalker() {
        this.walker = new TopologyWalker(new DepthFirstOnceVisitingStrategy(), this);
    }
    
    /**
     * Clears the collected statistics data in this walker.
     */
    public void clear() {
        this.aggregators = null;
        this.provider = null;
    }

    /**
     * Clears this instance for pooling. {@link #clear()} shall be called as
     * local cleanup during visiting.
     */
    protected void clearInstance() {
    }
    
    /**
     * Calculates the topology specified by <code>provider</code>. This method is not reentrant, i.e., this
     * instance is allocated during the visit by {@link #provider}.
     * 
     * @param provider the topology provider
     * @param aggregators the aggregators to calculate
     */
    public void visit(ITopologyProvider provider, ObservationAggregator... aggregators) {
        this.provider = provider;
        this.aggregators = aggregators;
        PipelineTopology topology = provider.getTopology();
        ITopologyProjection projection = provider.getTopologyProjection();
        if (null == projection) {
            walker.visit(topology);
        } else {
            walker.visit(projection);
        }
        clear();
    }

    @Override
    public boolean enter(Processor node, boolean isEnd, boolean isLoop) {
        boolean complete = false;
        if (!isLoop) {
            PipelineNodeSystemPart nodePart = aggregate(node, true);
            if (isEnd) {
                if (null != nodePart) {
                    // do not cause an end part for data mgt nodes at end - as implementation paths are intentionally
                    // without real sink and may represent shorter paths, which can lead to a wrong impression in 
                    // min/max calculation
                    complete = Type.DATA_MGT != nodePart.getComponentType();
                } else {
                    complete = true;
                }
            }
        } else {
            complete = true;
        }
        if (complete) {
            for (int a = 0; a < aggregators.length; a++) {
                aggregators[a].pathCompleted();
            }
        }
        return false;
    }

    @Override
    public boolean visit(Stream stream) {
        // not needed
        return false;
    }

    @Override
    public void exit(Processor node, boolean isEnd, boolean isLoop) {
        if (!isLoop) {
            aggregate(node, false);
        }
    }

    /**
     * Aggregates the values for <code>node</code>.
     * 
     * @param node the node to aggregate the values for
     * @param enter called by enter (<code>true</code>), else called by exit (<code>false</code>)
     * @return the node part on which the aggregation happened
     */
    private PipelineNodeSystemPart aggregate(Processor node, boolean enter) {
        PipelineNodeSystemPart nodePart;
        if (null != provider) {
            nodePart = provider.getNode(node.getName());
            if (null != nodePart) {
                for (int a = 0; a < aggregators.length; a++) {
                    ObservationAggregator agg = aggregators[a];
                    if (enter) {
                        agg.push(nodePart, nodePart == provider);
                    } else {
                        agg.pop();
                    }
                }
            }
        } else {
            nodePart = null;
        }
        return nodePart;
    }

    /**
     * Clears a set of aggregators. [utility]
     * 
     * @param aggregators the aggregators to clear
     */
    public static void clear(ObservationAggregator[] aggregators) {
        for (int a = 0; a < aggregators.length; a++) {
            aggregators[a].clear();
        }
    }

}
