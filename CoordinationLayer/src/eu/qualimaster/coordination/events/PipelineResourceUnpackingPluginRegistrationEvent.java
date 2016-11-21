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
package eu.qualimaster.coordination.events;

import eu.qualimaster.common.QMInternal;
import eu.qualimaster.coordination.IPipelineResourceUnpackingPlugin;
import eu.qualimaster.infrastructure.InfrastructureEvent;

/**
 * A plugin registration event for pipeline resource unpacking.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public class PipelineResourceUnpackingPluginRegistrationEvent extends InfrastructureEvent {

    private static final long serialVersionUID = -5946302920283511661L;
    private IPipelineResourceUnpackingPlugin plugin;
    
    /**
     * Creates the registration event.
     * 
     * @param plugin the plugin
     */
    public PipelineResourceUnpackingPluginRegistrationEvent(IPipelineResourceUnpackingPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Returns the plugin instance.
     * 
     * @return the plugin instance
     */
    public IPipelineResourceUnpackingPlugin getPlugin() {
        return plugin;
    }
    
}
