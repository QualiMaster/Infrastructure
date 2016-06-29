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

/**
 * The interface for reading data from a general source, e.g., a test / profiling data source.
 *
 * @author Holger Eichelberger
 */
public interface IDataInput {

    /**
     * Reads the next int from data.
     * 
     * @return the next int
     * @throws IOException in case that reading fails for some reason
     */
    public int nextInt() throws IOException;

    /**
     * Reads the next long from data.
     * 
     * @return the next long
     * @throws IOException in case that reading fails for some reason
     */
    public long nextLong() throws IOException;

    /**
     * Reads the next boolean from data.
     * 
     * @return the next boolean
     * @throws IOException in case that reading fails for some reason
     */
    public boolean nextBoolean() throws IOException;

    /**
     * Reads the next double from data.
     * 
     * @return the next double
     * @throws IOException in case that reading fails for some reason
     */
    public double nextDouble() throws IOException;

    /**
     * Reads the next String from data.
     * 
     * @return the next String
     * @throws IOException in case that reading fails for some reason
     */
    public String nextString() throws IOException;

    /**
     * Reads the next char from data.
     * 
     * @return the next char
     * @throws IOException in case that reading fails for some reason
     */
    public char nextChar() throws IOException;

    /**
     * Reads the next float from data.
     * 
     * @return the next float
     * @throws IOException in case that reading fails for some reason
     */
    public float nextFloat() throws IOException;

    /**
     * Reads the next short from data.
     * 
     * @return the next short
     * @throws IOException in case that reading fails for some reason
     */
    public short nextShort() throws IOException;

    /**
     * Reads the next byte from data.
     * 
     * @return the next byte
     * @throws IOException in case that reading fails for some reason
     */
    public byte nextByte() throws IOException;

    /**
     * Reads the next long array from data.
     * 
     * @return the next long array
     * @throws IOException in case that reading fails for some reason
     */
    public long[] nextLongArray() throws IOException;
    
    /**
     * Reads the next int array from data.
     * 
     * @return the next int array
     * @throws IOException in case that reading fails for some reason
     */
    public int[] nextIntArray() throws IOException;

    /**
     * Reads the next boolean array from data.
     * 
     * @return the next boolean array
     * @throws IOException in case that reading fails for some reason
     */
    public boolean[] nextBooleanArray() throws IOException;

    /**
     * Reads the next double array from data.
     * 
     * @return the next double array
     * @throws IOException in case that reading fails for some reason
     */
    public double[] nextDoubleArray() throws IOException;

    /**
     * Reads the next String array from data.
     * 
     * @return the next String array
     * @throws IOException in case that reading fails for some reason
     */
    public String[] nextStringArray() throws IOException;

    /**
     * Reads the next char array from data.
     * 
     * @return the next char array
     * @throws IOException in case that reading fails for some reason
     */
    public char[] nextCharArray() throws IOException;

    /**
     * Reads the next float array from data.
     * 
     * @return the next float array
     * @throws IOException in case that reading fails for some reason
     */
    public float[] nextFloatArray() throws IOException;

    /**
     * Reads the next short array from data.
     * 
     * @return the next short array
     * @throws IOException in case that reading fails for some reason
     */
    public short[] nextShortArray() throws IOException;

    /**
     * Reads the next byte array from data.
     * 
     * @return the next byte array
     * @throws IOException in case that reading fails for some reason
     */
    public byte[] nextByteArray() throws IOException;

    
    /**
     * Returns whether we are at the end-of-data.
     * 
     * @return <code>true</code> for end-of-data, <code>false</code> else
     */
    public boolean isEOD();
    
}
