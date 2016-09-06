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
package tests.eu.qualimaster.monitoring.profiling;

import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.monitoring.profiling.quantizers.DoubleIntegerQuantizer;
import eu.qualimaster.monitoring.profiling.quantizers.IdentityIntegerQuantizer;
import eu.qualimaster.monitoring.profiling.quantizers.RoundingDoubleQuantizer;
import eu.qualimaster.monitoring.profiling.quantizers.RoundingIntegerQuantizer;
import eu.qualimaster.monitoring.profiling.quantizers.ScalingDoubleQuantizer;
import eu.qualimaster.monitoring.profiling.quantizers.ScalingIntegerQuantizer;

/**
 * Tests the quantizers.
 * 
 * @author Holger Eichelberger
 */
public class QuantizerTest {

    /**
     * Tests the basic integer quantizers (identity, rounding).
     */
    @Test
    public void testIntQuantizer() {
        Assert.assertEquals(1234, IdentityIntegerQuantizer.INSTANCE.quantize(1234));
        Assert.assertEquals(-1234, IdentityIntegerQuantizer.INSTANCE.quantize(-1234));
        Assert.assertEquals(0, IdentityIntegerQuantizer.INSTANCE.quantize(0));
        
        Assert.assertEquals(0, RoundingIntegerQuantizer.STEP_100.quantize(0));
        Assert.assertEquals(200, RoundingIntegerQuantizer.STEP_100.quantize(225));
        Assert.assertEquals(300, RoundingIntegerQuantizer.STEP_100.quantize(275));

        Assert.assertEquals(0, RoundingIntegerQuantizer.STEP_1000.quantize(0));
        Assert.assertEquals(0, RoundingIntegerQuantizer.STEP_1000.quantize(225));
        Assert.assertEquals(1000, RoundingIntegerQuantizer.STEP_1000.quantize(1275));
        Assert.assertEquals(2000, RoundingIntegerQuantizer.STEP_1000.quantize(1775));
    }
    
    /**
     * Tests the self-scaling integer quantizer.
     */
    @Test
    public void testScalingIntQuantizer() {
        Assert.assertEquals(-3, ScalingIntegerQuantizer.INSTANCE.quantize(-3));
        Assert.assertEquals(0, ScalingIntegerQuantizer.INSTANCE.quantize(0));
        Assert.assertEquals(3, ScalingIntegerQuantizer.INSTANCE.quantize(3));
        Assert.assertEquals(30, ScalingIntegerQuantizer.INSTANCE.quantize(25));
        Assert.assertEquals(530, ScalingIntegerQuantizer.INSTANCE.quantize(525));
        Assert.assertEquals(1500, ScalingIntegerQuantizer.INSTANCE.quantize(1525));
        Assert.assertEquals(1600, ScalingIntegerQuantizer.INSTANCE.quantize(1625));
    }

    /**
     * Tests the basic double quantizers.
     */
    @Test
    public void testDoubleQuantizer() {
        Assert.assertEquals(1234, DoubleIntegerQuantizer.INSTANCE.quantize(1234.45));
        Assert.assertEquals(1235, DoubleIntegerQuantizer.INSTANCE.quantize(1234.55));
        Assert.assertEquals(0, DoubleIntegerQuantizer.INSTANCE.quantize(0.0));
        Assert.assertEquals(-10, DoubleIntegerQuantizer.INSTANCE.quantize(-10.25));
        
        Assert.assertEquals(0, RoundingDoubleQuantizer.STEP_100.quantize(0.0));
        Assert.assertEquals(200, RoundingDoubleQuantizer.STEP_100.quantize(225.2));
        Assert.assertEquals(300, RoundingDoubleQuantizer.STEP_100.quantize(250.59));
        
        Assert.assertEquals(0, RoundingDoubleQuantizer.STEP_1000.quantize(1.25));
        Assert.assertEquals(0, RoundingDoubleQuantizer.STEP_1000.quantize(221.79));
        Assert.assertEquals(1000, RoundingDoubleQuantizer.STEP_1000.quantize(1304.79));
        Assert.assertEquals(2000, RoundingDoubleQuantizer.STEP_1000.quantize(1704.79));
    }

    /**
     * Tests the self-scaling double quantizer.
     */
    @Test
    public void testScalingDoubleQuantizer() {
        Assert.assertEquals(3, ScalingDoubleQuantizer.INSTANCE.quantize(3.2));
        Assert.assertEquals(30, ScalingDoubleQuantizer.INSTANCE.quantize(25.3));
        Assert.assertEquals(530, ScalingDoubleQuantizer.INSTANCE.quantize(525.78));
        Assert.assertEquals(1500, ScalingDoubleQuantizer.INSTANCE.quantize(1525.2));
        Assert.assertEquals(1600, ScalingDoubleQuantizer.INSTANCE.quantize(1625.3));        
    }

}
