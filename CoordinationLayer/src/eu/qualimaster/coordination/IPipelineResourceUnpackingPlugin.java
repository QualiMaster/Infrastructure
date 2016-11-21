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
package eu.qualimaster.coordination;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/**
 * Allows unpacking resources bundled with a pipeline. Implementation shall be completely serializable and 
 * self-contained.
 * 
 * @author Holger Eichelberger
 */
public interface IPipelineResourceUnpackingPlugin extends Serializable {

    /**
     * Returns the top-level path this plugin reacts on.
     * 
     * @return the top-level path
     */
    public String getPath();
    
    /**
     * Unpacks the given path.
     * 
     * @param dir the top-level directory
     * @throws IOException in case that unpacking fails
     */
    public void unpack(File dir) throws IOException;
    
    /**
     * Returns a descriptive name for messages.
     * 
     * @return a descriptive name
     */
    public String getName();
    
}
