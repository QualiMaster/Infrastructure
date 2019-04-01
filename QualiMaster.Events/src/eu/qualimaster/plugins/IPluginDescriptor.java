/*
 * Copyright 2009-2019 University of Hildesheim, Software Systems Engineering
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
package eu.qualimaster.plugins;

import eu.qualimaster.plugins.IPlugin.Action;

/**
 * Describes a plugin and provides means to start/stop a plugin. Instances are assumed to be singletons. 
 * 
 * @author Holger Eichelberger
 */
public interface IPluginDescriptor {

    /**
     * The actual plugin state.
     * 
     * @author Holger Eichelberger
     */
    public enum State {
        
        /**
         * If we just don't know.
         */
        UNKNOWN,
        
        /**
         * After loading but without explicit {@link IPlugin.Action#START}.
         */
        INITIALIZED,

        /**
         * Through/after {@link IPlugin.Action#START}.
         */
        STARTED,
        
        /**
         * Through/after {@link IPlugin.Action#SHUTDOWN} after {@link #INITIALIZED}.
         */
        STOPPED,

        /**
         * Through/after {@link IPlugin.Action#SHUTDOWN}.
         */
        SHUTDOWN
    }
    
    /**
     * Returns the class name of the plugin.
     * 
     * @return the class name
     */
    public String getClassName();
    
    /**
     * The layer the plugin is assigned with for the given {@code action}.
     * 
     * @param action the action
     * @return the layer, may be <b>null</b> but then the plugin is ignored
     */
    public ILayerDescriptor assignedTo(Action action);
    
    /**
     * Executes the given plugin action.
     * 
     * @param action the action
     * @throws PluginException in case that executing the action fails
     */
    public void execute(Action action) throws PluginException;

    /**
     * Returns the plugin state.
     * 
     * @return the state
     */
    public State getState();
    
}
