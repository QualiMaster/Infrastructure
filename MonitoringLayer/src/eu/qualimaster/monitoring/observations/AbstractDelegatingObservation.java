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

import java.util.Collection;
import java.util.Set;

/**
 * Implements a an abstract observation by delegating the operations to another observation.
 * Basically, this implementation is neutral and intended as a basis for specific delegating observations. 
 * This allows using this class for compound and (single) non-compound observations.
 * 
 * @author Holger Eichelberger
 */
public abstract class AbstractDelegatingObservation implements IObservation {

    private static final long serialVersionUID = 1L;
    private IObservation observation;

    /**
     * Creates a delegating statistics observation.
     * 
     * @param observation the basic observation to take the values from
     * @throws IllegalArgumentException if <code>observation</code> is null
     */
    public AbstractDelegatingObservation(IObservation observation) {
        if (null == observation) {
            throw new IllegalArgumentException("observation must not be null");
        }
        this.observation = observation;
    }
    
    /**
     * Creates a new instance from a given <code>source</code>.
     * 
     * @param source the source observation to copy from
     * @param provider the parent observation provider
     */
    protected AbstractDelegatingObservation(AbstractDelegatingObservation source, IObservationProvider provider) {
        observation = source.observation.copy(provider);
    }

    @Override
    public boolean isComposite() {
        return observation.isComposite();
    }

    @Override
    public void setValue(double value, Object key) {
        observation.setValue(value, key);
    }

    @Override
    public void setValue(Double value, Object key) {
        observation.setValue(value, key);
    }

    @Override
    public void incrementValue(double value, Object key) {
        observation.incrementValue(value, key);
    }

    @Override
    public void incrementValue(Double value, Object key) {
        observation.incrementValue(value, key);
    }

    @Override
    public double getValue() {
        return observation.getValue();
    }

    @Override
    public double getLocalValue() {
        return observation.getLocalValue();
    }

    @Override
    public AtomicDouble getValue(Object key) {
        return observation.getValue(key);
    }

    @Override
    public boolean isValueSet() {
        return observation.isValueSet();
    }

    @Override
    public void clear() {
        observation.clear();
    }

    @Override
    public String toString() {
        return toStringShortcut() + "[" + getValue() + ";" + observation.toString() + "]";
    }
    
    /**
     * Returns the delegate.
     * 
     * @return the delegate
     */
    protected IObservation getDelegate() {
        return observation;
    }
    
    @Override
    public long getFirstUpdate() {
        return observation.getFirstUpdate();
    }
    
    @Override
    public long getLastUpdate() {
        return observation.getLastUpdate();
    }
    
    @Override
    public void setLastUpdate(long timestamp) {
        observation.setLastUpdate(timestamp);
    }

    @Override
    public int getComponentCount() {
        return observation.getComponentCount();
    }

    @Override
    public void clearComponents(Collection<Object> keys) {
        observation.clearComponents(keys);
    }

    @Override
    public Set<Object> getComponentKeys() {
        return observation.getComponentKeys();
    }
    
    @Override
    public void replaceComponentKeys(Object oldKey, Object newKey) {
        observation.replaceComponentKeys(oldKey, newKey);
    }

    @Override
    public void link(IObservation observation) {
        this.observation.link(observation);
    }

    @Override
    public void unlink(IObservation observation) {
        this.observation.unlink(observation);
    }
    
    @Override
    public int getLinkCount() {
        return observation.getLinkCount();
    }

    @Override
    public IObservation getLink(int index) {
        return observation.getLink(index);
    }

    @Override
    public boolean statisticsWhileReading() {
        return this.observation.statisticsWhileReading();
    }
    
    /**
     * Returns the shortcut for this observable to be printed out in {@link #toString()}.
     * 
     * @return the shortcut
     */
    protected String toStringShortcut() {
        return "Del";
    }

    @Override
    public void switchedTo(boolean direct) {
        observation.switchedTo(false);
    }

}
