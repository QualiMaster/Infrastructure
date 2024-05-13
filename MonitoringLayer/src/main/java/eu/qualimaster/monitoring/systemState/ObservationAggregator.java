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

import java.util.ArrayList;
import java.util.List;

import eu.qualimaster.observables.IObservable;

/**
 * Implements a path observation aggregator.
 * 
 * @author Holger Eichelberger
 */
public class ObservationAggregator {
    
    private double value;
    private transient List<Double> path = new ArrayList<Double>(10);
    //private transient double pathValue;
    private IObservable observable;
    private IAggregationFunction elementAggregator;
    private boolean pathAverage;
    private IAggregationFunction topologyAggregator;
    private boolean wasCompleted = false; // at least one defined path
    private long firstUpdate = -1;
    
    /**
     * An observation aggregator.
     * 
     * @param observable the observable to aggregate
     * @param elementAggregator the aggregation function for the individual elements of a topology path
     * @param pathAverage whether the average value of the result of applying <code>elementAggregator</code> 
     *   to a path shall be calculated as path value
     * @param topologyAggregator the aggregator over multiple paths of a topology
     */
    public ObservationAggregator(IObservable observable, IAggregationFunction elementAggregator, boolean pathAverage, 
        IAggregationFunction topologyAggregator) {
        this.observable = observable;
        this.elementAggregator = elementAggregator;
        this.topologyAggregator = topologyAggregator;
        this.pathAverage = pathAverage;
        clear();
    }
    
    /**
     * Returns the observable of this aggregator.
     * 
     * @return the observable
     */
    public IObservable getObservable() {
        return observable;
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
     * Returns the topology aggregator function.
     * 
     * @return the topology aggregator function
     */
    public IAggregationFunction getTopologyAggregator() {
        return topologyAggregator;
    }
    
    /**
     * Returns whether the average value over the results of {@link #getElementAggregator()} shall be considered
     * as the value of a path.
     * 
     * @return <code>true</code> for average, <code>false</code> else
     */
    public boolean doPathAverage() {
        return pathAverage;
    }
    
    /**
     * Clears this aggregator.
     */
    public void clear() {
        value = topologyAggregator.getInitialValue();
        path.clear();
        wasCompleted = false;
    }

    /**
     * Adds the observed value for {@link #observable} for the given <code>part</code> to the element values forming a 
     * path to be aggregated by {@link #getElementAggregator()}.
     * 
     * @param part the part to take the observed value from
     * @param localValue the local value or the aggregated value
     */
    public void push(PipelineNodeSystemPart part, boolean localValue) {
        Double value;
        if (part.hasValue(observable)) {
            value = part.getObservedValue(observable, localValue);
        } else {
            value = null;
        }
        path.add(value);
        if (firstUpdate < 0) {
            firstUpdate = part.getFirstUpdate(observable);
        } else {
            long tmp = part.getFirstUpdate(observable);
            if (tmp > 0) {
                firstUpdate = Math.min(firstUpdate, tmp);
            }
        }
    }
    
    /**
     * Returns the first update over all aggregated nodes.
     * 
     * @return the first update
     */
    public long getFirstUpdate() {
        return firstUpdate;
    }
    
    /**
     * Removes the last value added by {@link #push(PipelineNodeSystemPart)}.
     */
    public void pop() {
        int pos = path.size() - 1;
        if (pos > 0) {
            path.remove(pos);
        }
    }
    
    /**
     * Notifies that a path was completed and updates {@link #value} based on {@link #pathValue} 
     * and {@link #aggregator}.
     */
    void pathCompleted() {
        boolean undefined = false;
        double tmp = elementAggregator.getInitialValue();
        int pathLength = path.size();
        for (int e = 0; !undefined && e < pathLength; e++) {
            Double value = path.get(e);
            if (null == value) {
                undefined = true;
            } else {
                tmp = elementAggregator.calculate(tmp, value);
            }
        }
        if (!undefined) {
            if (pathAverage && pathLength > 0) {
                tmp /= pathLength;
            }
            if (value < 0) {
                value = tmp;
            } else { 
                value = topologyAggregator.calculate(value, tmp);
            }
            wasCompleted = true;
        }
    }
    
    /**
     * Returns the aggregated value.
     * 
     * @return the aggregated value (<code>0</code> for undefined)
     */
    public double getValue() {
        return wasCompleted ? value : 0;
    }
    
    /**
     * Returns whether this aggregator has a value.
     * 
     * @return <code>true</code> if a value over a path with defined values was calculated, <code>false</code> else
     */
    public boolean hasValue() {
        return wasCompleted;
    }
    
    @Override
    public String toString() {
        return elementAggregator.getName() + " (" + pathAverage + ") " + topologyAggregator.getName() + ":" + value;
    }
    
}