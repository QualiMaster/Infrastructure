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
 * Implements a simple string data output class.
 * 
 * @author Holger Eichelberger
 */
public class StringDataOutput extends AbstractDataOutput {

    private StringBuilder buffer = new StringBuilder();
    private char separator;
    
    /**
     * Creates a string data output instance with given data separator.
     * 
     * @param separator the separator (must not occur in the data)
     */
    public StringDataOutput(char separator) {
        this.separator = separator;
    }
    
    /**
     * Returns the collected output.
     * 
     * @return the collected output
     */
    public String getOutput() {
        return buffer.toString();
    }
    
    @Override
    public String toString() {
        return buffer.toString();
    }
    
    @Override
    public void writeInt(int value) throws IOException {
        buffer.append(value);
        buffer.append(separator);
    }

    @Override
    public void writeLong(long value) throws IOException {
        buffer.append(value);
        buffer.append(separator);
    }

    @Override
    public void writeBoolean(boolean value) throws IOException {
        buffer.append(value);
        buffer.append(separator);
    }

    @Override
    public void writeDouble(double value) throws IOException {
        buffer.append(value);
        buffer.append(separator);
    }

    @Override
    public void writeFloat(float value) throws IOException {
        buffer.append(value);
        buffer.append(separator);
    }

    @Override
    public void writeShort(short value) throws IOException {
        buffer.append(value);
        buffer.append(separator);
    }

    @Override
    public void writeChar(char value) throws IOException {
        buffer.append(value);
        buffer.append(separator);
    }

    @Override
    public void writeString(String value) throws IOException {
        buffer.append(value);
        buffer.append(separator);
    }

    @Override
    public void writeByte(byte value) throws IOException {
        buffer.append(value);
        buffer.append(separator);
    }

}
