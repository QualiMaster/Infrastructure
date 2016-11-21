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
package eu.qualimaster;

import java.io.Serializable;

/**
 * Changes (pipeline) options.
 * 
 * @author Holger Eichelberger
 */
public interface IOptionSetter {
    
    /**
     * Explicitly sets an option. Using the more specific setters shall be preferred except for free/unknown options.
     * 
     * @param key the key to set the value for (<b>null</b> is ignored)
     * @param value the value
     */
    public void setOption(String key, Serializable value);

}
