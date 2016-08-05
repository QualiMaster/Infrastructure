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
public class ConfigurationChangeRequest extends RequestMessage {

    private static final long serialVersionUID = -5170883496135114187L;
    private Map<String, Serializable> values;

    /**
     * Creates a configuration change message.
     * 
     * @param values the new values of configuration variables ([qualified] variable name - value)
     */
    public ConfigurationChangeRequest(Map<String, Serializable> values) {
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
        if (obj instanceof ConfigurationChangeRequest) {
            ConfigurationChangeRequest msg = (ConfigurationChangeRequest) obj;
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
    
    /**
     * Copies the contents of <code>map</code>.
     * 
     * @param map the map to be copied (may be <b>null</b>)
     * @return the copy
     */
    private static Map<String, Serializable> copy(Map<String, Serializable> map) {
        Map<String, Serializable> result = new HashMap<String, Serializable>();
        if (null != map) {
            result.putAll(map);
        }
        return result;
    }

    @Override
    public Message elevate() {
        return new ElevatedConfigurationChangeRequest(this);
    }

    /**
     * Implements an elevated change parameter request.
     * 
     * @author Holger Eichelberger
     */
    private static class ElevatedConfigurationChangeRequest extends ConfigurationChangeRequest {

        private static final long serialVersionUID = -6506199880938635337L;

        /**
         * Creates an elevated resource change request.
         * 
         * @param request the original request
         */
        private ElevatedConfigurationChangeRequest(ConfigurationChangeRequest request) {
            super(copy(request.getValues()));
            setMessageId(request.getMessageId());
            setClientId(request.getClientId());
        }
        
        @Override
        public final boolean requiresAuthentication() {
            return true; // privileged messages require always an authenticated connection, no reduction possible
        }

        @Override
        public final boolean passToUnauthenticatedClient() {
            return false; // pass never
        }
        
        @Override
        public final Message elevate() {
            return this; // we are already elevated
        }

    }

}
