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
package eu.qualimaster.base.algorithm;

/**
 * Calculates the average incrementally.
 * 
 * @author Holger Eichelberger
 */
public class IncrementalAverage {

    private long count;
    private double average;

    /**
     * Adds a value to the average.
     * 
     * @param value the new value
     */
    public void addValue(double value) {
        count++;
        average = average + (value - average) / count;
        if (count < 0) {
            average = 0;
            count = 0;
        }
    }
    
    /**
     * Returns the average collected so far.
     * 
     * @return the average
     */
    public double getAverage() {
        return average;
    }
    
    /**
     * Returns the number of measurements collected so far.
     * 
     * @return the number of measurements
     */
    public long getCount() {
        return count;
    }
    
}
