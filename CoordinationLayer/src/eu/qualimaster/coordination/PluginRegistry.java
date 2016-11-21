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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores plugins.
 * 
 * @author Holger Eichelberger
 */
class PluginRegistry {

    private static final Map<String, IPipelineResourceUnpackingPlugin> RESOURCE_UNPACKING 
        = Collections.synchronizedMap(new HashMap<String, IPipelineResourceUnpackingPlugin>());

    /**
     * Registers a pipeline resource unpacking plugin.
     * 
     * @param plugin the plugin (ignored if <b>null</b>)
     */
    static void registerPipelineResourceUnpackingPlugin(IPipelineResourceUnpackingPlugin plugin) {
        if (null != plugin) {
            RESOURCE_UNPACKING.put(plugin.getPath(), plugin);
        }
    }

    /**
     * Returns an unpacking plugin for a certain resource path (within a JAR).
     * 
     * @param path the path
     * @return the plugin (may be <b>null</b> if there is none)
     */
    static IPipelineResourceUnpackingPlugin getPipelineResourceUnpackingPlugin(String path) {
        return RESOURCE_UNPACKING.get(path);
    }
    
}
