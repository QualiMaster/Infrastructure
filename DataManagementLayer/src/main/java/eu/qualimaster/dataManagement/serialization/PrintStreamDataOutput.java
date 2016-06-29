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
import java.io.PrintStream;

/**
 * An data output class based on print streams and delimiter characters.
 * This class is intended to write output for {@link StringDataInput}.
 * 
 * @author Holger Eichelberger
 */
public class PrintStreamDataOutput extends AbstractDataOutput {

    private PrintStream out;
    private char delimiter;
    
    /**
     * Creates a data output instance based on print stream.
     *  
     * @param out the output stream
     * @param delimiter the delimiter
     */
    public PrintStreamDataOutput(PrintStream out, char delimiter) {
        this.out = out;
        this.delimiter = delimiter;
    }
    
    /**
     * Writes the delimiter.
     */
    private void writeDelimiter() {
        out.print(delimiter);
    }
    
    @Override
    public void writeInt(int value) throws IOException {
        out.print(value);
        writeDelimiter();
    }
    
    @Override
    public void writeLong(long value) throws IOException {
        out.print(value);
        writeDelimiter();
    }

    @Override
    public void writeBoolean(boolean value) throws IOException {
        out.print(value);
        writeDelimiter();
    }

    @Override
    public void writeDouble(double value) throws IOException {
        out.print(value);
        writeDelimiter();
    }

    @Override
    public void writeFloat(float value) throws IOException {
        out.print(value);
        writeDelimiter();
    }

    @Override
    public void writeShort(short value) throws IOException {
        out.print(value);
        writeDelimiter();
    }

    @Override
    public void writeChar(char value) throws IOException {
        out.print(value);
        writeDelimiter();
    }

    @Override
    public void writeString(String value) throws IOException {
        out.print(value);
        writeDelimiter();
    }

    @Override
    public void writeByte(byte value) throws IOException {
        out.print(value);
        writeDelimiter();
    }

}