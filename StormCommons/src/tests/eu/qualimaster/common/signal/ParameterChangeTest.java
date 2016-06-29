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
package tests.eu.qualimaster.common.signal;

import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.common.signal.ParameterChange;
import eu.qualimaster.common.signal.ValueFormatException;

/**
 * Tests the parameter change class.
 * 
 * @author Holger Eichelberger
 */
public class ParameterChangeTest {
    
    /**
     * Tests creating / accessing a parameter change instance for string.
     */
    @Test
    public void testStringChange() {
        final String name = "name";
        final String value = "value";
        
        ParameterChange change = new ParameterChange(name, value);
        Assert.assertEquals(name, change.getName());
        Assert.assertEquals(value, change.getValue());
        Assert.assertEquals(value, change.getStringValue());
        try {
            Assert.assertEquals(false, change.getBooleanValue());
        } catch (ValueFormatException e) {
            Assert.fail("unexpected excpetion");
        }
        try {
            Assert.assertEquals(value, change.getIntValue());
            Assert.fail("excpetion missing");
        } catch (ValueFormatException e) {
            // ok
        }
        try {
            Assert.assertEquals(value, change.getDoubleValue());
            Assert.fail("excpetion missing");
        } catch (ValueFormatException e) {
            // ok
        }
    }
    
    /**
     * Tests creating / accessing a parameter change instance for boolean.
     */
    @Test
    public void testBooleanChange() {
        final String name = "name";
        final boolean value = true;
        
        ParameterChange change = new ParameterChange(name, value);
        Assert.assertEquals(name, change.getName());
        Assert.assertEquals(value, change.getValue());
        Assert.assertEquals(String.valueOf(value), change.getStringValue());
        try {
            Assert.assertEquals(value, change.getBooleanValue());
        } catch (ValueFormatException e) {
            Assert.fail("unexpected Exception");
        }
    }

    /**
     * Tests creating / accessing a parameter change instance for int.
     */
    @Test
    public void testIntChange() {
        final String name = "name";
        final int value = 10;
        
        ParameterChange change = new ParameterChange(name, value);
        Assert.assertEquals(name, change.getName());
        Assert.assertEquals(value, change.getValue());
        Assert.assertEquals(String.valueOf(value), change.getStringValue());
        try {
            Assert.assertEquals(value, change.getIntValue());
        } catch (ValueFormatException e) {
            Assert.fail("unexpected Exception");
        }
    }

    /**
     * Tests creating / accessing a parameter change instance for double.
     */
    @Test
    public void testDoubleChange() {
        final String name = "name";
        final double value = Math.PI;
        
        ParameterChange change = new ParameterChange(name, value);
        Assert.assertEquals(name, change.getName());
        Assert.assertEquals(value, change.getValue());
        Assert.assertEquals(String.valueOf(value), change.getStringValue());
        try {
            Assert.assertEquals(value, change.getDoubleValue(), 0.05);
        } catch (ValueFormatException e) {
            Assert.fail("unexpected Exception");
        }
    }
    
    /**
     * Tests creating / accessing a parameter change instance for double.
     */
    @Test
    public void testNullChange() {
        final String name = "name";
        final String value = null;
        
        ParameterChange change = new ParameterChange(name, value);
        Assert.assertEquals(name, change.getName());
        Assert.assertEquals(value, change.getValue());
        Assert.assertEquals(value, change.getStringValue());

        try {
            Assert.assertEquals(value, change.getBooleanValue());
            Assert.fail("excpetion missing");
        } catch (ValueFormatException e) {
            // ok
        }
        try {
            Assert.assertEquals(value, change.getIntValue());
            Assert.fail("excpetion missing");
        } catch (ValueFormatException e) {
            // ok
        }
        try {
            Assert.assertEquals(value, change.getDoubleValue());
            Assert.fail("excpetion missing");
        } catch (ValueFormatException e) {
            // ok
        }
    }

}
