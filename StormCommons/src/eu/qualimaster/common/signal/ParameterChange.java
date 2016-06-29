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
package eu.qualimaster.common.signal;

import java.io.Serializable;

/**
 * Stores the information about a parameter change.
 * 
 * @author Holger Eichelberger
 */
public class ParameterChange implements Serializable {
    
    private static final long serialVersionUID = 1739918664243270324L;
    private String name;
    private Serializable value;
    
    /**
     * Creates a parameter change instance.
     * 
     * @param name the parameter name
     * @param value the parameter value (must be serializable and support <code>toString</code>)
     */
    public ParameterChange(String name, Serializable value) {
        this.name = name;
        this.value = value;
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
     * Returns the new value of the parameter.
     * 
     * @return the new value
     */
    public Serializable getValue() {
        return value;
    }

    /**
     * Returns the value as a string.
     * 
     * @return the value as a string
     */
    public String getStringValue() {
        return null == value ? null : value.toString();
    }

    /**
     * Returns the value as integer.
     * 
     * @return the value
     * @throws ValueFormatException in case that the value cannot be converted
     */
    public int getIntValue() throws ValueFormatException {
        int result;
        if (null != value) {
            try {
                result = Integer.parseInt(value.toString());
            } catch (NumberFormatException e) {
                throw new ValueFormatException(e);
            }
        } else {
            throw new ValueFormatException("null");
        }
        return result;
    }
    
    /**
     * Returns the value as double.
     * 
     * @return the value
     * @throws ValueFormatException in case that the value cannot be converted
     */
    public double getDoubleValue() throws ValueFormatException {
        double result;
        if (null != value) {
            try {
                result = Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                throw new ValueFormatException(e);
            }
        } else {
            throw new ValueFormatException("null");
        }
        return result;
    }

    /**
     * Returns the value as boolean.
     * 
     * @return the value
     * @throws ValueFormatException in case that the value cannot be converted
     */
    public boolean getBooleanValue() throws ValueFormatException {
        boolean result;
        if (null != value) {
            try {
                result = Boolean.parseBoolean(value.toString());
            } catch (NumberFormatException e) {
                throw new ValueFormatException(e);
            }
        } else {
            throw new ValueFormatException("null");
        }
        return result;
    }
    
    @Override
    public String toString() {
        return getName() + " " + getValue();
    }

}
