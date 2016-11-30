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
package eu.qualimaster.coordination;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.LogManager;

/**
 * Stores and registers plugins.
 * 
 * @author Holger Eichelberger
 */
public class PluginRegistry {

    private static final List<IPipelineResourceUnpackingPlugin> RESOURCE_UNPACKING 
        = Collections.synchronizedList(new ArrayList<IPipelineResourceUnpackingPlugin>());

    /**
     * Registers a pipeline resource unpacking plugin.
     * 
     * @param plugin the plugin (ignored if <b>null</b>)
     */
    public static void registerPipelineResourceUnpackingPlugin(IPipelineResourceUnpackingPlugin plugin) {
        if (null != plugin && !RESOURCE_UNPACKING.contains(plugin)) {
            RESOURCE_UNPACKING.add(plugin);
        }
    }

    /**
     * Unregisters a pipeline resource unpacking plugin.
     * 
     * @param plugin the plugin (ignored if <b>null</b>)
     */
    public static void unregisterPipelineResourceUnpackingPlugin(IPipelineResourceUnpackingPlugin plugin) {
        if (null != plugin) {
            RESOURCE_UNPACKING.remove(plugin);
        }
    }

    /**
     * Executes unpacking plugins by considering all registered plugins for <code>path</code>.
     * 
     * @param path the path to the perform the unpacking on
     * @param mapping the name mapping for pipeline artifact if given, configuration model unpacking if <b>null</b>
     */
    public static void executeUnpackingPlugins(File path, INameMapping mapping) {
        for (int u = 0; u < RESOURCE_UNPACKING.size(); u++) {
            IPipelineResourceUnpackingPlugin plugin = RESOURCE_UNPACKING.get(u);
            try {
                plugin.unpack(path, mapping);
            } catch (IOException e) {
                LogManager.getLogger(PluginRegistry.class).error("While unpacking resources  with " 
                     + plugin.getName() + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Access to all pipeline unpacking plugins.
     * 
     * @return all pipeline unpacking plugins
     */
    static Iterable<IPipelineResourceUnpackingPlugin> pipelineResourceUnpackingPlugins() {
        return RESOURCE_UNPACKING;
    }
    
}
