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
 * An input data implementation reading data from a string with a given delimiter char.
 * Shall be able to read data written by {@link PrintStreamDataOutput}.
 * 
 * @author Holger Eichelberger
 */
public class StringDataInput extends AbstractDataInput {

    private int pos;
    private String data;
    private char delimiter;
    
    /**
     * Creates a String input data object.
     * 
     * @param data the data
     * @param delimiter the delimiter char
     */
    public StringDataInput(String data, char delimiter) {
        this.data = data;
        this.delimiter = delimiter;
        this.pos = 0;
    }

    @Override
    protected String next() throws IOException {
        int nextPos = pos;
        while (nextPos < data.length() && delimiter != data.charAt(nextPos)) {
            nextPos++;
        }
        if (pos >= data.length()) {
            throw new IOException("EOD reached");
        }
        String result;
        if (pos == nextPos) {
            result = "";
        } else {
            result = data.substring(pos, nextPos);
        }
        pos = nextPos + 1;
        return result;
    }
    
    /**
     * Returns the rest and advances to end.
     * 
     * @return the rest
     * @throws IOException in case of EOD
     */
    protected String rest() throws IOException {
        String result;
        if (pos < data.length()) {
            result = data.substring(pos);
            pos = data.length() + 1;
        } else {
            throw new IOException("EOD reached");
        }
        return result;
    }
    
    /**
     * Returns whether we are at the beginning of data.
     * 
     * @return <code>true</code> for beginning of data, <code>false</code> else
     */
    public boolean isBOD() {
        return pos == 0;
    }

    @Override
    public boolean isEOD() {
        return pos >= data.length();
    }
    
}
