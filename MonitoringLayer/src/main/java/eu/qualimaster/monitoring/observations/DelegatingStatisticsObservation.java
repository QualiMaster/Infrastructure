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

import java.util.concurrent.atomic.AtomicLong;

/**
 * Implements a statistics summarizing compound observation by delegating basic
 * values to a given observation for determining the value. This allows using this
 * class for compound and (single) non-compound observations.
 * 
 * @author Holger Eichelberger
 */
public class DelegatingStatisticsObservation extends AbstractDelegatingObservation implements IStatisticsObservation {

    private static final long serialVersionUID = 1453425307466017603L;
    private AtomicDouble min = new AtomicDouble(Double.MAX_VALUE);
    private AtomicDouble max = new AtomicDouble(Double.MIN_VALUE);
    private AtomicDouble sum = new AtomicDouble();
    private AtomicLong count = new AtomicLong(); // TODO think about overflow
    private boolean averageAsValue;

    /**
     * Creates a delegating statistics observation returning the actual value.
     * 
     * @param observation the basic observation to take the values from
     */
    public DelegatingStatisticsObservation(IObservation observation) {
        this(observation, false);
    }
    
    /**
     * Creates a delegating statistics observation.
     * 
     * @param observation the basic observation to take the values from
     * @param averageAsValue if the value of the delegate (<code>false</code>) or the average (<code>true</code>) shall 
     *     be returned as value
     */
    public DelegatingStatisticsObservation(IObservation observation, boolean averageAsValue) {
        super(observation);
        this.averageAsValue = averageAsValue;
    }
    
    /**
     * Creates a new instance from a given <code>source</code>.
     * 
     * @param source the source observation to copy from
     * @param provider the parent observation provider
     */
    protected DelegatingStatisticsObservation(DelegatingStatisticsObservation source, IObservationProvider provider) {
        super(source, provider);
        this.min.set(source.min.get());
        this.max.set(source.max.get());
        this.sum.set(source.sum.get());
        this.count.set(source.count.get());
        this.averageAsValue = source.averageAsValue;
    }

    /**
     * Collects the statistics.
     */
    private void collectStatistics() {
        double value = getDelegate().getValue();
        sum.addAndGet(value);
        count.incrementAndGet();
        min.set(Math.min(min.get(), value));
        max.set(Math.max(max.get(), value));
    }

    @Override
    public void setValue(double value, Object key) {
        IObservation delegate = getDelegate();
        delegate.setValue(value, key);
        if (!statisticsWhileReading()) {
            collectStatistics();
        }
    }

    @Override
    public void setValue(Double value, Object key) {
        if (null != value) {
            setValue(value.doubleValue(), key);
        }
    }

    @Override
    public void incrementValue(double value, Object key) {
        IObservation delegate = getDelegate();
        delegate.incrementValue(value, key);
        if (!statisticsWhileReading()) {
            collectStatistics();
        }
    }
    
    @Override
    public void incrementValue(Double value, Object key) {
        if (null != value) {
            incrementValue(value.doubleValue(), key);
        } 
    }

    @Override
    public double getValue() {
        return getValue0();
    }

    @Override
    public double getLocalValue() {
        return getValue0();
    }

    /**
     * Returns the value.
     * 
     * @return the value
     */
    private double getValue0() {
        double result;
        if (statisticsWhileReading()) {
            collectStatistics();
        }
        if (averageAsValue) {
            result = getAverageValue();
        } else {
            result = super.getValue();
        }
        return result;
    }

    @Override
    public AtomicDouble getValue(Object key) {
        if (statisticsWhileReading()) {
            collectStatistics();
        }
        return super.getValue(key);
    }

    @Override
    public double getAverageValue() {
        double result;
        if (count.get() > 0) {
            result = sum.get() / count.get();
        } else {
            result = 0;
        }
        return result;
    }

    @Override
    public double getMinimumValue() {
        return min.get();
    }

    @Override
    public double getMaximumValue() {
        return max.get();
    }

    @Override
    public boolean isValueSet() {
        return count.get() != 0;
    }

    @Override
    public void clear() {
        super.clear();
        clearImpl();
    }

    /**
     * Implements clearing this instance.
     */
    private void clearImpl() {
        min.set(Double.MAX_VALUE);
        max.set(Double.MIN_VALUE);
        sum.set(0);
        count.set(0);
    }
    
    @Override
    public void switchedTo(boolean direct) {
        if (!direct) {
            clearImpl();
        }
        super.switchedTo(direct);
    }

    @Override
    public IObservation copy(IObservationProvider provider) {
        return new DelegatingStatisticsObservation(this, provider);
    }

    @Override
    public String toString() {
        String result = toStringShortcut();
        if (count.get() > 0) {
            result += "[" + min + " " + getValue(null) + " " + max + ";" + getDelegate().toString() + "]";
        } else {
            result += " -";
        }
        return result;
    }
    
    @Override
    protected String toStringShortcut() {
        return "Stat";
    }

}
