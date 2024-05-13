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
 * Implements a validator ensuring minimum values.
 * 
 * @author Holger Eichelberger
 */
public class MinValidator implements IValidator {

    /**
     * The default validator for positive values.
     */
    public static final IValidator MIN_0_VALIDATOR = new MinValidator(0);
    
    private double min;
    
    /**
     * Creates a minimum validator.
     * 
     * @param min the minimum allowed value
     */
    public MinValidator(double min) {
        this.min = min;
    }

    @Override
    public double validate(double value) {
        double result;
        if (value < min) {
            result = min;
        } else {
            result = value;
        }
        return result;
    }

}
