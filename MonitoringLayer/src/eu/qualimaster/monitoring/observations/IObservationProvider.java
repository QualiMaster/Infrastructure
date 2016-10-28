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
 * Describes the basic reading interface to a set of observations.
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
     * Returns the last update. [convenience method]
     * 
     * @param observable the value to be returned for
     * @return may be <b>-1</b> if <code>observable</code> is not supported or has not been monitored. In order to 
     *   discriminate these situations, please call {@link #supportsObservation(IObservable)} 
     *   or {@link #hasValue(IObservable)}. 
     */
    public long getLastUpdate(IObservable observable);

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
    
}
