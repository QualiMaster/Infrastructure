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

import eu.qualimaster.coordination.INameMapping.Component.Type;
import eu.qualimaster.monitoring.topology.ITopologyProvider;
import eu.qualimaster.observables.IObservable;

/**
 * Describes the basic reading interface to a set of observations. Initially, this interface was intended to hide
 * most of the system part operations. However, due to the need of the referencing observation the interface became
 * more open than intended. Other option would have been to expose the internal observables of the system part. 
 * 
 * @author Holger Eichelberger
 */
public interface IObservationProvider {

    /**
     * Returns the name of the provider.
     * 
     * @return the name
     */
    public String getName();

    /**
     * Returns a measured quality parameter value component. [convenience method]
     * 
     * @param observable the value to be returned for
     * @param key the key representing the compound in a composite observation, may be <b>null</b>
     * @return may be <b>null</b> if <code>observable</code> is not supported or has not been monitored or the 
     *   component for <code>key</code> does not exist. In order to discriminate these situations, please call 
     *   {@link #supportsObservation(IObservable)} or {@link #hasValue(IObservable)}. 
     */
    public ObservedValue getObservedValue(IObservable observable, Object key);
    
    /**
     * Returns a measured quality parameter value. [convenience method]
     * 
     * @param observable the value to be returned for
     * @return may be <b>0</b> if <code>observable</code> is not supported or has not been monitored. In order to 
     *   discriminate these situations, please call {@link #supportsObservation(IObservable)} 
     *   or {@link #hasValue(IObservable)}. 
     */
    public double getObservedValue(IObservable observable);

    /**
     * Returns all observables configured for this system part.
     * 
     * @return all observables
     */
    public Collection<IObservable> observables();

    /**
     * Returns a measured quality parameter value as integer. [convenience method]
     * 
     * @param observable the value to be returned for
     * @return may be <b>0</b> if parameter value is not supported or has not been monitored. In order to 
     *   discriminate these situations, please call {@link #supportsObservation(IObservable)} 
     *   or {@link #hasValue(IObservable)}. 
     */
    public int getObservedValueInt(IObservable observable);
    
    /**
     * Returns the last update. 
     * 
     * @param observable the value to be returned for
     * @return may be <b>-1</b> if <code>observable</code> is not supported or has not been monitored. In order to 
     *   discriminate these situations, please call {@link #supportsObservation(IObservable)} 
     *   or {@link #hasValue(IObservable)}. 
     */
    public long getLastUpdate(IObservable observable);
    
    /**
     * Returns the first update. 
     * 
     * @param observable the value to be returned for
     * @return may be <b>-1</b> if <code>observable</code> is not supported or has not been monitored. In order to 
     *   discriminate these situations, please call {@link #supportsObservation(IObservable)} 
     *   or {@link #hasValue(IObservable)}. 
     */
    public long getFirstUpdate(IObservable observable);

    /**
     * Called to indicate that state-based cleanup shall be performed (if needed) after an algorithm switch.
     * 
     * @param direct whether this is a direct call or an indirect call through other observations
     * @param observable the observable to inform
     */
    public void switchedTo(IObservable observable, boolean direct);
    
    /**
     * Maniuplates the last update. [testing only]
     * 
     * @param observable the value to manipulate
     * @param timestamp may be <b>-1</b> if <code>observable</code> is not supported or has not been monitored. In order
     *   to  discriminate these situations, please call {@link #supportsObservation(IObservable)} 
     *   or {@link #hasValue(IObservable)}. 
     */
    public void setLastUpdate(IObservable observable, long timestamp);
    
    /**
     * Returns whether this part supports the given observable.
     * 
     * @param observable the observation to be returned for
     * @return <code>true</code> if <code>observable</code> is supported, <code>false</code> else
     */
    public boolean supportsObservation(IObservable observable);

    /**
     * Returns whether this part has observed the given observable.
     * 
     * @param observable the observation to be returned for
     * @return <code>true</code> if <code>observable</code> was observed, <code>false</code> else
     */
    public boolean hasValue(IObservable observable);

    /**
     * Returns the component count for <code>observable</code>.
     * 
     * @param observable denotes the observable to return the component count for
     * @return the component count, 0 if there are yet no components
     */
    public int getComponentCount(IObservable observable);

    /**
     * Returns the component keys for <code>observable</code>.
     * 
     * @param observable the component count
     * @return the component keys, an empty collection if there are none
     */
    public Set<Object> getComponentKeys(IObservable observable);

    /**
     * Replaces a component key. Observations remain.
     * 
     * @param oldKey the old component key
     * @param newKey the new component key
     * @param observables the observables to apply the replacement to, apply to all if not given
     */
    public void replaceComponentKeys(Object oldKey, Object newKey, IObservable... observables);
    
    /**
     * Provides access to the topology of this observation.
     * 
     * @return the topology provider or <b>null</b> if there is none
     */
    public ITopologyProvider getTopologyProvider();
    
    /**
     * Returns the related component type.
     * 
     * @return the component type (may be <b>null</b> if not applicable)
     */
    public Type getComponentType();
    

    
    /**
     * Returns whether the observation for the given observable is a composite.
     * 
     * @param observable the observable
     * @return <code>true</code> if it is a composite, <code>false</code> else (also if <code>observable</code> does 
     *     not exist)
     */
    public boolean isComposite(IObservable observable);

    /**
     * Clears the values of the specified <code>observable</code>. This method is intended for 
     * debugging and shall not be used in usual monitoring code!
     * 
     * @param observable the observable to clear
     */
    public void clear(IObservable observable);

    /**
     * Sets the value of the given <code>observable</code> by replacing the existing one.
     * 
     * @param observable the observable to change
     * @param value the new value (may be <b>null</b> but then this method will perform an update of the data)
     * @param key the key representing the compound in a composite observation,
     *   may be <b>null</b>
     */
    public void setValue(IObservable observable, double value, Object key);
    
    /**
     * Sets the value of the given <code>observable</code> by replacing the existing one.
     * 
     * @param observable the observable to be modified
     * @param value the new value (may be <b>null</b> but then this method will perform an update of the data)
     * @param key the key representing the compound in a composite observation,
     *   may be <b>null</b>
     */
    public void setValue(IObservable observable, Double value, Object key);
    
    /**
     * Increments the of the given <code>observable</code> value by adding <code>value</code> to the existing one. 
     * 
     * @param observable the observable to be modified
     * @param value the new value (may be <b>null</b> but then this method will perform an update of the data)
     * @param key the key representing the compound in a composite observation,
     *   may be <b>null</b>
     */
    public void incrementValue(IObservable observable, double value, Object key);

    /**
     * Increments the of the given <code>observable</code> value by adding <code>value</code> to the existing one. 
     * 
     * @param observable the observable to be modified
     * @param value the new value (may be <b>null</b> but then this method will perform an update of the data)
     * @param key the key representing the compound in a composite observation,
     *   may be <b>null</b>
     */
    public void incrementValue(IObservable observable, Double value, Object key);

    /**
     * Returns a measured quality parameter value. [convenience method]
     * 
     * @param observable the value to be returned for
     * @param localValue the locally stored or the (topologically) aggregated value
     * @return may be <b>0</b> if <code>observable</code> is not supported or has not been monitored. In order to 
     *   discriminate these situations, please call {@link #supportsObservation(IObservable)} 
     *   or {@link #hasValue(IObservable)}. 
     */
    public double getObservedValue(IObservable observable, boolean localValue);
    
    /**
     * Links the (compounds of the) given observation into the observation of the given observable. Changes
     * on both sides are reflected.
     * 
     * @param observable the observable
     * @param observation the observation to be linked
     * @see #unlink(IObservable, IObservation)
     */
    public void link(IObservable observable, IObservation observation);

    /**
     * Unlinks the (compounds of the) given observation into the observation of the given observable. 
     * 
     * @param observable the observable
     * @param observation the observation to be unlinked
     * @see #link(IObservable, IObservation)
     */
    public void unlink(IObservable observable, IObservation observation);
    
    /**
     * Returns the number of links for the given <code>observable</code>.
     * 
     * @param observable the observable
     * @return the number of links
     */
    public int getLinkCount(IObservable observable);
    
    /**
     * Returns the specific link for the given <code>observable</code>.
     * 
     * @param observable the observable
     * @param index the 0-based index of the link 
     * @return the link
     * @throws IndexOutOfBoundsException if <code>index &lt; 0 || index &gt;={@link #getLinkCount()}</code> 
     */
    public IObservation getLink(IObservable observable, int index);
    
    /**
     * Returns whether the observation of the given observable requires statistics calculation while reading or 
     * while writing.
     * 
     * @param observable the observable to return the property for 
     * @return <code>true</code> for reading, <code>false</code> for writing
     */
    public boolean statisticsWhileReading(IObservable observable);
    
    /**
     * Clears the specified components of the given <code>observable</code>.
     * 
     * @param observable the observable
     * @param keys the keys for the components to be cleared
     */
    public void clearComponents(IObservable observable, Collection<Object> keys);
    
    /**
     * Returns the specific component value of the given observation.
     * 
     * @param observable the observable
     * @param key the key representing the compound in a composite observation,
     *   may be <b>null</b>
     * @return the component value (may be <b>null</b>)
     */
    public AtomicDouble getValue(IObservable observable, Object key);
    
}
