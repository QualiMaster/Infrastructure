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

import static eu.qualimaster.monitoring.profiling.predictors.Utils.parseDouble;

/**
 * Tests the utility methods.
 * 
 * @author Holger Eichelberger
 */
public class UtilsTest {

    /**
     * Tests the parse double method converting back from math3 textual formatting.
     */
    @Test
    public void testParseDouble() {
        Assert.assertEquals(parseDouble("0"), 0.0, 0.005);
        Assert.assertEquals(parseDouble("0.1"), 0.1, 0.005);
        Assert.assertEquals(parseDouble("103,050.4"), 103050.4, 0.005);
        Assert.assertEquals(parseDouble("103.050.4"), 103050.4, 0.005);
        try {
            parseDouble("aaa");
            Assert.fail("No exception");
        } catch (NumberFormatException e) {
        }
    }
    
}
