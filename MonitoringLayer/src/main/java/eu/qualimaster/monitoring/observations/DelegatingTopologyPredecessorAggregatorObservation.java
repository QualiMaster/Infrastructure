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

import java.util.HashSet;
import java.util.Set;

import eu.qualimaster.monitoring.systemState.IAggregationFunction;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.topology.ITopologyProvider;
import eu.qualimaster.monitoring.topology.PipelineTopology;
import eu.qualimaster.monitoring.topology.PipelineTopology.Processor;
import eu.qualimaster.observables.IObservable;

/**
 * Aggregates over direct predecessors only.
 * 
 * @author Holger Eichelberger
 */
public class DelegatingTopologyPredecessorAggregatorObservation extends AbstractTopologyAggregatorObservation {

    private static final long serialVersionUID = -2927321795602475342L;
    private IAggregationFunction aggregator;
    
    /**
     * Creates a delegating topology sink aggregator observation.
     * 
     * @param observation the basic observation to take the values from
     * @param observable the observable to process
     * @param provider the topology provider
     * @param aggregator the aggregation function
     */
    public DelegatingTopologyPredecessorAggregatorObservation(IObservation observation, IObservable observable,
        ITopologyProvider provider, IAggregationFunction aggregator) {
        super(observation, observable, provider);
        this.aggregator = aggregator;
    }

    @Override
    public IObservation copy(IObservationProvider provider) {
        // assumption: getDelegate is owned by this instance
        return new DelegatingTopologyPredecessorAggregatorObservation(getDelegate().copy(provider), getObservable(), 
            provider.getTopologyProvider(), aggregator);
    }
    
    @Override
    protected double calculateValue() {
        double result = aggregator.getInitialValue();
        ITopologyProvider provider = getProvider();
        PipelineTopology topology = provider.getTopology();
        
        Processor processor = topology.getProcessor(provider.getName());
        if (null != processor && processor.getInputCount() > 0) {
            Set<Processor> done = new HashSet<Processor>();
            done.add(processor); // do not aggregate/visit this
            for (int i = 0; i < processor.getInputCount(); i++) {
                Processor orig = processor.getInput(i).getOrigin();
                if (!done.contains(orig)) {
                    done.add(orig);
                    result = aggregate(result, orig, aggregator);
                }
            }
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
    protected boolean isSimpleTopoplogy() {
        return false; // force calculation
    }

    @Override
    protected String toStringShortcut() {
        return "Pred";
    }

}
