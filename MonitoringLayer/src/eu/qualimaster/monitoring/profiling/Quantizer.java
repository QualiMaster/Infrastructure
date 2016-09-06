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
package eu.qualimaster.monitoring.profiling;

import java.io.Serializable;

/**
 * A parameter/observable quantizer (as we cannot handle the full spectrum of values).
 * 
 * @param <T> the value type
 * @author Holger Eichelberger
 */
public abstract class Quantizer <T extends Serializable> {
    
    private Class<T> type;

    /**
     * Instantiates a quantizer.
     * 
     * @param type the type of data handled
     */
    protected Quantizer(Class<T> type) {
        this.type = type;
    }
    
    /**
     * Quantizes the given value.
     * 
     * @param value the value to be quantized
     * @return the quantized value
     */
    protected abstract int quantizeImpl(T value);
    
    /**
     * Quantizes the given value.
     * 
     * @param value the value to be quantized
     * @return the quantized value
     */
    public int quantize(Serializable value) {
        return quantizeImpl(type.cast(value));
    }
    
    /**
     * Returns the handled type.
     * 
     * @return the type
     */
    public Class<T> handles() {
        return type;
    }

}
