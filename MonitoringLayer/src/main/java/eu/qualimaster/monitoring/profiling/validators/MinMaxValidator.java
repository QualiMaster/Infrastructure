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
package eu.qualimaster.monitoring.profiling.validators;

/**
 * Implements a both-sided validator.
 * 
 * @author Holger Eichelberger
 */
public class MinMaxValidator implements IValidator {
    
    /**
     * The default validator for the range (0; 1).
     */
    public static final IValidator MIN_0_MAX_1_VALIDATOR = new MinMaxValidator(0, 1);

    private double min;
    private double max;
    
    /**
     * Creates a min-max-validator.
     * 
     * @param min the minimum allowed value
     * @param max the maximum allowed value
     */
    public MinMaxValidator(double min, double max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public double validate(double value) {
        double result;
        if (value < min) {
            result = min;
        } else if (value > max) {
            result = max;
        } else {
            result = value;
        }
        return result;
    }

}
