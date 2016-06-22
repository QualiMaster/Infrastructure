/*
 * Copyright 2009-2015 University of Hildesheim, Software Systems Engineering
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
package tests.eu.qualimaster;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.pipeline.AlgorithmChangeParameter;

/**
 * Tests {@link AlgorithmChangeParameter}.
 * 
 * @author Holger Eichelberger
 */
public class AlgorithmChangeParameterTest {

    /**
     * Tests the convert method.
     */
    @Test
    public void testConvert() {
        Map<String, Serializable> data = new HashMap<String, Serializable>();
        Map<AlgorithmChangeParameter, Serializable> result = AlgorithmChangeParameter.convert(data);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
        
        data.put(AlgorithmChangeParameter.INPUT_PORT.name(), 10);
        data.put(AlgorithmChangeParameter.OUTPUT_PORT.name(), 20);
        AlgorithmChangeParameter.setIntParameter(data, AlgorithmChangeParameter.CONTROL_REQUEST_PORT, 1024);

        Assert.assertNull(
            AlgorithmChangeParameter.getStringParameter(data, AlgorithmChangeParameter.COPROCESSOR_HOST, null));
        Assert.assertEquals("test", 
            AlgorithmChangeParameter.getStringParameter(data, AlgorithmChangeParameter.COPROCESSOR_HOST, "test"));
        try {
            AlgorithmChangeParameter.setIntParameter(data, AlgorithmChangeParameter.COPROCESSOR_HOST, 1024);
        } catch (IllegalArgumentException e) {
            Assert.fail("Implicit conversion, shall not lead to an exception");
        }
        AlgorithmChangeParameter.setStringParameter(data, AlgorithmChangeParameter.COPROCESSOR_HOST, "localhost");
        
        try {
            AlgorithmChangeParameter.setStringParameter(data, AlgorithmChangeParameter.CONTROL_REQUEST_PORT, "1024");
            Assert.fail("shall lead to an exception");
        } catch (IllegalArgumentException e) {
        }
        
        Assert.assertNull(
            AlgorithmChangeParameter.getIntParameter(data, AlgorithmChangeParameter.WARMUP_DELAY, null));
        Assert.assertEquals(10, 
            AlgorithmChangeParameter.getIntParameter(data, AlgorithmChangeParameter.WARMUP_DELAY, 10).intValue());
        Assert.assertNotNull(
            AlgorithmChangeParameter.getStringParameter(data, AlgorithmChangeParameter.COPROCESSOR_HOST, null));
        Assert.assertEquals("localhost", 
            AlgorithmChangeParameter.getStringParameter(data, AlgorithmChangeParameter.COPROCESSOR_HOST, null));
        Assert.assertNotNull(
            AlgorithmChangeParameter.getIntParameter(data, AlgorithmChangeParameter.CONTROL_REQUEST_PORT, null));
        Assert.assertEquals(1024, AlgorithmChangeParameter.getIntParameter(
            data, AlgorithmChangeParameter.CONTROL_REQUEST_PORT, null).intValue());
        
        Assert.assertEquals(Integer.class, AlgorithmChangeParameter.INPUT_PORT.getType());
        Assert.assertEquals(String.class, AlgorithmChangeParameter.COPROCESSOR_HOST.getType());
        
        result = AlgorithmChangeParameter.convert(data);
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        Assert.assertNotNull(result.get(AlgorithmChangeParameter.INPUT_PORT));
        Assert.assertEquals(10, ((Integer) result.get(AlgorithmChangeParameter.INPUT_PORT)).intValue());
        Assert.assertNotNull(result.get(AlgorithmChangeParameter.OUTPUT_PORT));
        Assert.assertEquals(20, ((Integer) result.get(AlgorithmChangeParameter.OUTPUT_PORT)).intValue());
        
        data.put("hallo", 30);
        try {
            result = AlgorithmChangeParameter.convert(data);
            Assert.fail("no exception thrown");
        } catch (IllegalArgumentException e) {
            // this is ok
        }
    }

    /**
     * Tests the set method.
     */
    @Test
    public void testSet() {
        Map<String, Serializable> data = new HashMap<String, Serializable>();
        AlgorithmChangeParameter.COPROCESSOR_HOST.setParameterValue(data, "HERE");
        Assert.assertEquals("HERE", AlgorithmChangeParameter.getStringParameter(
            data, AlgorithmChangeParameter.COPROCESSOR_HOST, ""));

        AlgorithmChangeParameter.INPUT_PORT.setParameterValue(data, 25);
        Assert.assertEquals(25, AlgorithmChangeParameter.getIntParameter(
            data, AlgorithmChangeParameter.INPUT_PORT, 0).intValue());

        try {
            AlgorithmChangeParameter.INPUT_PORT.setParameterValue(data, "aaa");
            Assert.fail("No exception thrown.");
        } catch (IllegalArgumentException e) {
        }
    }
    
}
