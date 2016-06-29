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
package tests.eu.qualimaster.dataManagement;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Test;
import org.junit.Assert;

import eu.qualimaster.dataManagement.serialization.IDataInput;
import eu.qualimaster.dataManagement.serialization.IDataOutput;
import eu.qualimaster.dataManagement.serialization.ISerializer;
import eu.qualimaster.dataManagement.serialization.SerializerRegistry;
import eu.qualimaster.dataManagement.serialization.StringDataInput;
import eu.qualimaster.dataManagement.serialization.StringDataOutput;

/**
 * Tests the string serialization/deserialization.
 * 
 * @author Holger Eichelberger
 */
public class StringSerializationTests {

    /**
     * A test data class to serialize/deserialize.
     * 
     * @author Holger Eichelberger
     */
    private static class DataClass {
        private int intData;
        private float floatData;
        private double doubleData;
        private char charData;
        private long longData;
        private short shortData;
        private String stringData;
        private boolean booleanData;
        private byte byteData;
        private int[] intDataArray;
        private double[] doubleDataArray;
        private float[] floatDataArray;
        private char[] charDataArray;
        private long[] longDataArray;
        private short[] shortDataArray;
        private String[] stringDataArray;
        private boolean[] booleanDataArray;
        private byte[] byteDataArray;
        
        /**
         * Creates an instance.
         */
        private DataClass() {
        }
        
        /**
         * Fills the instance with test data.
         */
        private void fillTestData() {
            intData = 1;
            doubleData = 5.34;
            floatData = 5.34f;
            charData = 7;
            longData = 3876717L;
            shortData = 234;
            stringData = "aba";
            booleanData = false;
            byteData = 122;
            
            intDataArray = new int[]{1, 2, 3};
            doubleDataArray = new double[]{1.2, 2.3, 3.4};
            floatDataArray = new float[]{0.2f, 1.3f, 2.4f};
            charDataArray = new char[]{8, 9, 22};
            longDataArray = new long[]{1296739L, 3678787687L, 3868127L};
            shortDataArray = new short[]{};
            stringDataArray = new String[] {"QM", "QM", "Q M", ""};
            booleanDataArray = new boolean[] {true, false, true};
            byteDataArray = new byte[] {22, -3, 127, 0};

            // TODO null - not important by now
        }
        
        /**
         * Asserts that this instance has the same content than <code>other</code>. 
         * 
         * @param other the other instance
         */
        private void assertEquals(DataClass other) {
            Assert.assertNotNull(other);
            Assert.assertEquals(intData, other.intData);
            Assert.assertEquals(doubleData, other.doubleData, 0.0005);
            Assert.assertEquals(floatData, other.floatData, 0.005f);
            Assert.assertEquals(charData, other.charData);
            Assert.assertEquals(longData, other.longData);
            Assert.assertEquals(shortData, other.shortData);
            Assert.assertEquals(stringData, other.stringData);
            Assert.assertEquals(booleanData, other.booleanData);
            Assert.assertEquals(byteData, other.byteData);
            
            Assert.assertArrayEquals(intDataArray, other.intDataArray);
            Assert.assertArrayEquals(doubleDataArray, other.doubleDataArray, 0.0005);
            Assert.assertArrayEquals(floatDataArray, other.floatDataArray, 0.005f);
            Assert.assertArrayEquals(charDataArray, other.charDataArray);
            Assert.assertArrayEquals(longDataArray, other.longDataArray);
            Assert.assertArrayEquals(shortDataArray, other.shortDataArray);
            Assert.assertArrayEquals(stringDataArray, other.stringDataArray);
            Assert.assertArrayEquals(booleanDataArray, other.booleanDataArray);
            Assert.assertArrayEquals(byteDataArray, other.byteDataArray);
        }
        
    }
    
    /**
     * A test serializer class for {@link DataClass}.
     * 
     * @author Holger Eichelberger
     */
    public static class DataClassSerializer implements ISerializer<DataClass> {

        @Override
        public void serializeTo(DataClass object, OutputStream out) throws IOException {
            // not relevant here
        }

        @Override
        public DataClass deserializeFrom(InputStream in) throws IOException {
            return null; // not relevant here
        }

        @Override
        public void serializeTo(DataClass object, IDataOutput out) throws IOException {
            out.writeInt(object.intData);
            out.writeDouble(object.doubleData);
            out.writeFloat(object.floatData);
            out.writeChar(object.charData);
            out.writeLong(object.longData);
            out.writeShort(object.shortData);
            out.writeString(object.stringData);
            out.writeBoolean(object.booleanData);
            out.writeByte(object.byteData);

            out.writeIntArray(object.intDataArray);
            out.writeDoubleArray(object.doubleDataArray);
            out.writeFloatArray(object.floatDataArray);
            out.writeCharArray(object.charDataArray);
            out.writeLongArray(object.longDataArray);
            out.writeShortArray(object.shortDataArray);
            out.writeStringArray(object.stringDataArray);
            out.writeBooleanArray(object.booleanDataArray);
            out.writeByteArray(object.byteDataArray);
        }

        @Override
        public DataClass deserializeFrom(IDataInput in) throws IOException {
            DataClass result = new DataClass();
            
            result.intData = in.nextInt();
            result.doubleData = in.nextDouble();
            result.floatData = in.nextFloat();
            result.charData = in.nextChar();
            result.longData = in.nextLong();
            result.shortData = in.nextShort();
            result.stringData = in.nextString();
            result.booleanData = in.nextBoolean();
            result.byteData = in.nextByte();

            result.intDataArray = in.nextIntArray();
            result.doubleDataArray = in.nextDoubleArray();
            result.floatDataArray = in.nextFloatArray();
            result.charDataArray = in.nextCharArray();
            result.longDataArray = in.nextLongArray();
            result.shortDataArray = in.nextShortArray();
            result.stringDataArray = in.nextStringArray();
            result.booleanDataArray = in.nextBooleanArray();
            result.byteDataArray = in.nextByteArray();

            return result;
        }
        
    }
    
    /**
     * Tests serialization and deserialization with simple strings.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testSerialization() throws IOException {
        // prepare
        Assert.assertTrue(SerializerRegistry.register(DataClass.class, DataClassSerializer.class));
        final char separator = ',';
        DataClass expected = new DataClass();
        expected.fillTestData();
        
        // serialize
        StringDataOutput out = new StringDataOutput(separator);
        ISerializer<DataClass> serializer = SerializerRegistry.getSerializer(DataClass.class);
        Assert.assertNotNull(serializer);
        serializer.serializeTo(expected, out);
        String serialized = out.getOutput();
        Assert.assertNotNull(serialized);

        // deserialize
        StringDataInput in = new StringDataInput(serialized, separator);
        DataClass actual = serializer.deserializeFrom(in);
        Assert.assertNotNull(actual);
        
        // assert results
        expected.assertEquals(actual);
        
        // check cleanup
        SerializerRegistry.unregister(DataClass.class);
        serializer = SerializerRegistry.getSerializer(DataClass.class);
        Assert.assertNull(serializer);
    }
    
}
