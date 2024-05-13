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
package eu.qualimaster.coordination.profiling;

import java.io.File;
import java.io.IOException;

/**
 * Defines the interface for profiling control file parser.
 * 
 * @author Holger Eichelberger
 */
public interface IProfileControlParser {

    /**
     * Parses the control file.
     * 
     * @param file the file to read
     * @param profile the profile data
     * @return the actual data file (from {@link IProfile#getDataFile()} if not changed by imports)
     * @throws IOException if loading/parsing the control file fails
     */
    public ParseResult parseControlFile(File file, IProfile profile) throws IOException;

}
