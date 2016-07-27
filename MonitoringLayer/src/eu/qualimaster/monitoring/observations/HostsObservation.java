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
import java.util.HashSet;
import java.util.Set;

import eu.qualimaster.monitoring.events.ComponentKey;
import eu.qualimaster.monitoring.observations.ObservationFactory.IObservationCreator;
import eu.qualimaster.monitoring.parts.IPartType;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.ResourceUsage;

/**
 * Implements a derived hosts observation, i.e., the distinct hosts making up the key set of the executors
 * of the given observation.
 * 
 * @author Holger Eichelberger
 */
class HostsObservation implements IObservation {

    static final IObservationCreator CREATOR = new IObservationCreator() {
        
        @Override
        public IObservation create(IObservable observable, IPartType type, 
            IObservationProvider observationProvider) {
            return new HostsObservation(observationProvider);
        }
    };
    
    private static final long serialVersionUID = -7614606775013746300L;
    private IObservationProvider provider;
    
    /**
     * Creates a hosts observation.
     * 
     * @param provider the observation provider to rely on
     */
    private HostsObservation(IObservationProvider provider) {
        this.provider = provider;
    }
    
    @Override
    public boolean isComposite() {
        return false;
    }

    @Override
    public void setValue(double value, Object key) {
        // derived, ignore
    }

    @Override
    public void setValue(Double value, Object key) {
        // derived, ignore
    }

    @Override
    public void incrementValue(double value, Object key) {
        // derived, ignore
    }

    @Override
    public void incrementValue(Double value, Object key) {
        // derived, ignore
    }

    /**
     * Returns the value.
     * 
     * @return the value
     */
    private double getValue0() {
        double result;
        Set<Object> keys = provider.getComponentKeys(ResourceUsage.EXECUTORS);
        if (null != keys) {
            Set<String> tmp = new HashSet<String>();
            for (Object key : keys) {
                if (key instanceof ComponentKey) {
                    ComponentKey cKey = (ComponentKey) key;
                    String host = cKey.getHostName();
                    if (null != host) {
                        tmp.add(host);
                    }
                }
            }
            result = tmp.size();
        } else {
            result = 0;
        }
        return result;
    }

    @Override
    public double getValue() {
        return getValue0();
    }
    
    @Override
    public double getLocalValue() {
        return getValue0();
    }

    @Override
    public AtomicDouble getValue(Object key) {
        return null;
    }

    @Override
    public boolean isValueSet() {
        return provider.hasValue(ResourceUsage.EXECUTORS);
    }

    @Override
    public void clear() {
    }

    @Override
    public IObservation copy(IObservationProvider provider) {
        return new HostsObservation(provider);
    }

    @Override
    public long getLastUpdate() {
        return provider.getLastUpdate(ResourceUsage.EXECUTORS);
    }

    @Override
    public int getComponentCount() {
        return 0;
    }

    @Override
    public void clearComponents(Collection<Object> keys) {
    }

    @Override
    public Set<Object> getComponentKeys() {
        return null;
    }

    @Override
    public void link(IObservation observation) {
        // nothing to do
    }

    @Override
    public void unlink(IObservation observation) {
        // nothing to do
    }
    
    @Override
    public String toString() {
        return String.valueOf(getValue());
    }

    @Override
    public boolean statisticsWhileReading() {
        return false;
    }

}
