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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * A plain file system source.
 * 
 * @author Holger Eichelberger
 */
public class FileSource implements IReplaySource {

    private File fileForData;

    /**
     * Creates a file system source.
     * 
     * @param pathToData the path to the data
     */
    public FileSource(String pathToData) {
        fileForData = new File(pathToData);
    }

    /**
     * Creates a file system source.
     * 
     * @param fileForData the data file
     */
    public FileSource(File fileForData) {
        this.fileForData = fileForData;
    }

    @Override
    public BufferedReader open() throws IOException {
        return new BufferedReader(new FileReader(fileForData));
    }
    
}