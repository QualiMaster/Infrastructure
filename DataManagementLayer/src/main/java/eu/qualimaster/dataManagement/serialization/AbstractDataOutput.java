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
package eu.qualimaster.dataManagement.serialization;

import java.io.IOException;

public abstract class AbstractDataOutput implements IDataOutput {

    @Override
    public void writeIntArray(int[] array) throws IOException {
        writeInt(array.length);
        for (int i = 0; i < array.length; i++) {
            writeInt(array[i]);
        }
    }

    @Override
    public void writeLongArray(long[] array) throws IOException {
        writeInt(array.length);
        for (int i = 0; i < array.length; i++) {
            writeLong(array[i]);
        }
    }

    @Override
    public void writeBooleanArray(boolean[] array) throws IOException {
        writeInt(array.length);
        for (int i = 0; i < array.length; i++) {
            writeBoolean(array[i]);
        }
    }

    @Override
    public void writeDoubleArray(double[] array) throws IOException {
        writeInt(array.length);
        for (int i = 0; i < array.length; i++) {
            writeDouble(array[i]);
        }
    }

    @Override
    public void writeFloatArray(float[] array) throws IOException {
        writeInt(array.length);
        for (int i = 0; i < array.length; i++) {
            writeFloat(array[i]);
        }
    }

    @Override
    public void writeShortArray(short[] array) throws IOException {
        writeInt(array.length);
        for (int i = 0; i < array.length; i++) {
            writeShort(array[i]);
        }
    }

    @Override
    public void writeCharArray(char[] array) throws IOException {
        writeInt(array.length);
        for (int i = 0; i < array.length; i++) {
            writeChar(array[i]);
        }
    }

    @Override
    public void writeStringArray(String[] array) throws IOException {
        writeInt(array.length);
        for (int i = 0; i < array.length; i++) {
            writeString(array[i]);
        }
    }

    @Override
    public void writeByteArray(byte[] array) throws IOException {
        writeInt(array.length);
        for (int i = 0; i < array.length; i++) {
            writeByte(array[i]);
        }
    }
    
}
