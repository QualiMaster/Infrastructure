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

import eu.qualimaster.monitoring.topology.ITopologyProjection;
import eu.qualimaster.monitoring.topology.ITopologyProvider;
import eu.qualimaster.observables.IObservable;

/**
 * A base class for different forms of topology aggregation. Sub classes shall call {@link #setFirstUpdate(long)} 
 * appropriately.
 * 
 * @author Holger Eichelberger
 */
public abstract class AbstractTopologyAggregatorObservation extends AbstractDelegatingObservation {

    private static final long serialVersionUID = 5689818695869352019L;
    private ITopologyProvider provider;
    private IObservable observable;
    private long firstUpdate = -1;
    
    /**
     * Creates a delegating topology aggregator for the given observable.
     * 
     * @param observation the basic observation to take the values from
     * @param observable the observable to process
     * @param provider the topology provider
     */
    public AbstractTopologyAggregatorObservation(IObservation observation, IObservable observable, 
        ITopologyProvider provider) {
        super(observation);
        this.provider = provider;
        this.observable = observable;
    }
    
    @Override
    public double getValue() {
        double result = 0;
        if (null != provider && null != provider.getTopology() && !isSimpleTopoplogy()) {
            // simple: end of "recursion"
            result = calculateValue();
        } else {
            result = getLocalValue();
        }
        return result;
    }
    
    /**
     * Calculates the aggregated value.
     * 
     * @return the aggregated value
     */
    protected abstract double calculateValue();
    
    /**
     * Returns whether the requested aggregation topology is simple.
     * 
     * @return <code>true</code> if simple, <code>false</code> else
     */
    protected boolean isSimpleTopoplogy() {
        ITopologyProjection prj = provider.getTopologyProjection();
        return null != prj ? prj.isSimpleTopology() || prj.getStartCount() == 0 : false;
    }

    /**
     * Returns the topology provider.
     * 
     * @return the topology provider
     */
    protected ITopologyProvider getProvider() {
        return provider;
    }

    /**
     * Returns the actual observable this observation is holding.
     * 
     * @return the observable
     */
    protected IObservable getObservable() {
        return observable;
    }
    
    @Override
    public boolean isValueSet() {
        return true; // recalculate
    }

    @Override
    protected String toStringShortcut() {
        return "Topo";
    }
    
    @Override
    public void clear() {
        super.clear();
        firstUpdate = -1;
    }
    
    @Override
    public synchronized long getFirstUpdate() {
        long tmp = super.getFirstUpdate();
        if (firstUpdate < 0) {
            firstUpdate = tmp;
        } else if (tmp > 0) {
            firstUpdate = Math.min(firstUpdate, tmp);
        }
        return firstUpdate;
    }
    
    /**
     * Updates the first update value.
     * 
     * @param firstUpdate the new time stamp of the first update, may be negative if unknown (shall then not modify
     *  the first update of this instance)
     */
    protected synchronized void setFirstUpdate(long firstUpdate) {
        setFirstUpdate0(super.getFirstUpdate()); // take underlying component into account
        setFirstUpdate0(firstUpdate);
    }

    /**
     * Updates the first update value.
     * 
     * @param firstUpdate the new time stamp of the first update, may be negative if unknown (shall then not modify
     *  the first update of this instance)
     */
    private void setFirstUpdate0(long firstUpdate) {
        if (this.firstUpdate < 0) {
            this.firstUpdate = firstUpdate;
        } else if (firstUpdate > 0) {
            this.firstUpdate = Math.min(this.firstUpdate, firstUpdate);
        }
    }
    
    @Override
    public synchronized void switchedTo(boolean direct) {
        firstUpdate = -1;
        super.switchedTo(direct);
    }

}
