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

/**
 * Allows to obtain a profile control parser based on the type of file.
 * 
 * @author Holger Eichelberger
 */
public class ProfileControlParserFactory {

    public static final ProfileControlParserFactory INSTANCE = new ProfileControlParserFactory();

    /**
     * Prevents external instantiation.
     */
    private ProfileControlParserFactory() {
    }
    
    /**
     * Returns the parser for the given file (type).
     * 
     * @param file the file to be parsed
     * @return the parser
     */
    public IProfileControlParser getParser(File file) {
        return new SimpleParser(); // currently there is just one ;)
    }
    
}
