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

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.qualimaster.coordination.INameMapping.Component.Type;
import eu.qualimaster.monitoring.events.FrozenSystemState;
import eu.qualimaster.monitoring.observations.AbstractCompoundObservation;
import eu.qualimaster.monitoring.observations.AtomicDouble;
import eu.qualimaster.monitoring.observations.IObservation;
import eu.qualimaster.monitoring.observations.IObservationProvider;
import eu.qualimaster.monitoring.observations.ObservationFactory;
import eu.qualimaster.monitoring.observations.ObservedValue;
import eu.qualimaster.monitoring.parts.IPartType;
import eu.qualimaster.monitoring.topology.ITopologyProjection;
import eu.qualimaster.monitoring.topology.ITopologyProvider;
import eu.qualimaster.monitoring.topology.PipelineTopology;
import eu.qualimaster.observables.IObservable;

/**
 * Stores monitoring information about a system part.
 * Links are not copied, must be re-established by caller.
 * 
 * @author Holger Eichelberger
 */
public class SystemPart implements IObservationProvider, ITopologyProvider, Serializable {

    public static final Object NULL_KEY = AbstractCompoundObservation.NULL_KEY;
    private static final long serialVersionUID = -4586931879306733242L;
    private IPartType type;
    private Type componentType;
    private String name;
    private Map<IObservable, IObservation> parameterValues = new HashMap<IObservable, IObservation>();
    private Map<Object, Map<IObservable, Double>> valueStore = null;

    /**
     * Creates a system part with observables and no component type (<b>null</b>). In case that this class is a 
     * topology provider, it will automatically be used for initialized the observations with a reference to this class.
     * 
     * @param type the type of the part
     * @param name the (descriptive) name of the system part
     */
    SystemPart(IPartType type, String name) {
        this(type, null, name);
    }
    
    /**
     * Creates a system part with observables. In case that this class is a topology provider, it will automatically
     * be used for initialized the observations with a reference to this class.
     * 
     * @param type the type of the part
     * @param componentType the component type (may be <b>null</b>)
     * @param name the (descriptive) name of the system part
     */
    SystemPart(IPartType type, Type componentType, String name) {
        this.type = type;
        this.name = name;
        this.componentType = componentType;
        initObservables();
    }

    /**
     * Creates a copy of this system part.
     * 
     * @param source the source system part
     * @param state the parent system state for crosslinks
     */
    protected SystemPart(SystemPart source, SystemState state) {
        this.type = source.type;
        this.name = source.name;
        this.componentType = source.componentType;
        synchronized (parameterValues) {
            for (Map.Entry<IObservable, IObservation> value : source.parameterValues.entrySet()) {
                IObservable observable = value.getKey();
                IObservation observation = value.getValue();
                this.parameterValues.put(observable, observation.copy(this));
            }
            // don't copy the links
        }
    }

    /**
     * Initializes the observables. Allows overriding/super call if needed.
     */
    protected void initObservables() {
        synchronized (parameterValues) {
            addObservables(ObservationFactory.getObservations(type));
            if (null != SystemState.getConfigurer()) {
                addObservables(SystemState.getConfigurer().additionalObservables(type));
            }
        }
    }

    /**
     * Returns all observables for a type.
     * 
     * @param type the type
     * @return all observables
     */
    public static Collection<IObservable> getObservables(IPartType type) {
        Set<IObservable> result = new HashSet<IObservable>();
        addAll(result, ObservationFactory.getObservations(type));
        if (null != SystemState.getConfigurer()) {
            addAll(result, SystemState.getConfigurer().additionalObservables(type));    
        }
        return result;
    }
    
    /**
     * Whether thrift shall be used for monitoring. This is intended as a hint to the Monitoring layer.
     * 
     * @return <code>true</code> for thrift, <code>false</code> else
     */
    public boolean useThrift() {
        return false; // may be true for nodes only
    }
    
    /**
     * Returns all observables supported by this system part.
     * 
     * @return all observables
     */
    public Collection<IObservable> getObservables() {
        return parameterValues.keySet();
    }

    /**
     * Adds all elements from <code>data</code> to <code>set</code>.
     * 
     * @param <T> the data type
     * @param set the set to be modified as a side effect
     * @param data the data to be added to <code>set</code>
     */
    private static <T> void addAll(Set<T> set, List<T> data) {
        if (null != data) {
            for (int d = 0; d < data.size(); d++) {
                set.add(data.get(d));
            }
        }
    }

    /**
     * Add observables (enables extension). Only unknown observables are added.
     * 
     * @param observables the observables to be added
     */
    private void addObservables(List<IObservable> observables) {
        if (null != observables) {
            synchronized (parameterValues) {
                for (int i = 0; i < observables.size(); i++) {
                    IObservable observable = observables.get(i);
                    if (!parameterValues.containsKey(observable)) {
                        parameterValues.put(observable, 
                            ObservationFactory.createObservation(observable, type, this));
                    }
                }
            }
        }
    }

    /**
     * Stores a value from in the value store.
     * 
     * @param observable the observable to associate the value with (call is ignored if <b>null</b>)
     * @param value the value (call is ignored if <b>null</b>)
     * @param key the compound key (may be <b>null</b> for none/default)
     */
    public void setStoreValue(IObservable observable, Double value, Object key) {
        if (null != observable && null != value) {
            if (null == valueStore) {
                valueStore = new HashMap<Object, Map<IObservable, Double>>();
            }
            Object normKey = null == key ? NULL_KEY : key; 
            Map<IObservable, Double> storeForKey = valueStore.get(normKey);
            if (null == storeForKey) {
                storeForKey = new HashMap<IObservable, Double>();
                valueStore.put(normKey, storeForKey);
            }
            storeForKey.put(observable, value);
        }
    }
    
    /**
     * Returns a value from the value store.
     * 
     * @param observable the observable to look for
     * @param key the compound key (may be <b>null</b> for none/default)
     * @return the stored value (may be <b>null</b>, in particular if no value has been stored before)
     */
    public Double getStoreValue(IObservable observable, Object key) {
        Double result;
        if (null == valueStore || null == observable) {
            result = null;
        } else {
            Map<IObservable, Double> storeForKey = valueStore.get(null == key ? NULL_KEY : key);
            if (null == storeForKey) {
                result = null; 
            } else {
                result = storeForKey.get(observable);
            }
        }
        return result;
    }

    /**
     * Copies the observables supported by the part.
     * 
     * @param assignedOnly whether only assigned values or all values shall be returned
     * @return the observables
     */
    public Map<String, Double> copyObservables(boolean assignedOnly) {
        Map<String, Double> copy = new HashMap<String, Double>();
        synchronized (parameterValues) {
            for (IObservable observable : parameterValues.keySet()) {
                if (!observable.isInternal()) {
                    if (!assignedOnly || (assignedOnly && hasValue(observable))) { // reduce data load
                        copy.put(observable.name(), getObservedValue(observable));
                    }
                }
            }
        }
        return copy;
    }

    @Override
    public void setValue(IObservable observable, double value, Object key) {
        synchronized (parameterValues) {
            IObservation observation = parameterValues.get(observable);
            if (null != observation) {
                observation.setValue(value, key);
            }
        }
    }
    
    @Override
    public void setValue(IObservable observable, Double value, Object key) {
        synchronized (parameterValues) {
            IObservation observation = parameterValues.get(observable);
            if (null != observation && null != value) {
                observation.setValue(value, key);
            }
        }
    }

    @Override
    public void incrementValue(IObservable observable, double value, Object key) {
        synchronized (parameterValues) {
            IObservation observation = parameterValues.get(observable);
            if (null != observation) {
                observation.setValue(value, key);
            }
        }
    }
    
    @Override
    public void incrementValue(IObservable observable, Double value, Object key) {
        synchronized (parameterValues) {
            IObservation observation = parameterValues.get(observable);
            if (null != observation && null != value) {
                observation.setValue(value, key);
            }
        }
    }

    @Override
    public int getComponentCount(IObservable observable) {
        int result = 0;
        synchronized (parameterValues) {
            IObservation observation = parameterValues.get(observable);
            if (null != observation) {
                result = observation.getComponentCount();
            }
        }
        return result;
    }

    /**
     * Clears the specified components.
     * 
     * @param observable denotes the observable to clear the component count for
     * @param keys the keys for the components to be cleared
     */
    public void clearComponents(IObservable observable, Collection<Object> keys) {
        synchronized (parameterValues) {
            IObservation observation = parameterValues.get(observable);
            if (null != observation) {
                observation.clearComponents(keys);
            }
        }
    }
    
    /**
     * Clears the values of the specified <code>observable</code>. This method is intended for 
     * debugging and shall not be used in usual monitoring code!
     * 
     * @param observable the observable to clear
     */
    @Override
    public void clear(IObservable observable) {
        synchronized (parameterValues) {
            IObservation observation = parameterValues.get(observable);
            if (null != observation) {
                observation.clear();
            }
        }
    }
    
    @Override
    public Set<Object> getComponentKeys(IObservable observable) {
        Set<Object> result = null;
        synchronized (parameterValues) {
            IObservation observation = parameterValues.get(observable);
            if (null != observation) {
                result = observation.getComponentKeys();
            }
        }
        if (null == result) {
            result = Collections.unmodifiableSet(new HashSet<Object>());
        }
        return result;
    }
    
    @Override
    public void replaceComponentKeys(Object oldKey, Object newKey, IObservable... observables) {
        synchronized (parameterValues) {
            if (null == observables || 0 == observables.length) {
                for (IObservable obs : observables) {
                    IObservation o = parameterValues.get(obs);
                    if (null != o) {
                        o.replaceComponentKeys(oldKey, newKey);
                    }
                }
            } else {
                for (IObservation obs : parameterValues.values()) {
                    obs.replaceComponentKeys(oldKey, newKey);
                }
            }
        }
    }

    @Override
    public ObservedValue getObservedValue(IObservable observable, Object key) {
        ObservedValue result = null;
        synchronized (parameterValues) {
            IObservation observation = parameterValues.get(observable);
            if (null != observation) {
                result = observation.getValue(key);
            }
        }
        return result;
    }

    @Override
    public double getObservedValue(IObservable observable) {
        return getObservedValue(observable, false);
    }
    
    @Override
    public double getObservedValue(IObservable observable, boolean localValue) {
        double result = 0;
        synchronized (parameterValues) {
            IObservation observation = parameterValues.get(observable);
            if (null != observation) {
                if (localValue) {
                    result = observation.getLocalValue();
                } else {
                    result = observation.getValue();
                }
            }
        }
        return result;
    }
    
    @Override
    public long getLastUpdate(IObservable observable) {
        long result = -1;
        synchronized (parameterValues) {
            IObservation observation = parameterValues.get(observable);
            if (null != observation) {
                result = observation.getLastUpdate();
            }
        }
        return result;
    }
    

    @Override
    public long getFirstUpdate(IObservable observable) {
        long result = -1;
        synchronized (parameterValues) {
            IObservation observation = parameterValues.get(observable);
            if (null != observation) {
                result = observation.getFirstUpdate();
            }
        }
        return result;
    }
    
    @Override
    public void switchedTo(IObservable observable, boolean direct) {
        synchronized (parameterValues) {
            IObservation observation = parameterValues.get(observable);
            if (null != observation) {
                observation.switchedTo(direct);
            }
        }
    }
    
    /**
     * Signals an algorithm switch to all observations.
     */
    protected void switchedTo() {
        synchronized (parameterValues) {
            for (IObservation obs : parameterValues.values()) {
                obs.switchedTo(true);
            }
        }
    }
    
    @Override
    public void setLastUpdate(IObservable observable, long timestamp) {
        synchronized (parameterValues) {
            IObservation observation = parameterValues.get(observable);
            if (null != observation) {
                observation.setLastUpdate(timestamp);
            }
        }
    }

    @Override
    public int getObservedValueInt(IObservable observable) {
        int result = 0;
        synchronized (parameterValues) {
            IObservation observation = parameterValues.get(observable);
            if (null != observation) {
                result = (int) observation.getValue();
            }
        }
        return result;
    }
    
    @Override
    public boolean isComposite(IObservable observable) {
        boolean result = false;
        synchronized (parameterValues) {
            IObservation observation = parameterValues.get(observable);
            if (null != observation) {
                result = observation.isComposite();
            }
        }
        return result;        
    }

    @Override
    public boolean hasValue(IObservable observable) {
        boolean result = false;
        synchronized (parameterValues) {
            IObservation observation = parameterValues.get(observable);
            if (null != observation) {
                result = observation.isValueSet();
            }
        }
        return result;
    }
    
    @Override
    public int getLinkCount(IObservable observable) {
        int result = 0;
        synchronized (parameterValues) {
            IObservation observation = parameterValues.get(observable);
            if (null != observation) {
                result = observation.getLinkCount();
            }
        }
        return result;
    }

    @Override
    public IObservation getLink(IObservable observable, int index) {
        IObservation result = null;
        synchronized (parameterValues) {
            IObservation observation = parameterValues.get(observable);
            if (null != observation) {
                result = observation.getLink(index);
            } else {
                throw new IndexOutOfBoundsException();
            }
        }
        return result;        
    }
    
    @Override
    public void link(IObservable observable, IObservation obs) {
        synchronized (parameterValues) {
            IObservation observation = parameterValues.get(observable);
            if (null != observation) {
                observation.link(obs);
            }
        }
    }

    @Override
    public void unlink(IObservable observable, IObservation obs) {
        synchronized (parameterValues) {
            IObservation observation = parameterValues.get(observable);
            if (null != observation) {
                observation.unlink(obs);
            }
        }
    }
    
    @Override
    public boolean statisticsWhileReading(IObservable observable) {
        boolean result = false;
        synchronized (parameterValues) {
            IObservation observation = parameterValues.get(observable);
            if (null != observation) {
                result = observation.statisticsWhileReading();
            }
        }
        return result;
    }
    
    @Override
    public AtomicDouble getValue(IObservable observable, Object key) {
        AtomicDouble result = null;
        synchronized (parameterValues) {
            IObservation observation = parameterValues.get(observable);
            if (null != observation) {
                result = observation.getValue(key);
            }
        }
        return result;
    }

    @Override
    public boolean supportsObservation(IObservable observable) {
        synchronized (parameterValues) {
            return null != parameterValues.get(observable);
        }
    }

    @Override
    public String getName() {
        return name;
    }
    
    /**
     * Returns the part type.
     * 
     * @return the part type
     */
    public IPartType getType() {
        return type;
    }
    
    /**
     * Clears this system part.
     */
    protected void clear() {
        synchronized (parameterValues) {
            for (IObservation obs : parameterValues.values()) {
                obs.clear();
            }
        }
    }
    
    /**
     * Fills the frozen system state.
     * 
     * @param prefix the prefix to be used for filling
     * @param name the name of the element
     * @param state the state to be filled (modified as a side effect)
     * @param factors adjustment factors for individual observables (may be <b>null</b>)
     */
    protected void fill(String prefix, String name, FrozenSystemState state, Map<IObservable, Double> factors) {
        synchronized (parameterValues) {
            for (Map.Entry<IObservable, IObservation> entry : parameterValues.entrySet()) {
                IObservation observation = entry.getValue();
                if (observation.isValueSet()) {
                    double factor = 1;
                    if (null != factors) {
                        Double f = factors.get(entry.getKey());
                        if (null != f) {
                            factor = f;
                        }
                    }
                    state.setObservation(prefix, name, entry.getKey(), observation.getValue() * factor);
                }
            }
        }
    }

    @Override
    public Collection<IObservable> observables() {
        return parameterValues.keySet();
    }
    
    /**
     * Links the observables of <code>part</code> into this observable.
     * 
     * @param part the part to take the observables to unlink from (may be <b>null</b>)
     * @param selector selects the relevant observables
     * @see #linkImpl(SystemPart, boolean)
     */
    protected void link(SystemPart part, ILinkSelector selector) {
        linkImpl(part, true, selector);
    }
    
    /**
     * Unlinks the observables of <code>part</code> from this observable.
     * 
     * @param part the part to take the observables to unlink from (may be <b>null</b>)
     * @param selector selects the relevant observables
     * @see #linkImpl(SystemPart, boolean)
     */
    protected void unlink(SystemPart part, ILinkSelector selector) {
        linkImpl(part, false, selector);
    }

    /**
     * Links/unlinks the observables of <code>part</code> from this observable.
     * 
     * @param part the part to take the observables to unlink from (may be <b>null</b>)
     * @param link to link or not to link
     * @param selector selects the relevant observables
     * @see #isLinkEnabled(IObservable)
     */
    private void linkImpl(SystemPart part, boolean link, ILinkSelector selector) {
        if (null != part) {
            synchronized (part.parameterValues) {
                for (Map.Entry<IObservable, IObservation> value : part.parameterValues.entrySet()) {
                    IObservable observable = value.getKey();
                    if (selector.isLinkEnabled(observable)) {
                        IObservation observation = value.getValue();
                        IObservation myObservation = parameterValues.get(observable);
                        if (null != myObservation) {
                            if (link) {
                                myObservation.link(observation);
                            } else {
                                myObservation.unlink(observation);    
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Formats a map of system parts.
     * 
     * @param map the map
     * @param indent the indentation
     * @return the textual representation
     */
    public static String format(Map<String, ? extends SystemPart> map, String indent) {
        String result = "";
        for (Map.Entry<String, ? extends SystemPart> entry : map.entrySet()) {
            result += "\n" + indent + entry.getKey() + "=" + entry.getValue().format(indent + " ");
        }
        return result;
    }

    /**
     * Creates a textual representation but in contrast to {@link #toString()} in this case performs pretty printing.
     * 
     * @param indent the indentation
     * @return the textual representation
     */
    public String format(String indent) {
        return toString() + " " + System.identityHashCode(this);
    }
 
    @Override
    public String toString() {
        return name + " " + type + " " + componentType + " " + parameterValues 
            + (null != valueStore ? " store " + valueStore : "");
    }

    @Override
    public PipelineTopology getTopology() {
        return null;
    }

    @Override
    public ITopologyProjection getTopologyProjection() {
        return null;
    }

    @Override
    public PipelineNodeSystemPart getNode(String name) {
        return null;
    }

    @Override
    public ITopologyProvider getTopologyProvider() {
        return this;
    }

    @Override
    public Type getComponentType() {
        return componentType;
    }

    @Override
    public IObservationProvider getParent() {
        return null;
    }

}