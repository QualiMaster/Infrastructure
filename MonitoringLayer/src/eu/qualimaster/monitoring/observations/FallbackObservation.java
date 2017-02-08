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

import eu.qualimaster.monitoring.observations.ObservationFactory.IObservationCreator;
import eu.qualimaster.monitoring.parts.IPartType;
import eu.qualimaster.observables.IObservable;

/**
 * A fallback observation combining two observations, a foreground observation and a fallback observation. As long
 * as no value is set to this observation, the fallback observation (typically a {@link ReferencingObservation} will 
 * provide the values. If a value is set, the foreground observation creator creates lazily the foreground observation,
 * which takes over recording, aggregating and returning values.
 * 
 * @author Holger Eichelberger
 */
public class FallbackObservation implements IObservation {

    private static final long serialVersionUID = 3056421089356648829L;
    private IObservationCreator foregroundObservationCreator;
    private IObservation foregroundObservation;
    private IObservation fallbackObservation;
    private IObservation activeReadingObservation;
    private IObservable observable;
    private IPartType partType;
    private IObservationProvider observationProvider;

    /**
     * Creates a fallback observation.
     * 
     * @param foregroundObservationCreator the observation creator for lazy initialization of the foreground observation
     * @param fallbackObservation the fallback observation to be used if there is no foreground observation
     * @param observable the observable the observation is created for
     * @param partType the part type the observation is created for
     * @param observationProvider the observation provider the observation is aggregating on
     */
    public FallbackObservation(IObservationCreator foregroundObservationCreator, 
        IObservation fallbackObservation, IObservable observable, IPartType partType, 
        IObservationProvider observationProvider) {
        this.foregroundObservationCreator = foregroundObservationCreator;
        this.fallbackObservation = fallbackObservation;
        this.activeReadingObservation = fallbackObservation;
        this.observable = observable;
        this.partType = partType;
        this.observationProvider = observationProvider;
    }

    @Override
    public boolean isComposite() {
        return activeReadingObservation.isComposite();
    }
    
    /**
     * Performs a lazy initialization of the foreground observation (call upon value changes).
     */
    private void lazyInitForegroundObservation() {
        if (null == foregroundObservation) {
            foregroundObservation = foregroundObservationCreator.create(observable, partType, observationProvider);
            activeReadingObservation = foregroundObservation;
        }
    }
    
    /**
     * Switches over the active reading observation to the foreground observation. Adjusts the links if needed.
     * Shall be called after setting the first value to the foreground observation to shadow handover.
     */
    private void setActiveReadingObservationToForeground() {
        if (null != foregroundObservation && activeReadingObservation != foregroundObservation) {
            activeReadingObservation = foregroundObservation;
            for (int l = 0; l < fallbackObservation.getLinkCount(); l++) {
                activeReadingObservation.link(fallbackObservation.getLink(l));
            }
            // fallbackObservation = null; // ??
        }
    }

    @Override
    public void setValue(double value, Object key) {
        lazyInitForegroundObservation();
        foregroundObservation.setValue(value, key);
        setActiveReadingObservationToForeground();
    }

    @Override
    public void setValue(Double value, Object key) {
        lazyInitForegroundObservation();
        foregroundObservation.setValue(value, key);
        setActiveReadingObservationToForeground();
    }

    @Override
    public void incrementValue(double value, Object key) {
        lazyInitForegroundObservation();
        foregroundObservation.incrementValue(value, key);
        setActiveReadingObservationToForeground();
    }

    @Override
    public void incrementValue(Double value, Object key) {
        lazyInitForegroundObservation();
        foregroundObservation.incrementValue(value, key);
        setActiveReadingObservationToForeground();
    }

    @Override
    public double getValue() {
        return activeReadingObservation.getValue();
    }

    @Override
    public double getLocalValue() {
        return activeReadingObservation.getLocalValue();
    }

    @Override
    public AtomicDouble getValue(Object key) {
        return activeReadingObservation.getValue(key);
    }

    @Override
    public boolean isValueSet() {
        return activeReadingObservation.isValueSet();
    }

    @Override
    public void clear() {
        activeReadingObservation.clear();
    }

    @Override
    public IObservation copy(IObservationProvider provider) {
        FallbackObservation result = new FallbackObservation(foregroundObservationCreator, 
            fallbackObservation.copy(provider), observable, partType, provider);
        if (null != foregroundObservation) {
            result.foregroundObservation = foregroundObservation.copy(provider);
        }
        return result;
    }

    @Override
    public long getLastUpdate() {
        return activeReadingObservation.getLastUpdate();
    }

    @Override
    public long getFirstUpdate() {
        return activeReadingObservation.getFirstUpdate();
    }

    @Override
    public void setLastUpdate(long timestamp) {
        activeReadingObservation.setLastUpdate(timestamp); // unsure
    }

    @Override
    public int getComponentCount() {
        return activeReadingObservation.getComponentCount();
    }

    @Override
    public void clearComponents(Collection<Object> keys) {
        activeReadingObservation.clearComponents(keys);
    }

    @Override
    public Set<Object> getComponentKeys() {
        return activeReadingObservation.getComponentKeys();
    }

    @Override
    public void replaceComponentKeys(Object oldKey, Object newKey) {
        activeReadingObservation.replaceComponentKeys(oldKey, newKey);
    }

    @Override
    public void link(IObservation observation) {
        activeReadingObservation.link(observation);
    }

    @Override
    public void unlink(IObservation observation) {
        activeReadingObservation.unlink(observation);
    }

    @Override
    public int getLinkCount() {
        return activeReadingObservation.getLinkCount();
    }

    @Override
    public IObservation getLink(int index) {
        return activeReadingObservation.getLink(index);
    }

    @Override
    public boolean statisticsWhileReading() {
        return activeReadingObservation.statisticsWhileReading();
    }

    @Override
    public void switchedTo(boolean direct) {
        activeReadingObservation.switchedTo(direct);
    }
    
    @Override
    public String toString() {
        return "Fallback " + activeReadingObservation;
    }
    
}
