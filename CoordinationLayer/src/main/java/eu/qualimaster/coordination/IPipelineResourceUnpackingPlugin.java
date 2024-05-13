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
 * Allows unpacking resources bundled with a pipeline or the configuration model. Implementation shall be completely 
 * serializable and self-contained. Implementations are expected to know the target artifact / folder for unpacking to.
 * 
 * @author Holger Eichelberger
 */
public interface IPipelineResourceUnpackingPlugin extends Serializable {
    
    /**
     * Unpacks the given path.
     * 
     * @param path the path to the top-level directory of the configuration model or the artifact (jar)
     * @param mapping the name mapping of the artifact (<b>null</b> for a configuration model artifact/path)
     * @throws IOException in case that unpacking fails
     */
    public void unpack(File path, INameMapping mapping) throws IOException;
    
    /**
     * Returns a descriptive name for messages.
     * 
     * @return a descriptive name
     */
    public String getName();
    
}
