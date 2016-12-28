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

/**
 * Represents a (dummy) constant observation.
 * 
 * @author Holger Eichelberger
 */
public class ConstantObservation implements IObservation {

    public static final IObservation NULL_OBSERVATION = new ConstantObservation(0);
    private static final long serialVersionUID = -5109453973120477761L;
    private double value;

    /**
     * Creates a new no-observation.
     * 
     * @param value the constant value of the observation
     */
    public ConstantObservation(double value) {
        this.value = value;
    }
    
    @Override
    public boolean isComposite() {
        return false;
    }

    @Override
    public void setValue(double value, Object key) {
    }

    @Override
    public void setValue(Double value, Object key) {
    }

    @Override
    public void incrementValue(double value, Object key) {
    }

    @Override
    public void incrementValue(Double value, Object key) {
    }

    @Override
    public double getValue() {
        return value;
    }

    @Override
    public double getLocalValue() {
        return value;
    }

    @Override
    public AtomicDouble getValue(Object key) {
        return null;
    }

    @Override
    public boolean isValueSet() {
        return true;
    }

    @Override
    public void clear() {
    }

    @Override
    public IObservation copy(IObservationProvider provider) {
        IObservation result;
        if (this == NULL_OBSERVATION) {
            result = NULL_OBSERVATION; 
        } else {
            result = new ConstantObservation(value);
        }
        return result;
    }

    @Override
    public long getFirstUpdate() {
        return 0;
    }
    
    @Override
    public long getLastUpdate() {
        return 0;
    }
    
    @Override
    public void setLastUpdate(long timestamp) {
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
    }

    @Override
    public void unlink(IObservation observation) {
    }
    
    @Override
    public int getLinkCount() {
        return 0;
    }

    @Override
    public IObservation getLink(int index) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public boolean statisticsWhileReading() {
        return false;
    }

    @Override
    public String toString() {
        return "Const[" + value + "]";
    }

    @Override
    public void switchedTo(boolean direct) {
    }

}
