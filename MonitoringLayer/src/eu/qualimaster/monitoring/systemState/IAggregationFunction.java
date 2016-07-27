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

/**
 * Defines the interface for a binary function with some basic implementations.
 * Constant implementations shall not have an own state, other implementations may have.
 * 
 * @author Holger Eichelberger
 */
public interface IAggregationFunction extends Serializable {

    /**
     * Maximum aggregation function without average.
     */
    public static final IAggregationFunction MAX = new IAggregationFunction() {

        private static final long serialVersionUID = -2113267059849863722L;

        @Override
        public double calculate(double value1, double value2) {
            return Math.max(value1, value2);
        }

        @Override
        public double getInitialValue() {
            return Double.MIN_VALUE;
        }

        @Override
        public String getName() {
            return "max";
        }

        @Override
        public boolean doAverage() {
            return false;
        }

    };

    /**
     * Minimum aggregation function without average.
     */
    public static final IAggregationFunction MIN = new IAggregationFunction() {

        private static final long serialVersionUID = 6801685574166072723L;

        @Override
        public double calculate(double value1, double value2) {
            return Math.min(value1, value2);
        }

        @Override
        public double getInitialValue() {
            return Double.MAX_VALUE;
        }
        
        @Override
        public String getName() {
            return "min";
        }

        @Override
        public boolean doAverage() {
            return false;
        }

    };

    /**
     * Sum aggregation function.
     */
    public static final IAggregationFunction SUM = new IAggregationFunction() {

        private static final long serialVersionUID = -9009507883268183095L;

        @Override
        public double calculate(double value1, double value2) {
            return value1 + value2;
        }

        @Override
        public double getInitialValue() {
            return 0;
        }

        @Override
        public String getName() {
            return "sum";
        }

        @Override
        public boolean doAverage() {
            return false;
        }

    };

    /**
     * Average aggregation function.
     */
    public static final IAggregationFunction AVG = new IAggregationFunction() {

        private static final long serialVersionUID = 3904930195958799505L;

        @Override
        public double calculate(double value1, double value2) {
            return value1 + value2;
        }

        @Override
        public double getInitialValue() {
            return 0;
        }

        @Override
        public String getName() {
            return "avg";
        }

        @Override
        public boolean doAverage() {
            return true;
        }

    };
    
    /**
     * Calculates a value from <code>value1</code> and <code>value2</code>.
     *  
     * @param value1 the first value to consider
     * @param value2 the second value to consider
     * @return the result value
     */
    public double calculate(double value1, double value2);
    
    /**
     * Returns the typical initial value to be used with this function.
     * 
     * @return the initial value
     */
    public double getInitialValue();
    
    /**
     * Returns the average value.
     * 
     * @return the average
     */
    public boolean doAverage();
    
    /**
     * Returns a descriptive name.
     * 
     * @return the descriptive name
     */
    public String getName();

}