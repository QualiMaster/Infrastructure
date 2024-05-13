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
package eu.qualimaster.common.shedding;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Defines the basic interface of a load schedder, i.e., a mechanism that throws away data. Load schedders must have
 * a public default constructor and be serializable. Parameters are set at runtime if needed, e.g., upon activation. 
 * Load schedding is considered as a default infrastructure capability and, thus, not handled via parameters. 
 * {@link #configure(ILoadShedderConfigurer)} must be called to activate the shedding. Shedders must be serializable 
 * 
 * @param <T> the tuple type, use object for type-independent schedders. If this type is not fulfilled upon calling,
 *   the load shedder will not be considered and the respective tuples will pass
 * @author Holger Eichelberger
 */
public abstract class LoadShedder<T> implements Serializable {
    
    private static final long serialVersionUID = -5825369421252622601L;
    private ILoadShedderDescriptor descriptor;
    private Class<T> tupleType;
    private Set<ILoadSheddingParameter> parameters;

    /**
     * Creates a load shedder.
     * 
     * @param identifier the load shedder identifier
     * @param tupleType the tuple type
     * @param parameters the supported parameters
     */
    protected LoadShedder(final String identifier, Class<T> tupleType, ILoadSheddingParameter... parameters) {
        this(new ILoadShedderDescriptor() {
            
            private static final long serialVersionUID = -3453452878085684163L;

            @Override
            public String getIdentifier() {
                return identifier;
            }

            @Override
            public String getShortName() {
                return null;
            }
        }, tupleType);
    }

    /**
     * Creates a load shedder.
     * 
     * @param descriptor the load shedder descriptor
     * @param tupleType the tuple type
     * @param parameters the supported parameters
     */
    protected LoadShedder(ILoadShedderDescriptor descriptor, Class<T> tupleType, ILoadSheddingParameter... parameters) {
        this.descriptor = descriptor;
        this.tupleType = tupleType;
        this.parameters = new HashSet<ILoadSheddingParameter>();
        for (ILoadSheddingParameter p : parameters) {
            this.parameters.add(p);
        }
    }
    
    /**
     * Returns the tuple type.
     * 
     * @return the tuple type
     */
    public Class<T> getTupleType() {
        return tupleType;
    }
    
    /**
     * Returns whether <code>tuple</code> is enabled and shall not be shedded.
     * 
     * @param tuple the tuple
     * @return <code>true</code> for enabled, <code>false</code> else
     */
    public boolean isEnabled(Object tuple) {
        boolean result = true;
        if (tupleType.isInstance(tuple)) {
            result = isEnabledImpl(tupleType.cast(tuple));
        }
        return result;
    }
    
    /**
     * Returns whether <code>tuple</code> is enabled and shall not be shedded.
     * 
     * @param tuple the tuple
     * @return <code>true</code> for enabled, <code>false</code> else
     */
    protected abstract boolean isEnabledImpl(T tuple);
    
    /**
     * Returns the load schedding descriptor.
     * 
     * @return the descriptor
     */
    public ILoadShedderDescriptor getDescriptor() {
        return descriptor;
    }
    
    /**
     * Returns whether this shedder supports a certain parameter.
     * 
     * @param parameter the parameter
     * @return <code>true</code> for support, <code>false</code> else
     */
    public boolean supportsParameter(ILoadSheddingParameter parameter) {
        return null == parameter ? false : parameters.contains(parameter);
    }
   
    /**
     * Configures this shedder.
     * 
     * @param configurer the configurer containing configuration information
     */
    public abstract void configure(ILoadShedderConfigurer configurer);
    
    /**
     * Returns the actual configuration of this shedder.
     * 
     * @return the configuration in terms of parameters 
     */
    public abstract Map<ILoadSheddingParameter, Serializable> getConfiguration();

    /**
     * Convenience method to create a configuration map for one parameter.
     * 
     * @param parameter the parameter
     * @param value the value
     * @return the configuration map
     */
    protected static Map<ILoadSheddingParameter, Serializable> getConfiguration(ILoadSheddingParameter parameter, 
        Serializable value) {
        Map<ILoadSheddingParameter, Serializable> result = new HashMap<ILoadSheddingParameter, Serializable>();
        result.put(parameter, value);
        return result;
    }
    
    /**
     * Returns the actual configuration of this shedder in terms of the names of the parameters (strings).
     * 
     * @return the converted configuration
     */
    public final Map<String, Serializable> getStringConfiguration() {
        Map<String, Serializable> result = new HashMap<String, Serializable>();
        for (Map.Entry<ILoadSheddingParameter, Serializable> e : getConfiguration().entrySet()) {
            result.put(e.getKey().name(), e.getValue());
        }
        return result;
    }
    
}
