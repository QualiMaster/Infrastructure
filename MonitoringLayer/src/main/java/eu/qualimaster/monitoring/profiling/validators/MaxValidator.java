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
 * Implements a validator ensuring maximum values.
 * 
 * @author Holger Eichelberger
 */
public class MaxValidator implements IValidator {

    private double max;
    
    /**
     * Creates a maximum-validator.
     * 
     * @param max the maximum allowed value
     */
    public MaxValidator(double max) {
        this.max = max;
    }

    @Override
    public double validate(double value) {
        double result;
        if (value > max) {
            result = max;
        } else {
            result = value;
        }
        return result;
    }
    
}
