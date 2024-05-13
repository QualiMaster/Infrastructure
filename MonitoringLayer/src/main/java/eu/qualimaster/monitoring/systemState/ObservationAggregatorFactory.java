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

import java.util.HashMap;
import java.util.Map;

import eu.qualimaster.observables.IObservable;
import net.ssehub.easy.basics.pool.IPoolManager;
import net.ssehub.easy.basics.pool.Pool;

/**
 * Implements a factory for observation aggregators.
 * 
 * @author Holger Eichelberger
 */
public class ObservationAggregatorFactory {
    
    /**
     * A pool manager creating observation aggregators for a certain combination
     * of observable and aggregator function.
     * 
     * @author Holger Eichelberger
     */
    public static class ObservationAggregatorPoolManager implements IPoolManager<ObservationAggregator> {

        private IObservable observable;
        private IAggregationFunction elementAggregator;
        private boolean pathAverage;
        private IAggregationFunction topologyAggregator;
        
        /**
         * Creates a new aggregation pool manager.
         * 
         * @param observable the observable
         * @param elementAggregator the aggregator function for path elements
         * @param pathAverage <code>true</code> if the average over the results of <code>elementAggregator</code> shall 
         *   be calculated, <code>false</code> if the direct value shall be taken
         * @param topologyAggregator the aggregator function over paths on topology level
         */
        public ObservationAggregatorPoolManager(IObservable observable, IAggregationFunction elementAggregator, 
            boolean pathAverage, IAggregationFunction topologyAggregator) {
            this.observable = observable;
            this.elementAggregator = elementAggregator;
            this.pathAverage = pathAverage;
            this.topologyAggregator = topologyAggregator;
        }
        
        @Override
        public ObservationAggregator create() {
            return new ObservationAggregator(observable, elementAggregator, pathAverage, topologyAggregator);
        }

        @Override
        public void clear(ObservationAggregator instance) {
            instance.clear();
        }
        
        /**
         * Returns the element aggregator function.
         * 
         * @return the element aggregator function
         */
        public IAggregationFunction getElementAggregator() {
            return elementAggregator;
        }
        
        /**
         * Returns whether the average over the path result provided by {@link #getElementAggregator()} shall be 
         * taken as path value or the direct path value.
         * 
         * @return <code>true</code> for the average, <code>false</code> for the direct value
         */
        public boolean doPathAverage() {
            return pathAverage;
        }

        /**
         * Returns the topology aggregator function.
         * 
         * @return the topology aggregator function
         */
        public IAggregationFunction getTopologyAggregator() {
            return topologyAggregator;
        }

        /**
         * Returns the observable.
         * 
         * @return the observable
         */
        public IObservable getObservable() {
            return observable;
        }
        
    }

    private static final Map<IObservable, Pool<ObservationAggregator>> POOLS
        = new HashMap<IObservable, Pool<ObservationAggregator>>();
    
    /**
     * Registers a pool via its pool manager. Modifies {@link #POOLS}.
     * 
     * @param manager the pool manager (<b>null</b> and instances with unspecified aggregators/observable are ignored)
     */
    public static synchronized void register(ObservationAggregatorPoolManager manager) {
        if (null != manager) {
            IObservable observable = manager.getObservable();
            IAggregationFunction elementAggregator = manager.getElementAggregator();
            IAggregationFunction topologyAggregator = manager.getTopologyAggregator();
            if (null != elementAggregator && null != topologyAggregator && null != observable) {
                POOLS.put(observable, new Pool<ObservationAggregator>(manager));
            }
        }
    }
    
    /**
     * Returns an aggregator instance.
     * 
     * @param observable the observable to return the aggregator for
     * @return the aggregator
     * @throws IllegalArgumentException if no pool was registered for <code>observable</code>
     * @see #register(ObservationAggregatorPoolManager)
     */
    public static synchronized ObservationAggregator getAggregator(IObservable observable) {
        Pool<ObservationAggregator> pool = POOLS.get(observable);
        if (null == pool) {
            throw new IllegalArgumentException("no pool registered for " + observable);
        }
        ObservationAggregator result = pool.getInstance();
        return result;
    }
    
    /**
     * Releases an aggregator instance previously obtained via {@link #getAggregator(IObservable)}.
     * Unregistered observables will be ignored.
     * 
     * @param aggregator the aggregator to release
     */
    public static synchronized void releaseAggregator(ObservationAggregator aggregator) {
        IObservable observable = aggregator.getObservable();
        if (null != observable) {
            Pool<ObservationAggregator> pool = POOLS.get(observable);
            if (null != pool) {
                pool.releaseInstance(aggregator);
            } else {
                aggregator.clear(); // at least the same cleanup behavior
            }
        }
    }
    
}
