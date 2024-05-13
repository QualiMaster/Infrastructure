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
 * Defines the validation interface for predicted values.
 * 
 * @author Holger Eichelberger
 */
public interface IValidator {

    /**
     * Validates a predicted <code>value</code>.
     * 
     * @param value the predicted value
     * @return the validated/corrected predicted value, may be <code>value</code> but also corrected if invalid
     */
    public double validate(double value);

}
