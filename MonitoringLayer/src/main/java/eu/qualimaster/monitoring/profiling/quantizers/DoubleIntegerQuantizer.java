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
package eu.qualimaster.monitoring.profiling.quantizers;

import java.io.Serializable;

/**
 * A quantizer turning double values into ints.
 * 
 * @author Holger Eichelberger
 */
public class DoubleIntegerQuantizer extends AbstractDoubleQuantizer {

    public static final DoubleIntegerQuantizer INSTANCE = new DoubleIntegerQuantizer();
    
    /**
     * Creates a double quantizer.
     */
    private DoubleIntegerQuantizer() {
        super();
    }

    @Override
    public int quantize(Serializable value) {
        int result;
        if (value instanceof Integer) {
            result = (Integer) value;
        } else {
            result = super.quantize(value);
        }
        return result;
    }

    @Override
    protected int quantizeImpl(Double value) {
        return (int) Math.round(value);
    }
    
    @Override
    public Serializable parse(String text) {
        Serializable result;
        try {
            // this is a to-int quantizer!
            result = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            result = super.parse(text);
        }
        return result;
    }

}
