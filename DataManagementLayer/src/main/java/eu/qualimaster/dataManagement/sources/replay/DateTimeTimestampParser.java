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
import java.text.SimpleDateFormat;

import org.joda.time.DateTime;

/**
 * A timestamp parser for the (legacy) default timestamps.
 * 
 * @author Holger Eichelberger
 */
public class DateTimeTimestampParser implements ITimestampParser {

    public static final ITimestampParser INSTANCE = new DateTimeTimestampParser(0);
    private SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy' 'HH:mm:ss");
    private int offset;
    
    /**
     * Creates a parser instance.
     * 
     * @param offset start index of the time stamp where in a "," separated line
     */
    public DateTimeTimestampParser(int offset) {
        this.offset = Math.max(0, offset);
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
            // simplified ms timestamp would only require Character.isDigit(c)
            if ((Character.isDigit(c) || '/' == c || ':' == c || Character.isWhitespace(c) || ',' == c) 
                && commaCount <= offset + 1) {
                pos++;
            } else {
                break;
            }
        }
        return pos;
    }

    @Override
    public long parseTimestamp(String timestamp) throws ParseException {
        String ar[] = timestamp.split(",");
        String dateStr = ar[offset] + " " + ar[offset + 1];
        return new DateTime(sdf.parse(dateStr).getTime()).getMillis();
    }

    @Override
    public boolean skipParsing(String line) {
        return line.startsWith(" ");
    }
    
}
