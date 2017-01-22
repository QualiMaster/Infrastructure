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
package eu.qualimaster.monitoring.observations;

import eu.qualimaster.monitoring.systemState.IAggregationFunction;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.topology.ITopologyProjection;
import eu.qualimaster.monitoring.topology.ITopologyProvider;
import eu.qualimaster.monitoring.topology.ITopologyVisitor;
import eu.qualimaster.monitoring.topology.PipelineTopology;
import eu.qualimaster.monitoring.topology.TopologyWalker;
import eu.qualimaster.monitoring.topology.PipelineTopology.Processor;
import eu.qualimaster.monitoring.topology.PipelineTopology.Stream;
import eu.qualimaster.monitoring.topology.TopologyWalker.DepthFirstOnceVisitingStrategy;
import eu.qualimaster.observables.IObservable;

/**
 * Aggregates over sinks only.
 * 
 * @author Holger Eichelberger
 */
public class DelegatingTopologySinkAggregatorObservation extends AbstractTopologyAggregatorObservation {

    private static final long serialVersionUID = -6527125537843235525L;

    private IAggregationFunction aggregator;
    
    /**
     * Creates a delegating topology sink aggregator observation.
     * 
     * @param observation the basic observation to take the values from
     * @param observable the observable to process
     * @param provider the topology provider
     * @param aggregator the aggregation function
     */
    public DelegatingTopologySinkAggregatorObservation(IObservation observation, IObservable observable,
        ITopologyProvider provider, IAggregationFunction aggregator) {
        super(observation, observable, provider);
        this.aggregator = aggregator;
    }

    @Override
    public IObservation copy(IObservationProvider provider) {
        // assumption: getDelegate is owned by this instance
        return new DelegatingTopologySinkAggregatorObservation(getDelegate().copy(provider), getObservable(), 
            provider.getTopologyProvider(), aggregator);
    }
    
    /**
     * Implements a sink topology visitor.
     * 
     * @author Holger Eichelberger
     */
    private class SinkTopologyVisitor implements ITopologyVisitor {

        private double result;
        private IAggregationFunction aggregator;

        /**
         * Creates the visitor.
         * 
         * @param result the actual result value (if nothing is aggregated, {@link #getResult()} will return this value)
         * @param aggregator the aggregator function
         */
        private SinkTopologyVisitor(double result, IAggregationFunction aggregator) {
            this.result = result;
            this.aggregator = aggregator;
        }
        
        /**
         * The result value.
         * 
         * @return the result value
         */
        private double getResult() {
            return result;
        }
        
        @Override
        public boolean visit(Stream stream) {
            return false;
        }
        
        @Override
        public void exit(Processor node, boolean isEnd, boolean isLoop) {
        }
        
        @Override
        public boolean enter(Processor node, boolean isEnd, boolean isLoop) {
            if (isEnd) {
                result = aggregate(result, node, aggregator);
            }
            return false;
        }
        
    }
    
    @Override
    protected double calculateValue() {
        double result = aggregator.getInitialValue();
        ITopologyProvider provider = getProvider();
        PipelineTopology topology = provider.getTopology();
        ITopologyProjection projection = provider.getTopologyProjection();
        if (null == projection) {
            for (int s = 0; s < topology.getSinkCount(); s++) {
                result = aggregate(result, topology.getSink(s), aggregator);
            }
        } else {
            SinkTopologyVisitor visitor = new SinkTopologyVisitor(result, aggregator);
            TopologyWalker walker = new TopologyWalker(new DepthFirstOnceVisitingStrategy(), visitor);
            walker.visit(projection);
            result = visitor.getResult();
        }
        return result;
    }

    /**
     * Aggregates the value of <code>processor</code> using <code>aggregator</code>.
     *
     * @param value the actual value
     * @param processor the processor to aggregate
     * @param aggregator the aggregator function
     * @return the aggregated value
     */
    private double aggregate(double value, Processor processor, IAggregationFunction aggregator) {
        double result = value;
        PipelineNodeSystemPart nodePart = getProvider().getNode(processor.getName());
        if (null != nodePart) {
            result = aggregator.calculate(result, nodePart.getObservedValue(getObservable(), 
                nodePart == getProvider())); // last - self-cycle control
            setFirstUpdate(nodePart.getFirstUpdate(getObservable()));
        }
        return result;
    }

    @Override
    protected String toStringShortcut() {
        return "Sink";
    }

}
