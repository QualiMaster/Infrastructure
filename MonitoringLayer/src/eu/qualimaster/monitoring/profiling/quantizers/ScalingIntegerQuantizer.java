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
 * A scaling integer quantizer, which determines the quantization step by the logarithm of value - 1.
 * 
 * @author Holger Eichelberger
 */
public class ScalingIntegerQuantizer extends Quantizer<Integer> {

    public static final ScalingIntegerQuantizer INSTANCE = new ScalingIntegerQuantizer();
    
    /**
     * Creates an integer quantizer.
     */
    private ScalingIntegerQuantizer() {
        super(Integer.class);
    }

    @Override
    protected int quantizeImpl(Integer value) {
        int log10 = (int) Math.log10(value);
        if (log10 > 1) {
            log10--;
        }
        int step = (int) Math.pow(10, log10);
        int v = value.intValue();
        int sgn = v < 0 ? -1 : 1;
        return (int) ((v / (double) step) + sgn * 0.5) * step;
    }

}
