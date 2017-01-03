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
import java.util.HashMap;
import java.util.Map;

import eu.qualimaster.common.QMSupport;

/**
 * Defines optional parameters for algorithm changes. An implementation of a parameter change shall follow
 * the conventions below:
 * <ul>
 *   <li>If given, {@link #INPUT_PORT} and {@link #OUTPUT_PORT} the co-processor algorithm is already 
 *       loaded onto {@link #COPROCESSOR_HOST} and ready for switch. Use these ports for communication.</li>
 *   <li>If neither {@link #INPUT_PORT} and {@link #OUTPUT_PORT} is given, an implementation shall use 
 *       {@link #COPROCESSOR_HOST}, {@link #CONTROL_REQUEST_PORT} and {@link #CONTROL_RESPONSE_PORT} to create a 
 *       command connection and start the algorithm there.</li>
 *   <li>If none of the communication information is given, the implementation shall resort to the (default) 
 *       settings in the configuration.</li>
 *   <li>A warmup shall only be performed, if the {@link #WARMUP_DELAY} is given.</li>
 * </ul>
 * 
 * @author Holger Eichelberger
 */
@QMSupport
public enum AlgorithmChangeParameter {

    /**
     * Delay used to defer the algorithm change, to run both algorithms (old and new) 
     * in parallel in order to enable (initial) state creation. The delay is given in milliseconds.
     */
    WARMUP_DELAY(Integer.class, IParameterAccessor.INT_ACCESSOR),
    
    /**
     * The host name of a hardware co-processor to switch to.
     */
    COPROCESSOR_HOST(String.class, IParameterAccessor.STRING_ACCESSOR),

    /**
     * TCP port for receiving command responses from a hardware co-processor. If not given or needed, the same value 
     * as {@link #OUTPUT_PORT} is assumed.
     */
    CONTROL_RESPONSE_PORT(Integer.class, IParameterAccessor.INT_ACCESSOR),

    /**
     * TCP port for sending requests to a hardware co-processor. If not given or needed, the same value 
     * as {@link #OUTPUT_PORT} is assumed.
     */
    CONTROL_REQUEST_PORT(Integer.class, IParameterAccessor.INT_ACCESSOR),
    
    /**
     * TCP port for receiving data from a hardware co-processor. If not given or needed, the same value 
     * as {@link #OUTPUT_PORT} is assumed.
     */
    INPUT_PORT(Integer.class, IParameterAccessor.INT_ACCESSOR),
    
    /**
     * TCP port for sending data to a hardware co-processor. If not given or needed, the same value 
     * as {@link #INPUT_PORT} is assumed.
     */
    OUTPUT_PORT(Integer.class, IParameterAccessor.INT_ACCESSOR),
    
    /**
     * The actual implementation artifact of the algorithm. Shall contain the URL containing the artifact in the 
     * expected format (jar, zip, ...).
     */
    IMPLEMENTING_ARTIFACT(String.class, IParameterAccessor.STRING_ACCESSOR);
    
    private Class<? extends Serializable> type;
    private IParameterAccessor<?> accessor;
    
    /**
     * Creates a change parameter.
     * 
     * @param type the type of the parameter
     * @param accessor the parameter accessor
     */
    private AlgorithmChangeParameter(Class<? extends Serializable> type, IParameterAccessor<?> accessor) {
        this.type = type;
        this.accessor = accessor;
    }
    
    /**
     * Returns the parameter type.
     * 
     * @return the type
     */
    public Class<? extends Serializable> getType() {
        return type;
    }
    
    /**
     * Changes a parameter value in a generic way.
     * 
     * @param parameters the parameters
     * @param value the value
     */
    public void setParameterValue(Map<String, Serializable> parameters, Serializable value) {
        accessor.setSerializableParameter(parameters, this, value);
    }
    
    /**
     * Converts a name-value to a instance-value map.
     * 
     * @param <V> the value type
     * @param nameValues the name-values to be converted
     * @return the converted map
     * @throws IllegalArgumentException if one of the names is not a constant of this enum
     */
    public static <V> Map<AlgorithmChangeParameter, V> convert(Map<String, V> nameValues) {
        Map<AlgorithmChangeParameter, V> result = new HashMap<AlgorithmChangeParameter, V>();
        for (Map.Entry<String, V> entry : nameValues.entrySet()) {
            AlgorithmChangeParameter param = AlgorithmChangeParameter.valueOf(entry.getKey());
            result.put(param, entry.getValue());
        }
        return result;
    }
    
    /**
     * Sets an integer parameter.
     * 
     * @param parameters the parameters to change
     * @param param the parameter identifier
     * @param value the value
     * @throws IllegalArgumentException in case that the parameter does not accept an integer (or a string as fallback)
     */
    public static void setIntParameter(Map<String, Serializable> parameters, AlgorithmChangeParameter param, 
        int value) {
        if (Integer.class.isAssignableFrom(param.getType())) {
            parameters.put(param.name(), value);
        } else {
            setStringParameter(parameters, param, String.valueOf(value));
        }
    }
    
    /**
     * Returns an integer parameter.
     * 
     * @param parameters the parameters to obtain the value from
     * @param param the parameter identifier
     * @param dflt the default value in case that the parameter is not specified or cannot be turned into an integer
     *   (may be <b>null</b>)
     * @return the value of <code>param</code>, <code>dflt</code> if not specified / not an integer value
     */
    public static Integer getIntParameter(Map<String, Serializable> parameters, AlgorithmChangeParameter param, 
        Integer dflt) {
        Integer result;
        Object tmp = parameters.get(param.name());
        if (null == tmp) {
            result = dflt;
        } else if (tmp instanceof Integer) {
            result = (Integer) tmp;
        } else {
            try {
                result = Integer.valueOf(tmp.toString());
            } catch (NumberFormatException e) {
                result = dflt;
            }
        }
        return result;
    }

    /**
     * Sets a String parameter.
     * 
     * @param parameters the parameters to change
     * @param param the parameter identifier
     * @param value the value
     * @throws IllegalArgumentException in case that parameter does not accet a String value
     */
    public static void setStringParameter(Map<String, Serializable> parameters, AlgorithmChangeParameter param, 
        String value) {
        if (String.class.isAssignableFrom(param.getType())) {
            parameters.put(param.name(), value);
        } else {
            throw new IllegalArgumentException("value does not match the type of parameter " + param.name() 
                + "(" + param.getType().getName() + ")");
        }
    }
    
    /**
     * Returns a String parameter.
     * 
     * @param parameters the parameters to obtain the value from
     * @param param the parameter identifier
     * @param dflt the default value in case that the parameter is not specified (may be <b>null</b>)
     * @return the value of <code>param</code>, <code>dflt</code> if not specified
     */
    public static String getStringParameter(Map<String, Serializable> parameters, AlgorithmChangeParameter param, 
        String dflt) {
        String result;
        Object tmp = parameters.get(param.name());
        if (null == tmp) {
            result = null;
        } else {
            result = tmp.toString();
        }
        if (null == result) {
            result = dflt;
        }
        return result;
    }

    /**
     * Returns the algorithm change parameter for the given <code>name</code>.
     * 
     * @param name the name of the parameter
     * @return the related constant or <b>null</b> if none was found
     */
    public static AlgorithmChangeParameter valueOfSafe(String name) {
        AlgorithmChangeParameter result = null;
        for (AlgorithmChangeParameter p : values()) {
            if (p.name().equals(name)) {
                result = p;
            }
        }
        return result;
    }
    
}
