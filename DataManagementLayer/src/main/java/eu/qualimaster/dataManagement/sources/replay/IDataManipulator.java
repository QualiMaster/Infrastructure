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

/**
 * Allows influencing the final data to be emitted.
 * 
 * @author Holger Eichelberger
 */
public interface IDataManipulator {

    /**
     * Changes the plain input line.
     * 
     * @param line the actual input line
     * @return the input line to process (may be <code>line</code>)
     */
    public String changeInput(String line, boolean firstLine);
    // return firstLine ? line : line.replace((char) 65533, (char) 183); may go here - see tests

    /**
     * Composes the final data in case that the output data of the replay mechanism shall, however, also contain
     * the timestamp.
     * 
     * @param timestamp the timestamp (as read from the replay source and adjusted to the replay time)
     * @param line the fill line (as read from the replay source)
     * @return the composed data (typically <code>payload</code>)
     */
    public String composeData(long timestamp, String line);
    // return ar[0] + "," + newDate[0] + "," + newDate[1] + "," + ar[3] + "," + ar[4]; may go here - see tests
    
}