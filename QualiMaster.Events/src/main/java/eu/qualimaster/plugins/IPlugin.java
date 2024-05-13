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

/**
 * Interface that can optionally be implemented by plugins. Plugins not implementing this interface are assumed
 * to have a simplified lifecycle: startup upon loading, no explicit shutdown needed. 
 * 
 * @author Holger Eichelberger
 */
public interface IPlugin {

    /**
     * Generic plugin actions.
     * 
     * @author Holger Eichelberger
     */
    public enum Action {
        
        /**
         * Explicitly starts the plugin.
         */
        START,
        
        /**
         * Explicitly shuts down the plugin. Registered plugins shall be discarded/released as consequence.
         */
        SHUTDOWN
    }
    
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
     */
    public void execute(Action action);

}
