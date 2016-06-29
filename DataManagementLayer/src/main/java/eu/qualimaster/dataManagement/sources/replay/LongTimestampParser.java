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
package eu.qualimaster.dataManagement.sources.replay;

import java.text.ParseException;

/**
 * A timestamp parser for timestams given as (usual) long ms values.
 * 
 * @author Holger Eichelberger
 */
public class LongTimestampParser implements ITimestampParser {

    public static final ITimestampParser INSTANCE = new LongTimestampParser();
    
    /**
     * Creates a parser instance.
     */
    private LongTimestampParser() {
    }
    
    @Override
    public int consumeTimestamp(String line) {
        int pos = 0;
        int commaCount = 0;
        while (pos < line.length()) {
            char c = line.charAt(pos);
            if (',' == c) {
                commaCount++;
            }
            if ((Character.isDigit(c)) 
                && commaCount <=1) {
                pos++;
            } else {
                break;
            }
        }
        return pos;
    }

    @Override
    public long parseTimestamp(String timestamp) throws ParseException {
        try {
            return Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            throw new ParseException(e.getMessage(), 0);
        }
    }
    
    @Override
    public boolean skipParsing(String line) {
        return false;
    }

}
