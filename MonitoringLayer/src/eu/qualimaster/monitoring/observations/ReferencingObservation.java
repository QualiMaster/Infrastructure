/*
 * Copyright 2009-2016 University of Hildesheim, Software Systems Engineering
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

import java.util.Collection;
import java.util.Set;

import eu.qualimaster.observables.IObservable;

/**
 * Implements an observation which just references to another observation.
 * 
 * @author Holger Eichelberger
 */
public class ReferencingObservation implements IObservation {

    private static final long serialVersionUID = -6735215148987705350L;
    private IObservationProvider provider;
    private IObservable observable;
    private boolean enableValueChange;
    
    /**
     * Creates a referencing observation.
     * 
     * @param provider the observation provider
     * @param observable the observable within <code>provider</code> to reference to
     * @param enableValueChange enable or disable value changes (<code>true</code> may lead to topology 
     *     aggregation loops)
     */
    public ReferencingObservation(IObservationProvider provider, IObservable observable, boolean enableValueChange) {
        this.provider = provider;
        this.observable = observable;
        this.enableValueChange = enableValueChange;
    }
    
    @Override
    public boolean isComposite() {
        return provider.isComposite(observable);
    }

    @Override
    public void setValue(double value, Object key) {
        if (enableValueChange) {
            provider.setValue(observable, value, key);
        }
    }

    @Override
    public void setValue(Double value, Object key) {
        if (enableValueChange) {
            provider.setValue(observable, value, key);
        }
    }

    @Override
    public void incrementValue(double value, Object key) {
        if (enableValueChange) {
            provider.incrementValue(observable, value, key);
        }
    }

    @Override
    public void incrementValue(Double value, Object key) {
        if (enableValueChange) {
            provider.setValue(observable, value, key);
        }
    }

    @Override
    public double getValue() {
        return provider.getObservedValue(observable);
    }

    @Override
    public double getLocalValue() {
        return provider.getObservedValue(observable, true);
    }

    @Override
    public AtomicDouble getValue(Object key) {
        return provider.getValue(observable, key);
    }

    @Override
    public boolean isValueSet() {
        return provider.hasValue(observable);
    }

    @Override
    public void clear() {
        provider.clear(observable);
    }

    @Override
    public IObservation copy(IObservationProvider provider) {
        return new ReferencingObservation(provider, observable, enableValueChange);
    }

    @Override
    public long getLastUpdate() {
        return provider.getLastUpdate(observable);
    }
    
    @Override
    public long getFirstUpdate() {
        return provider.getFirstUpdate(observable);
    }

    @Override
    public void setLastUpdate(long timestamp) {
        provider.setLastUpdate(observable, timestamp);
    }

    @Override
    public int getComponentCount() {
        return provider.getComponentCount(observable);
    }

    @Override
    public void clearComponents(Collection<Object> keys) {
        provider.clearComponents(observable, keys);
    }

    @Override
    public Set<Object> getComponentKeys() {
        return provider.getComponentKeys(observable);
    }
    
    @Override
    public void replaceComponentKeys(Object oldKey, Object newKey) {
        // causes loop
    }

    @Override
    public void link(IObservation observation) {
        provider.link(observable, observation);
    }

    @Override
    public void unlink(IObservation observation) {
        provider.unlink(observable, observation);
    }
    
    @Override
    public int getLinkCount() {
        return provider.getLinkCount(observable);
    }

    @Override
    public IObservation getLink(int index) {
        return provider.getLink(observable, index);
    }

    @Override
    public boolean statisticsWhileReading() {
        return provider.statisticsWhileReading(observable);
    }

    @Override
    public void switchedTo(boolean direct) {
        provider.switchedTo(observable, false);
    }

    @Override
    public String toString() {
        return "ref " + observable + " " + getValue();
    }
    
}
