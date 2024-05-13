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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import eu.qualimaster.plugins.IPlugin.Action;

/**
 * Handles plugins and provides access to them.
 * 
 * @author Holger Eichelberger
 */
public class PluginRegistry {

    public static final String JAR_ATTRIBUTE = "QM-Plugins";
    private static final Logger LOGGER = LogManager.getLogger(PluginRegistry.class);
    private static final Map<String, ILayerDescriptor> LAYERS = new HashMap<String, ILayerDescriptor>();
    
    // due to different phases for the actions, we just register all plugins and select them later.
    // assumption: not so many plugins that we run into performance problems
    private static final List<IPluginDescriptor> PLUGINS = new ArrayList<IPluginDescriptor>();

    /**
     * Registers a layer (descriptor). Registering is needed to avoid forward declarations, to enable layer 
     * extensibility and to keep the descriptor singleton property.
     * 
     * @param descriptor the descriptor to be registered
     */
    public static void registerLayer(ILayerDescriptor descriptor) {
        if (null != descriptor && null != descriptor.name() && !LAYERS.containsKey(descriptor.name())) {
            LAYERS.put(descriptor.name(), descriptor);
        } else {
            LOGGER.error("Cannot register plugin descriptor: " + descriptor + " (something is null)");
        }
    }
    
    /**
     * Loads all plugins from a given directory. All required {@link ILayerDescriptor layer descriptors} shall be
     * registered beforehand.
     * 
     * @param dir the directory to load the plugins from
     */
    public static void loadPlugins(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (null != files) {
                for (File f : files) {
                    if (f.isFile() && f.getName().endsWith(".jar")) {
                        loadPluginJar(f);
                    }
                }
            }
        } else {
            LOGGER.error("Given plugin directory " + dir + " does not exist.");
        }
    }

    /**
     * Loads all plugins from the given JAR file. All required {@link ILayerDescriptor layer descriptors} shall be
     * registered beforehand.
     * 
     * @param file the JAR file to load the plugins from
     */
    private static void loadPluginJar(File file) {
        try {
            JarFile jar = new JarFile(file);
            Attributes attr = jar.getManifest().getMainAttributes();
            String value = null;
            if (null != attr) {
                value = attr.getValue(JAR_ATTRIBUTE);
            }
            try {
                jar.close();
                if (null != value) {
                    EntryParser.parseManifestPluginEntry(value, new PluginHandler(file));
                } else {
                    LOGGER.warn("No attribute " + JAR_ATTRIBUTE + " found in " + file + ". Cannot load plugins. "
                        + "Ignoring File.");
                }
            } catch (IOException e) {
                LOGGER.warn("Cannot close " + file + " as JAR. Cannot load plugins. Ignoring File.");
            }
        } catch (IOException e) {
            LOGGER.warn("Cannot open " + file + " as JAR. Cannot load plugins. Ignoring File.");
        }
    }
    
    /**
     * Plugin handler for loading plugins.
     * 
     * @author Holger Eichelberger
     */
    private static class PluginHandler implements EntryParser.IPluginEntryHandler {

        private URLClassLoader loader;
        private File file;
        
        /**
         * Creates a handler.
         * 
         * @param file the JAR file the handler is operating on
         */
        private PluginHandler(File file) {
            this.file = file;
        }
        
        @Override
        public void handle(String cls, String[] layers) {
            if (null == loader) {
                try {
                    URL[] urls = new URL[] {file.toURI().toURL()};
                    loader = new URLClassLoader(urls);
                } catch (MalformedURLException e) {
                    LOGGER.error("Cannot obtain URL for " + file + ": " + e.getMessage());
                }
            }
            if (null != loader) {
                try {
                    ILayerDescriptor[] ldesc = toDescriptors(layers);
                    Class<?> c = loader.loadClass(cls);
                    IPluginDescriptor descriptor;
                    if (IPlugin.class.isAssignableFrom(c)) {
                        descriptor = new InterfacedPluginDescriptor(c, ldesc);
                    } else {
                        descriptor = new BasicPluginDescriptor(c, ldesc);
                    }
                    PLUGINS.add(descriptor);
                } catch (PluginException e) {
                    LOGGER.error("Cannot load/register plugin: " + e.getMessage());
                } catch (ClassNotFoundException e) {
                    LOGGER.error("Cannot load/register plugin " + cls + ": " + e.getMessage());
                }
            }
        }
        
    }
    
    /**
     * Turns layer names to layers. 
     * 
     * @param layers the layer names (may be null, empty, with one or two names)
     * @return the layers as singleton/constants
     * @throws PluginException in case that a layer name is not known
     */
    private static ILayerDescriptor[] toDescriptors(String[] layers) throws PluginException {
        ILayerDescriptor[] result;
        if (null == layers || layers.length == 0) {
            result = new ILayerDescriptor[0];
        } else {
            result = new ILayerDescriptor[2];
            result[0] = getDescriptor(layers[0]);
            if (layers.length > 1) {
                result[1] = getDescriptor(layers[1]);
            } else {
                result[1] = result[0];
            }
        }
        return result;
    }
    
    /**
     * Turns a layer name into a layer singleton/constant.
     * 
     * @param layer the layer
     * @return the layer constant instance
     * @throws PluginException in case that {@code layer} is unknown or <b>null</b>
     */
    private static ILayerDescriptor getDescriptor(String layer) throws PluginException {
        if (null == layer || null == LAYERS.get(layer)) {
            throw new PluginException("Unknown/unregistered layer: " + layer);
        }
        return LAYERS.get(layer);
    }
    
    /**
     * Returns an iterable of registered plugins.
     * 
     * @return the iterable
     */
    public static Iterable<IPluginDescriptor> registered() {
        return PLUGINS;
    }

    /**
     * Returns the number of registered plugins.
     * 
     * @return the number of layers
     */
    public static int registeredCount() {
        return PLUGINS.size();
    }
    
    /**
     * Returns the registered layers.
     * 
     * @return the registered layers (no sequence guarantee!)
     */
    public static Iterable<ILayerDescriptor> registeredLayers() {
        return LAYERS.values();
    }

    /**
     * Returns the number of registered layers.
     * 
     * @return the number of layers
     */
    public static int registeredLayersCount() {
        return LAYERS.size();
    }

    /**
     * Performs an {@code action} on all plugins registered for {@code layer}.
     * 
     * @param layer the layer to select the plugins for
     * @param action the action to execute
     */
    private static void doFor(ILayerDescriptor layer, Action action) {
        List<IPluginDescriptor> done = new ArrayList<IPluginDescriptor>();
        for (IPluginDescriptor pl : registered()) {
            if (layer == pl.assignedTo(action)) {
                try {
                    pl.execute(action);
                    done.add(pl);
                } catch (PluginException e) {
                    LOGGER.error(e.getMessage());
                }
            }
        }
        if (Action.SHUTDOWN == action) {
            for (IPluginDescriptor pl : done) {
                PLUGINS.remove(pl);
            }
        }
    }
    
    /**
     * Starts all plugins for {@code layer}. [convenience]
     * 
     * @param layer the layer to start the plugins for
     */
    public static void startPlugins(ILayerDescriptor layer) {
        doFor(layer, Action.START);
    }

    /**
     * Stops all plugins for {@code layer}. [convenience]
     * 
     * @param layer the layer to start the plugins for
     */
    public static void shutdownPlugins(ILayerDescriptor layer) {
        doFor(layer, Action.SHUTDOWN);
    }
    
    /**
     * Clears the entire registry. [testing]
     */
    public static void clear() {
        PLUGINS.clear();
        LAYERS.clear();
    }

}
