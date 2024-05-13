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
package eu.qualimaster.monitoring.profiling.approximation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.math3.fitting.WeightedObservedPoint;

/**
 * Container for weighted observed points used maintaining only the last recent values for given abscissa values.
 *
 * @since 3.3
 */
public class LastRecentWeightedObservedPoints {

    private TreeMap<Integer, WeightedObservedPoint> observations = new TreeMap<>();
    
    /**
     * Adds a point to the sample.
     * Calling this method is equivalent to calling
     * {@code add(1.0, x, y)}.
     *
     * @param x Abscissa of the point.
     * @param y Observed value  at {@code x}. After fitting we should
     * have {@code f(x)} as close as possible to this value.
     *
     * @see #add(double, double, double)
     * @see #add(WeightedObservedPoint)
     * @see #toList()
     */
    public void add(double x, double y) {
        add(1d, x, y);
    }

    /**
     * Adds a point to the sample.
     *
     * @param weight Weight of the observed point.
     * @param x Abscissa of the point.
     * @param y Observed value  at {@code x}. After fitting we should
     * have {@code f(x)} as close as possible to this value.
     *
     * @see #add(double, double)
     * @see #add(WeightedObservedPoint)
     * @see #toList()
     */
    public void add(double weight, double x, double y) {
        add(new WeightedObservedPoint(weight, x, y));
    }

    /**
     * Adds a point to the sample.
     *
     * @param observed Observed point to add.
     *
     * @see #add(double, double)
     * @see #add(double, double, double)
     * @see #toList()
     */
    public void add(WeightedObservedPoint observed) {
        if (null != observed) {
            double x = observed.getX();
            int key = (int) x;
            observations.put(key, observed);
        }
    }
    
    /**
     * Gets a <em>snapshot</em> of the observed points.
     * The list of stored points is copied in order to ensure that
     * modification of the returned instance does not affect this
     * container.
     * Conversely, further modification of this container (through
     * the {@code add} or {@code clear} methods) will not affect the
     * returned list.
     *
     * @return the observed points, in the order they were added to this
     * container.
     *
     * @see #add(double, double)
     * @see #add(double, double, double)
     * @see #add(WeightedObservedPoint)
     */
    public List<WeightedObservedPoint> toList() {
        // The copy is necessary to ensure thread-safety because of the
        // "clear" method (which otherwise would be able to empty the
        // list of points while it is being used by another thread).
        return new ArrayList<WeightedObservedPoint>(observations.values());
    }
    
    /**
     * Returns a snapshot of observed points.
     * 
     * @return x in the first array, y in the second array, length of both arrays is {@link #size()}.
     */
    protected double[][] getPointArrays() {
        double[][] result = new double[2][];
        int count = observations.size();
        double[] x = new double[count];
        double[] y = new double[count];
        result[0] = x;
        result[1] = y;
        
        count = 0;
        for (WeightedObservedPoint p : observations.values()) {
            x[count] = p.getX(); 
            y[count] = p.getY(); 
            count++;
        }
        return result;
    }
    
    /**
     * Removes all observations from this container.
     */
    public void clear() {
        observations.clear();
    }

    /**
     * Returns whether this and the given data set contains the same data.
     * 
     * @param approx the approximator to compare with
     * @return <code>true</code> for the same data, <code>false</code> else
     */
    public boolean containsSameData(LastRecentWeightedObservedPoints approx) {
        boolean equals = false;
        if (null != approx) {
            Collection<WeightedObservedPoint> o1 = observations.values();
            Collection<WeightedObservedPoint> o2 = approx.observations.values();
            if (o1.size() == o2.size()) {
                Iterator<WeightedObservedPoint> i1 = o1.iterator();
                Iterator<WeightedObservedPoint> i2 = o2.iterator();
                equals = true;
                while (equals && i1.hasNext()) { // o1.size() == o2.size()
                    WeightedObservedPoint p1 = i1.next();
                    WeightedObservedPoint p2 = i2.next();
                    equals = Math.abs(p1.getX() - p2.getX()) < 0.005;
                    equals = Math.abs(p1.getY() - p2.getY()) < 0.005;
                    equals = Math.abs(p1.getWeight() - p2.getWeight()) < 0.005;
                }
            }
        }
        return equals;
    }
    
    /**
     * Returns the number of data points.
     * 
     * @return the number of data points
     */
    public int size() {
        return observations.size();
    }
    
}
