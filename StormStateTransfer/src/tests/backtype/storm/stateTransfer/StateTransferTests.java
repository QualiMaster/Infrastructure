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
package tests.backtype.storm.stateTransfer;

import java.lang.reflect.Field;

import org.junit.Assert;
import org.junit.Test;

import backtype.storm.stateTransfer.PartOfState;
import backtype.storm.stateTransfer.StateTransfer;
import backtype.storm.stateTransfer.StateTransferHandler;
import backtype.storm.stateTransfer.StateTransferHandlerRegistry;

/**
 * State transfer tests.
 * 
 * @author Holger Eichelberger
 */
public class StateTransferTests {

    /**
     * A specific type to be transferred.
     * 
     * @author Holger Eichelberger
     */
    private static class TestType {
    }

    /**
     * A "custom" handler for {@link TestType}.
     * 
     * @author Holger Eichelberger
     */
    private static class MyStateTransferHandler extends StateTransferHandler<TestType> {

        /**
         * Creates the handler.
         */
        protected MyStateTransferHandler() {
            super(TestType.class);
        }

        @Override
        public boolean doStateTransfer(PartOfState annotation, Field field, Object target, TestType oldValue,
            TestType newValue) throws SecurityException, IllegalArgumentException, IllegalAccessException {
            return doDefaultObjectStateTransfer(annotation, field, target, oldValue, newValue);
        }
        
    }
    
    /**
     * A test class for which the state shall be transferred.
     * 
     * @author Holger Eichelberger
     */
    private static class TestAlg {
        
        private TestType type;
        private int value;
        
    }

    /**
     * Tests registration and unregistration.
     * 
     * @throws NoSuchFieldException shall not occur
     * @throws SecurityException shall not occur
     */
    @Test
    public void registerTest() throws NoSuchFieldException, SecurityException {
        final Field typeField = TestAlg.class.getDeclaredField("type");
        final Field valueField = TestAlg.class.getDeclaredField("value");
        
        // default
        Assert.assertEquals(StateTransferHandlerRegistry.DEFAULT_OBJECT_HANDLER, 
            StateTransferHandlerRegistry.getHandler(null, typeField));
        Assert.assertEquals(StateTransferHandlerRegistry.DEFAULT_PRIMITIVE_HANDLER, 
            StateTransferHandlerRegistry.getHandler(null, valueField));
        
        // global
        MyStateTransferHandler handler = new MyStateTransferHandler();
        StateTransferHandlerRegistry.registerHandler(handler);
        Assert.assertEquals(handler, StateTransferHandlerRegistry.getHandler(null, typeField));
        Assert.assertEquals(StateTransferHandlerRegistry.DEFAULT_PRIMITIVE_HANDLER, 
            StateTransferHandlerRegistry.getHandler(null, valueField));
        StateTransferHandlerRegistry.unregisterHandler(handler);

        // back to default
        Assert.assertEquals(StateTransferHandlerRegistry.DEFAULT_OBJECT_HANDLER, 
            StateTransferHandlerRegistry.getHandler(null, typeField));
        Assert.assertEquals(StateTransferHandlerRegistry.DEFAULT_PRIMITIVE_HANDLER, 
            StateTransferHandlerRegistry.getHandler(null, valueField));

        // type specific
        StateTransferHandlerRegistry.registerHandler(TestAlg.class, handler);
        Assert.assertEquals(StateTransferHandlerRegistry.DEFAULT_OBJECT_HANDLER, 
            StateTransferHandlerRegistry.getHandler(null, typeField));
        Assert.assertEquals(StateTransferHandlerRegistry.DEFAULT_PRIMITIVE_HANDLER, 
            StateTransferHandlerRegistry.getHandler(null, valueField));
        Assert.assertEquals(handler, StateTransferHandlerRegistry.getHandler(TestAlg.class, typeField));
        Assert.assertEquals(StateTransferHandlerRegistry.DEFAULT_PRIMITIVE_HANDLER, 
            StateTransferHandlerRegistry.getHandler(null, valueField));
        StateTransferHandlerRegistry.unregisterHandler(TestAlg.class, handler);

        // back to default
        Assert.assertEquals(StateTransferHandlerRegistry.DEFAULT_OBJECT_HANDLER, 
            StateTransferHandlerRegistry.getHandler(null, typeField));
        Assert.assertEquals(StateTransferHandlerRegistry.DEFAULT_PRIMITIVE_HANDLER, 
            StateTransferHandlerRegistry.getHandler(null, valueField));
    }

    /**
     * Tests the state transfer functionality.
     * 
     * @throws IllegalAccessException shall not occur 
     * @throws IllegalArgumentException shall not occur 
     * @throws SecurityException shall not occur
     */
    @Test
    public void testStateTransfer() throws SecurityException, IllegalArgumentException, IllegalAccessException {
        Object src = new Object();
        Object tgt = new Object();
        StateTransfer.transferState(tgt, src);
        
        TestAlg src1 = new TestAlg();
        src1.value = 10;
        src1.type = new TestType();
        TestAlg tgt1 = new TestAlg();
        StateTransfer.transferState(tgt1, src1);
        Assert.assertEquals(src1.value, tgt1.value);
        Assert.assertEquals(src1.type, tgt1.type);
    }
    
}
