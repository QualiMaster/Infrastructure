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

public interface IDataOutput {

    /**
     * Writes an integer value.
     * 
     * @param value the value
     * @throws IOException if the value cannot be written
     */
    public void writeInt(int value) throws IOException;

    /**
     * Writes a long value.
     * 
     * @param value the value
     * @throws IOException if the value cannot be written
     */
    public void writeLong(long value) throws IOException;

    /**
     * Writes a boolean value.
     * 
     * @param value the value
     * @throws IOException if the value cannot be written
     */
    public void writeBoolean(boolean value) throws IOException;

    /**
     * Writes a double value.
     * 
     * @param value the value
     * @throws IOException if the value cannot be written
     */
    public void writeDouble(double value) throws IOException;

    /**
     * Writes a float value.
     * 
     * @param value the value
     * @throws IOException if the value cannot be written
     */
    public void writeFloat(float value) throws IOException;

    /**
     * Writes a short value.
     * 
     * @param value the value
     * @throws IOException if the value cannot be written
     */
    public void writeShort(short value) throws IOException;

    /**
     * Writes a char value.
     * 
     * @param value the value
     * @throws IOException if the value cannot be written
     */
    public void writeChar(char value) throws IOException;

    /**
     * Writes a String value.
     * 
     * @param value the value
     * @throws IOException if the value cannot be written
     */
    public void writeString(String value) throws IOException;

    /**
     * Writes a Byte value.
     * 
     * @param value the value
     * @throws IOException if the value cannot be written
     */
    public void writeByte(byte value) throws IOException;

    /**
     * Writes a long array.
     * 
     * @param array the array
     * @throws IOException if the value cannot be written
     */
    public void writeLongArray(long[] array) throws IOException;
    
    /**
     * Writes an integer array.
     * 
     * @param array the array
     * @throws IOException if the value cannot be written
     */
    public void writeIntArray(int[] array) throws IOException;

    /**
     * Writes a boolean array.
     * 
     * @param array the array
     * @throws IOException if the value cannot be written
     */
    public void writeBooleanArray(boolean[] array) throws IOException;

    /**
     * Writes a double array.
     * 
     * @param array the array
     * @throws IOException if the value cannot be written
     */
    public void writeDoubleArray(double[] array) throws IOException;

    /**
     * Writes a float array.
     * 
     * @param array the array
     * @throws IOException if the value cannot be written
     */
    public void writeFloatArray(float[] array) throws IOException;

    /**
     * Writes a short array.
     * 
     * @param array the array
     * @throws IOException if the value cannot be written
     */
    public void writeShortArray(short[] array) throws IOException;

    /**
     * Writes a char array.
     * 
     * @param array the array
     * @throws IOException if the value cannot be written
     */
    public void writeCharArray(char[] array) throws IOException;

    /**
     * Writes a String array.
     * 
     * @param array the array
     * @throws IOException if the value cannot be written
     */
    public void writeStringArray(String[] array) throws IOException;

    /**
     * Writes a byte array.
     * 
     * @param array the array
     * @throws IOException if the value cannot be written
     */
    public void writeByteArray(byte[] array) throws IOException;

}
