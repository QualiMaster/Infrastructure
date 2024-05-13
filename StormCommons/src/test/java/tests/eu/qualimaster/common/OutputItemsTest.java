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
package tests.eu.qualimaster.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;

import org.junit.Assert;
import org.junit.Test;
import org.junit.Ignore;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import eu.qualimaster.base.algorithm.AbstractOutputItem;
import eu.qualimaster.base.algorithm.IDirectGroupingInfo;
import eu.qualimaster.base.algorithm.IOutputItem;
import eu.qualimaster.base.algorithm.IOutputItemIterator;

/**
 * Tests the enhanced output items.
 * 
 * @author Holger Eichelberger
 */
public class OutputItemsTest {

    /**
     * The family interface for testing.
     * 
     * @author Holger Eichelberger
     */
    private interface IMyFamilyOutput extends Serializable, IOutputItem<IMyFamilyOutput>, IDirectGroupingInfo {
        
        /**
         * Returns the value.
         * 
         * @return the value
         */
        public int getValue();
        
        /**
         * Changes the value.
         * 
         * @param value the new value
         */
        public void setValue(int value);
        
    }
    
    /**
     * Implements an output item as the generation shall derive it.
     * 
     * @author Holger Eichelberger
     */
    @SuppressWarnings("serial")
    private static class OutputItem extends AbstractOutputItem<IMyFamilyOutput> implements IMyFamilyOutput {

        private int value;
        private int taskId;
        
        /**
         * Creates the item.
         */
        public OutputItem() {
            this(true);
        }

        /**
         * Creates a sub-item.
         * 
         * @param topLevel whether it is a top-level item
         */
        private OutputItem(boolean topLevel) {
            super(topLevel);
            setParent(this);
        }
        
        @Override
        public OutputItem createItem() {
            return new OutputItem(false);
        }

        @Override
        public int getValue() {
            return value;
        }

        @Override
        public void setValue(int value) {
            this.value = value;
        }
        
        @Override
        public boolean equals(Object object) {
            boolean result = false;
            if (object instanceof OutputItem) {
                OutputItem item = (OutputItem) object;
                result = item.value == this.value;
            }
            return result;
        }
        
        @Override
        public int hashCode() {
            return value;
        }
        
        @Override
        public void setTaskId(int taskId) {
            this.taskId = taskId;
        }

        @Override
        public int getTaskId() {
            return taskId;
        }
        
    }
    
    /**
     * Tests the item instances.
     */
    @Test
    @Ignore // JDK 17
    public void testItems() {
        OutputItem item = new OutputItem();
        
        item.setValue(5);
        assertProcessing(item, 5);
        assertProcessing(item, 5); // reset is included
        
        item.clear();
        item.setValue(6);
        assertProcessing(item, 6);
        assertProcessing(item, 6); // reset is included
        
        item.clear();
        item.noOutput();
        assertProcessing(item);
        assertProcessing(item); // reset is included
        
        item.clear();
        item.setValue(7);
        IMyFamilyOutput further = item.addFurther();
        further.setValue(8);
        further = item.addFurther();
        further.setValue(9);
        
        assertProcessing(item, 7, 8, 9);
        assertProcessing(item, 7, 8, 9); // reset is included
    }
    
    /**
     * Assert the processing akin to the generated bolts.
     *  
     * @param item the item to be assured
     * @param expected the expected result due to the iteration
     */
    private void assertProcessing(OutputItem item, int... expected) {
        int pos = 0;
        IOutputItemIterator<IMyFamilyOutput> iter = item.iterator();
        iter.reset();
        while (iter.hasNext()) {
            IMyFamilyOutput i = iter.next();
            Assert.assertTrue(pos < expected.length);
            Assert.assertEquals(expected[pos], i.getValue());
            if (pos > 0) { // no sub-iterators
                Assert.assertTrue(i.iterator().hasNext());
                Assert.assertEquals(i, i.iterator().next());
                Assert.assertNull(i.iterator().next());
                i.iterator().reset();
            }
            pos++;
        }
        Assert.assertNull(iter.next());
        Assert.assertEquals(expected.length, item.count());
    }
    
    /**
     * Tests whether individual instances are serializable.
     */
    @Test
    @Ignore // JDK 17
    public void testSerialization() {
        IMyFamilyOutput item = new OutputItem();
        item.setValue(5);
        Kryo k = StormTestUtils.createStormKryo();
        assertSerializable(k, item);
        
        IMyFamilyOutput further = item.addFurther();
        further.setValue(6);
        
        IOutputItemIterator<IMyFamilyOutput> iter = item.iterator();
        while (iter.hasNext()) {
            IMyFamilyOutput elt = iter.next();
            assertSerializable(k, elt);
        }
    }
    
    /**
     * Asserts that <code>item</code> is serializable with the given Kryo.
     * 
     * @param kryo the Kryo instance 
     * @param item the item to be serialized / deserialized
     */
    private void assertSerializable(Kryo kryo, IMyFamilyOutput item) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Output out = new Output(buffer);
        kryo.writeObject(out, item);
        out.close();
        
        byte[] ser = buffer.toByteArray();
        
        Input in = new Input(new ByteArrayInputStream(ser));
        OutputItem test = kryo.readObject(in, OutputItem.class);
        in.close();
        Assert.assertEquals(item, test);
    }
    
}
