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
package eu.qualimaster.monitoring.profiling;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * Represents the map file for a certain parameter point.
 * 
 * @author Holger Eichelberger
 */
public class MapFile {

    private File file;
    private Properties mapData = new Properties(); // might not be ideal, but shall do the job

    /**
     * Creates an empty map file with target storage folder.
     * 
     * @param folder the storage folder
     */
    public MapFile(File folder) {
        this.file = new File(folder, "_map");
    }
    
    /**
     * Loads the mappings from a file.
     * 
     * @throws IOException in case of I/O problems
     */
    public void load() throws IOException {
        Utils.load(file, mapData);
    }
    
    /**
     * Writes the mappings to <code>file</code>.
     * 
     * @throws IOException in case of I/O problems
     */
    public void store() throws IOException {
        Utils.store(file, mapData);
    }
    
    /**
     * Adds the mapping form an identifier to its file id.
     * 
     * @param identifier the identifier
     * @param id the id
     */
    public void put(String identifier, int id) {
        mapData.put(identifier, Integer.valueOf(id));
    }
    
    /**
     * Removes a specific mapping.
     * 
     * @param identifier the identifier to remove
     */
    public void remove(String identifier) {
        mapData.remove(identifier); 
    }
    
    /**
     * Returns the amount of entries.
     * 
     * @return the amount of entries
     */
    public int size() {
        return mapData.size();
    }

    /**
     * Finds an entry in the map file.
     * 
     * @param identifier the identifier to search for
     * @return the id or <code>-1</code> if not found
     */
    public int get(String identifier) {
        int result = -1;
        String tmp = mapData.getProperty(identifier);
        if (null != tmp) {
            try {
                result = Integer.parseInt(tmp);
            } catch (NumberFormatException e) {
                // ignore, result = -1
            }
        }
        return result;
    }

}
