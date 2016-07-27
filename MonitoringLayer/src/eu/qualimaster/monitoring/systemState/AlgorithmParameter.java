/*
 * Copyright 2009-2015 University of Hildesheim, Software Systems Engineering
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
package eu.qualimaster.monitoring.systemState;

/**
 * Represents an algorithm parameter.
 * 
 * @author Holger Eichelberger
 */
public class AlgorithmParameter {

    private String name;
    private String value;

    /**
     * Creates an algorithm parameter.
     * 
     * @param name the name of the parameter
     * @param value the actual value
     */
    public AlgorithmParameter(String name, String value) {
        this.name = name;
        this.value = value;
    }
    
    /**
     * Sets the value of the parameter representation.
     *  
     * @param value the new value
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Sets the value of the parameter representation.
     *  
     * @param value the new value
     */
    public void setValue(Object value) {
        if (null == value) {
            this.value = "";
        } else {
            this.value = value.toString();
        }
    }

    /**
     * Returns the name of the parameter.
     * 
     * @return the name of the parameter
     */
    public String getName() {
        return name;
    }
    
    /**
     * Returns the value of the parameter.
     * 
     * @return the value
     */
    public String getValue() {
        return value;
    }
    
}
