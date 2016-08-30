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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import backtype.storm.stateTransfer.PartOfState;
import backtype.storm.stateTransfer.StateHandlingStrategy;
import backtype.storm.stateTransfer.StateTransfer;
import backtype.storm.stateTransfer.StateTransferHandler;
import backtype.storm.stateTransfer.StateTransferHandlerRegistry;
import backtype.storm.stateTransfer.Stateful;

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
            TestType newValue) throws SecurityException, IllegalArgumentException, IllegalAccessException, 
            InstantiationException {
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
        private List<Integer> intList;
        private Set<Integer> intSet;
        private Map<String, Integer> map;
        private transient int value1 = 10;
    }

    /**
     * A second test class for testing annotations.
     * 
     * @author Holger Eichelberger
     */
    @Stateful(considerAll = false)
    private static class TestObj {
        
        @PartOfState(strategy = StateHandlingStrategy.CLEAR_AND_FILL)
        private List<Integer> intListCaF = new ArrayList<Integer>();

        @PartOfState(strategy = StateHandlingStrategy.MERGE)
        private List<Integer> intListMerge = new ArrayList<Integer>();

        @PartOfState(strategy = StateHandlingStrategy.MERGE_AND_KEEP_OLD)
        private List<Integer> intListMergeKeep = new ArrayList<Integer>();

        @PartOfState(strategy = StateHandlingStrategy.CLEAR_AND_FILL)
        private Set<Integer> intSetCaF = new HashSet<Integer>();

        @PartOfState(strategy = StateHandlingStrategy.MERGE)
        private Set<Integer> intSetMerge = new HashSet<Integer>();

        @PartOfState(strategy = StateHandlingStrategy.MERGE_AND_KEEP_OLD)
        private Set<Integer> intSetMergeKeep = new HashSet<Integer>();

        @PartOfState(strategy = StateHandlingStrategy.CLEAR_AND_FILL)
        private Map<String, Integer> intMapCaF = new HashMap<String, Integer>();

        @PartOfState(strategy = StateHandlingStrategy.MERGE)
        private Map<String, Integer> intMapMerge = new HashMap<String, Integer>();

        @PartOfState(strategy = StateHandlingStrategy.MERGE_AND_KEEP_OLD)
        private Map<String, Integer> intMapMergeKeep = new HashMap<String, Integer>();

        private int value = 1;
        private transient int value1 = 10;

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
     * Tests the state transfer functionality (Object).
     * 
     * @throws IllegalAccessException shall not occur 
     * @throws IllegalArgumentException shall not occur 
     * @throws SecurityException shall not occur
     * @throws InstantiationException shall not occur
     */
    @Test
    public void testStateTransferObject() throws SecurityException, IllegalArgumentException, IllegalAccessException, 
        InstantiationException {
        Object src = new Object();
        Object tgt = new Object();
        StateTransfer.transferState(tgt, src);
    }

    /**
     * Tests the state transfer functionality (no annotations).
     * 
     * @throws IllegalAccessException shall not occur 
     * @throws IllegalArgumentException shall not occur 
     * @throws SecurityException shall not occur
     * @throws InstantiationException shall not occur
     */
    @Test
    public void testStateTransferTestAlg() throws SecurityException, IllegalArgumentException, IllegalAccessException, 
        InstantiationException {
        TestAlg src = new TestAlg();
        src.value = 10;
        src.type = new TestType();
        src.intList = new ArrayList<Integer>();
        fill(src.intList, 1, 2, 3, 4, 7);
        src.intSet = new HashSet<Integer>();
        fill(src.intSet, 8, 7, 6, 1);
        src.map = new HashMap<String, Integer>();
        src.map.put("aaa", 1234);
        src.value1 = 20;

        TestAlg tgt = new TestAlg();

        StateTransfer.transferState(tgt, src);
        Assert.assertEquals(src.value, tgt.value);
        Assert.assertEquals(src.type, tgt.type);
        Assert.assertEquals(src.intList, tgt.intList);
        Assert.assertEquals(src.intSet, tgt.intSet);
        Assert.assertEquals(src.map, tgt.map);
        Assert.assertEquals(10, tgt.value1); // transient, keep default
    }

    /**
     * Tests the state transfer functionality (annotations).
     * 
     * @throws IllegalAccessException shall not occur 
     * @throws IllegalArgumentException shall not occur 
     * @throws SecurityException shall not occur
     * @throws InstantiationException shall not occur
     */
    @Test
    public void testStateTransferTestObj() throws SecurityException, IllegalArgumentException, IllegalAccessException, 
        InstantiationException {
        TestObj src = new TestObj();
        fill(src.intListCaF, 1, 2, 3, 4);
        fill(src.intListMerge, 1, 2, 3, 4);
        fill(src.intListMergeKeep, 1, 2, 3, 4);
        fill(src.intSetCaF, 10, 20, 30, 40);
        fill(src.intSetMerge, 10, 20, 30, 40);
        fill(src.intSetMergeKeep, 10, 20, 30, 40);
        src.value = 10;
        src.value1 = 0;
        
        TestObj tgt = new TestObj();
        fill(tgt.intListCaF, 7, 8, 9, 0);
        fill(tgt.intListMerge, 4, 7, 8, 9, 0);
        fill(tgt.intListMergeKeep, 1, 2, 7, 8, 9, 0);
        fill(tgt.intSetCaF, 70, 80, 90, 100);
        fill(tgt.intSetMerge, 40, 70, 80, 90, 100);
        fill(tgt.intSetMergeKeep, 10, 20, 70, 80, 90, 100);
        
        Assert.assertEquals(1, tgt.value); // @Stateful, consider only marked attributes, default value
        Assert.assertEquals(10, tgt.value1); // transient, default value
        
        StateTransfer.transferState(tgt, src);
        ArrayList<Integer> cmpList = new ArrayList<Integer>();
        fill(cmpList, 1, 2, 3, 4);
        Assert.assertEquals(cmpList, tgt.intListCaF);
        fill(cmpList, 4, 7, 8, 9, 0, 1, 2, 3, 4);
        Assert.assertEquals(cmpList, tgt.intListMerge);
        fill(cmpList, 1, 2, 7, 8, 9, 0, 3, 4);
        Assert.assertEquals(cmpList, tgt.intListMergeKeep);

        Set<Integer> cmpSet = new HashSet<Integer>();
        fill(cmpSet, 10, 20, 30, 40);
        Assert.assertEquals(cmpSet, tgt.intSetCaF);
        fill(cmpSet, 70, 80, 90, 100, 10, 20, 30, 40);
        Assert.assertEquals(cmpSet, tgt.intSetMerge);
        fill(cmpSet, 10, 20, 70, 80, 90, 100, 30, 40);
        Assert.assertEquals(cmpSet, tgt.intSetMergeKeep);
    }
    
    /**
     * Fills <code>coll</code> with <code>values</code>.
     * 
     * @param <T> the entity type
     * @param coll the collection to fill
     * @param values the values
     */
    @SuppressWarnings("unchecked")
    private static <T> void fill(Collection<T> coll, T... values) {
        coll.clear();
        for (T v: values) {
            coll.add(v);
        }
    }
    
}
