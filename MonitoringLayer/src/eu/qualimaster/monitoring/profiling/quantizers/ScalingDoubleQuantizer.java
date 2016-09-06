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

/**
 * A quantizer for double values which, which determines the scale (log) based on the given value.
 * 
 * @author Holger Eichelberger
 */
public class ScalingDoubleQuantizer extends Quantizer<Double> {

    public static final ScalingDoubleQuantizer INSTANCE = new ScalingDoubleQuantizer();
    
    /**
     * Creates a double quantizer.
     */
    private ScalingDoubleQuantizer() {
        super(Double.class);
    }

    @Override
    protected int quantizeImpl(Double value) {
        int log10 = (int) Math.log10(value);
        if (log10 > 1) {
            log10--;
        }
        int step = (int) Math.pow(10, log10);
        return ((int) Math.round(value.doubleValue() / step)) * step;
    }

}
