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
package eu.qualimaster.adaptation.external;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Indicates a change of configuration changes (runtime variables, admin event).
 * 
 * @author Holger Eichelberger
 */
public class ConfigurationChangeMessage extends PrivilegedMessage {

    private static final long serialVersionUID = -5170883496135114187L;
    private Map<String, Serializable> values;

    /**
     * Creates a configuration change message.
     * 
     * @param values the new values of configuration variables ([qualified] variable name - value)
     */
    public ConfigurationChangeMessage(Map<String, Serializable> values) {
        this.values = null == values ? new HashMap<String, Serializable>() : values;
    }

    /**
     * Returns the variables to change.
     * 
     * @return the variables to change (never <b>null</b>, may be empty)
     */
    public Map<String, Serializable> getValues() {
        return values;
    }
    
    @Override
    public void dispatch(IDispatcher dispatcher) {
        dispatcher.handleConfigurationChangeMessage(this);
    }

    @Override
    public int hashCode() {
        return null == values ? 0 : values.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        boolean equals = false;
        if (obj instanceof ConfigurationChangeMessage) {
            ConfigurationChangeMessage msg = (ConfigurationChangeMessage) obj;
            equals = Utils.equals(getValues(), msg.getValues());
        }
        return equals;
    }
    
    @Override
    public Message toInformation() {
        return new InformationMessage("<configuration>", null, values.toString(), null);
    }

    @Override
    public String toString() {
        return "ConfigurationChangeMessage " + values;
    }

}
