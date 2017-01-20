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

/**
 * Defines a time framed delegating observation, i.e., an observation for which the value
 * is related to the given time frame (e.g, tuples per second) based on an underlying observation. To keep
 * monitoring information stable, we base the measurement on the first point in time the underlying observation
 * received monitoring values. Upon algorithm switch, the related point in time must be cleared properly.
 * 
 * @author Holger Eichelberger
 */
public class DelegatingMovingTimeFramedObservation extends AbstractDelegatingObservation {

    private static final long serialVersionUID = 5671311309699530270L;
    private long timeFrame;
    private long maxTimeBaseDiff = -1;
    
    private double result = 0;
    private long lastUpdate = -1;
    private long timeBase = -1;
    private double valueAtTimeBase = 0;

    /**
     * Creates a time framed summarizing compound observation for the given time frame.
     * 
     * @param observation the observation to delegate to
     * @param timeFrame the time frame to relate the measurement data to (at least 1)
     */
    public DelegatingMovingTimeFramedObservation(IObservation observation, long timeFrame) {
        this(observation, timeFrame, 0);
    }
    
    /**
     * Creates a time framed summarizing compound observation for the given time frame.
     * 
     * @param observation the observation to delegate to
     * @param timeFrame the time frame to relate the measurement data to (at least 1)
     * @param maxTimeBaseDiff the maximum difference between now and the actual time base causing a shift of the time 
     *     base (moving aggregation, aggregation to initial time base if not positive)
     */
    public DelegatingMovingTimeFramedObservation(IObservation observation, long timeFrame, long maxTimeBaseDiff) {
        super(observation);
        this.timeFrame = Math.max(1,  timeFrame);
        this.maxTimeBaseDiff = maxTimeBaseDiff;
    }
    
    /**
     * Creates a time framed summarizing compound observation from a given source.
     * 
     * @param source the source to copy from
     * @param provider the parent observation provider
     */
    protected DelegatingMovingTimeFramedObservation(DelegatingMovingTimeFramedObservation source, 
        IObservationProvider provider) {
        super(source, provider);
        this.timeFrame = source.timeFrame;
        this.result = source.result;
        
        this.lastUpdate = source.lastUpdate;
        this.timeBase = source.timeBase;
        this.valueAtTimeBase = source.valueAtTimeBase;
    }

    @Override
    public void clear() {
        result = 0;
        lastUpdate = -1;
        timeBase = -1; 
        valueAtTimeBase = 0;
        super.clear();
    }

    /**
     * Updates the time-frame observation.
     */
    private synchronized void update() {
        if (maxTimeBaseDiff > 0) {
            boolean update = false;
            if (lastUpdate > 0) {
                update = true;
            } else { // initialization
                long firstUpdate = super.getFirstUpdate();
                update = firstUpdate > 0;
                timeBase = firstUpdate;
            }
            if (update) {
                long now = System.currentTimeMillis();
                long timeDiff = now - timeBase;
                if (timeDiff > 0 && timeDiff >= timeFrame) {
                    double value = super.getValue();
                    result = (value - valueAtTimeBase) / (now - timeBase) * timeFrame;
                    if (Double.isNaN(result) || Double.isInfinite(result)) {
                        result = 0;
                    }
                    lastUpdate = now;
                    if (now - timeBase > maxTimeBaseDiff) {
                        timeBase = now;
                        valueAtTimeBase = value;
                    }
                }
            }
        } else {
            long firstUpdate = super.getFirstUpdate();
            if (firstUpdate > 0) {
                long now = System.currentTimeMillis();
                double value = super.getValue();
                result = value / (now - firstUpdate) * timeFrame;
                if (Double.isNaN(result) || Double.isInfinite(result)) {
                    result = 0;
                }
            }            
        }
        
    }
    
    @Override
    public double getValue() {
        update();
        return result;
    }

    @Override
    public double getLocalValue() {
        // no update, may imply topological aggregation and, thus, loops
        return result;
    }

    @Override
    public IObservation copy(IObservationProvider provider) {
        return new DelegatingMovingTimeFramedObservation(this, provider);
    }
    
    @Override
    public boolean statisticsWhileReading() {
        return true;
    }
    
    @Override
    protected String toStringShortcut() {
        return "TimeM";
    }

}