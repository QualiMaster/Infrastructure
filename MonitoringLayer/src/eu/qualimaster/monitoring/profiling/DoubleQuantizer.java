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

/**
 * A quantizer for double values.
 * 
 * @author Holger Eichelberger
 */
public class DoubleQuantizer extends Quantizer<Double> {

    public static final DoubleQuantizer TO_INT = new DoubleQuantizer(1);
    public static final DoubleQuantizer STEP_100 = new DoubleQuantizer(100);
    public static final DoubleQuantizer STEP_1000 = new DoubleQuantizer(1000);
    
    private int step;
    
    /**
     * Creates a double quantizer.
     * 
     * @param step the quantization step
     */
    public DoubleQuantizer(int step) {
        super(Double.class);
        this.step = Math.max(1, step);
    }

    @Override
    protected int quantizeImpl(Double value) {
        return ((int) Math.round(value.doubleValue())) % step;
    }

}
