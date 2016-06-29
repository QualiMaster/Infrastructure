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
 * Parses a timestamp in a replay data source.
 * 
 * @author Holger Eichelberger
 */
public interface ITimestampParser {
    
    /**
     * Consumes <code>line</code> from the beginning until the end of the timestamp.
     * 
     * @param line the line to be consumed
     * @return the position in line denoting the end of the timestamp, <code>0</code> if no timestamp was found
     */
    public int consumeTimestamp(String line);

    /**
     * Returns the timestamp.
     * 
     * @param timestamp the timestamp indicated by {@link #consumeTimestamp(String)} as a String
     * @return the timestamp as a long
     * @throws ParseException in case that parsing fails
     */
    public long parseTimestamp(String timestamp) throws ParseException;
    
    /**
     * Returns whether parsing of the given <code>line</code> shall be skipped before looking into the timestamp itself.
     * 
     * @param line the line
     * @return <code>true</code> for skip, <code>false</code> else
     */
    public boolean skipParsing(String line);
    
}