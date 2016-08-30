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
     * A test class for which the state shall be transferred. Don't remove the fields!
     * 
     * @author Holger Eichelberger
     */
    private static class TestAlg {
        
        @SuppressWarnings("unused")
        private TestType type;
        @SuppressWarnings("unused")
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
     */
    @Test
    public void testStateTransfer() {
    }
    
}
