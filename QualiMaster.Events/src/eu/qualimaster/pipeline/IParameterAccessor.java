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
package eu.qualimaster.pipeline;

import java.io.Serializable;
import java.util.Map;

/**
 * Parameter value accessors.
 * 
 * @param <T> the value type
 * @author Holger Eichelberger
 */
interface IParameterAccessor<T> {
    
    public static IParameterAccessor<Integer> INT_ACCESSOR = new IParameterAccessor<Integer>() {
        
        @Override
        public void setSerializableParameter(Map<String, Serializable> parameters, AlgorithmChangeParameter parameter, 
            Serializable value) {
            if (value instanceof Integer) {
                setParameter(parameters, parameter, (Integer) value);
            } else if (value != null) {
                String tmp = value.toString();
                try {
                    setParameter(parameters, parameter, Integer.parseInt(tmp));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Parameter value is not an integer " + e.getMessage());
                }
            } else {
                throw new IllegalArgumentException("Parameter value is null");
            }
        }

        @Override
        public void setParameter(Map<String, Serializable> parameters, AlgorithmChangeParameter parameter, 
            Integer value) {
            AlgorithmChangeParameter.setIntParameter(parameters, parameter, (Integer) value);
        }

        @Override
        public Integer getParameter(Map<String, Serializable> parameters, AlgorithmChangeParameter parameter, 
            Integer dflt) {
            return AlgorithmChangeParameter.getIntParameter(parameters, parameter, dflt);
        }

    };
    
    public static IParameterAccessor<String> STRING_ACCESSOR = new IParameterAccessor<String>() {
        
        @Override
        public void setSerializableParameter(Map<String, Serializable> parameters, AlgorithmChangeParameter parameter, 
            Serializable value) {
            if (value instanceof String) {
                setParameter(parameters, parameter, (String) value);
            } else {
                throw new IllegalArgumentException("Parameter value is not of type String");
            }
        }
        
        public void setParameter(Map<String, Serializable> parameters, AlgorithmChangeParameter parameter, 
            String value) {
            AlgorithmChangeParameter.setStringParameter(parameters, parameter, (String) value);
        }
        
        @Override
        public String getParameter(Map<String, Serializable> parameters, AlgorithmChangeParameter parameter, 
            String dflt) {
            return AlgorithmChangeParameter.getStringParameter(parameters, parameter, dflt);
        }

    };
    
    /**
     * Sets a parameter value.
     * 
     * @param parameters containing the value mapping
     * @param parameter the parameter
     * @param value the value
     * @throws IllegalArgumentException in case that value does not fit to <code>parameter</code>
     */
    public void setSerializableParameter(Map<String, Serializable> parameters, AlgorithmChangeParameter parameter, 
        Serializable value);

    /**
     * Sets a parameter value.
     * 
     * @param parameters containing the value mapping
     * @param parameter the parameter
     * @param value the value
     * @throws IllegalArgumentException in case that value does not fit to <code>parameter</code>
     */
    public void setParameter(Map<String, Serializable> parameters, AlgorithmChangeParameter parameter, T value);

    /**
     * Returns a parameter value.
     * 
     * @param parameters containing the value mapping
     * @param parameter the parameter
     * @param deflt the default value if not set
     * @return the value or <code>deflt</code>
     */
    public T getParameter(Map<String, Serializable> parameters, AlgorithmChangeParameter parameter, T deflt);

}
