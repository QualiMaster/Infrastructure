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

import eu.qualimaster.monitoring.systemState.ObservationAggregator;
import eu.qualimaster.monitoring.systemState.ObservationAggregatorFactory;
import eu.qualimaster.monitoring.systemState.StatisticsWalker;
import eu.qualimaster.monitoring.topology.ITopologyProvider;
import eu.qualimaster.observables.IObservable;

/**
 * Implements a delegating topology aggregator observation, i.e., an observation
 * behaving like it's delegate but calculating the actual value using a {@link StatisticsWalker}.
 * 
 * @author Holger Eichelberger
 */
public class DelegatingTopologyAggregatorObservation extends AbstractTopologyAggregatorObservation {

    private static final long serialVersionUID = -5172325134705252683L;
    
    /**
     * Creates a delegating topology aggregator for the give observable using {@link StatisticsWalker}.
     * 
     * @param observation the basic observation to take the values from
     * @param observable the observable to process
     * @param provider the topology provider
     */
    public DelegatingTopologyAggregatorObservation(IObservation observation, IObservable observable, 
        ITopologyProvider provider) {
        super(observation, observable, provider);
    }
    
    @Override
    protected double calculateValue() {
        StatisticsWalker walker = StatisticsWalker.POOL.getInstance();
        ObservationAggregator aggregator = ObservationAggregatorFactory.getAggregator(getObservable());
        walker.visit(getProvider(), aggregator);
        double result = aggregator.getValue();
        setFirstUpdate(aggregator.getFirstUpdate());
        ObservationAggregatorFactory.releaseAggregator(aggregator);
        StatisticsWalker.POOL.releaseInstance(walker);
        return result;
    }
    
    @Override
    public IObservation copy(IObservationProvider provider) {
        // assumption: getDelegate is owned by this instance
        return new DelegatingTopologyAggregatorObservation(getDelegate().copy(provider), getObservable(), 
            provider.getTopologyProvider());
    }
    
    @Override
    protected String toStringShortcut() {
        return "Topo";
    }

}
