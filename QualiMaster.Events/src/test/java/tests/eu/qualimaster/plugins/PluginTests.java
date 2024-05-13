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
package tests.eu.qualimaster.plugins;

import org.junit.Test;

import eu.qualimaster.plugins.ILayerDescriptor;
import eu.qualimaster.plugins.IPlugin;
import eu.qualimaster.plugins.IPluginDescriptor;
import eu.qualimaster.plugins.IPluginDescriptor.State;
import eu.qualimaster.plugins.PluginRegistry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import tests.eu.qualimaster.AbstractTest;

/**
 * Tests the plugin mechanism.
 * 
 * @author Holger Eichelberger
 */
public class PluginTests extends AbstractTest {

    /**
     * Tests the plugin mechanism.
     */
    @Test
    public void testPlugins() {
        Assert.assertEquals(0, PluginRegistry.registeredLayersCount());
        for (TestPhases p : TestPhases.values()) {
            PluginRegistry.registerLayer(p);
        }
        
        Assert.assertEquals(TestPhases.values().length, PluginRegistry.registeredLayersCount());
        Set<ILayerDescriptor> tmp = new HashSet<ILayerDescriptor>();
        for (ILayerDescriptor d : PluginRegistry.registeredLayers()) {
            tmp.add(d);
        }
        for (TestPhases p : TestPhases.values()) {
            tmp.remove(p);
        }        
        Assert.assertEquals(0, tmp.size());
        
        Assert.assertEquals(0, PluginRegistry.registeredCount());
        PluginRegistry.loadPlugins(AbstractTest.TESTDATA);
        // all but one
        Assert.assertEquals(4, PluginRegistry.registeredCount());
        Map<String, IPluginDescriptor> plugins = new HashMap<String, IPluginDescriptor>();
        for (IPluginDescriptor desc : PluginRegistry.registered()) {
            String name = desc.getClassName();
            int pos = name.lastIndexOf('.');
            if (pos > 0 && pos < name.length() - 1) {
                name = name.substring(pos + 1);
            }
            plugins.put(name, desc);
        }
        // our plugins have unique simple names
        Assert.assertEquals(PluginRegistry.registeredCount(), plugins.size());
        // this was failing with the constructor, shall not be there
        Assert.assertTrue(!plugins.containsKey("FailPlugin1"));

        testInit(plugins);
        testStart(plugins);
        testShutdown(plugins);

        PluginRegistry.clear();
        Assert.assertEquals(0, PluginRegistry.registeredCount());
        Assert.assertEquals(0, PluginRegistry.registeredLayersCount());
    }

    /**
     * Tests initialized plugins and states.
     * 
     * @param plugins the plugins
     */
    private static void testInit(Map<String, IPluginDescriptor> plugins) {
        // as in Manifest (default)
        assertPlugin(plugins, "BasicPlugin", State.INITIALIZED, TestPhases.TEST1, TestPhases.TEST2);
        // as in code
        assertPlugin(plugins, "LifecyclePlugin", State.INITIALIZED, TestPhases.TEST2, TestPhases.TEST3);
        // as in manifest, code fails
        assertPlugin(plugins, "FailPlugin2", State.INITIALIZED, TestPhases.TEST2, TestPhases.TEST3);
        // as in Manifest, overriding
        assertPlugin(plugins, "FailPlugin3", State.INITIALIZED, TestPhases.TEST1, TestPhases.TEST2);
    }
    
    /**
     * Tests starting plugins and states.
     * 
     * @param plugins the plugins
     */
    private static void testStart(Map<String, IPluginDescriptor> plugins) {
        PluginRegistry.startPlugins(TestPhases.TEST1);
        // remains initialized as basic plugin
        assertPlugin(plugins, "BasicPlugin", State.INITIALIZED, TestPhases.TEST1, TestPhases.TEST2);
        assertPlugin(plugins, "LifecyclePlugin", State.INITIALIZED, TestPhases.TEST2, TestPhases.TEST3);
        assertPlugin(plugins, "FailPlugin2", State.INITIALIZED, TestPhases.TEST2, TestPhases.TEST3);
        // remains initialized due to exception
        assertPlugin(plugins, "FailPlugin3", State.INITIALIZED, TestPhases.TEST1, TestPhases.TEST2);

        PluginRegistry.startPlugins(TestPhases.TEST2);
        // remains initialized as basic plugin
        assertPlugin(plugins, "BasicPlugin", State.INITIALIZED, TestPhases.TEST1, TestPhases.TEST2);
        assertPlugin(plugins, "LifecyclePlugin", State.STARTED, TestPhases.TEST2, TestPhases.TEST3);
        assertPlugin(plugins, "FailPlugin2", State.STARTED, TestPhases.TEST2, TestPhases.TEST3);
        assertPlugin(plugins, "FailPlugin3", State.INITIALIZED, TestPhases.TEST1, TestPhases.TEST2);
        
        PluginRegistry.startPlugins(TestPhases.TEST3);
        assertPlugin(plugins, "BasicPlugin", State.INITIALIZED, TestPhases.TEST1, TestPhases.TEST2);
        assertPlugin(plugins, "LifecyclePlugin", State.STARTED, TestPhases.TEST2, TestPhases.TEST3);
        assertPlugin(plugins, "FailPlugin2", State.STARTED, TestPhases.TEST2, TestPhases.TEST3);
        assertPlugin(plugins, "FailPlugin3", State.INITIALIZED, TestPhases.TEST1, TestPhases.TEST2);
    }

    /**
     * Tests shutting down plugins and states.
     * 
     * @param plugins the plugins
     */
    private static void testShutdown(Map<String, IPluginDescriptor> plugins) {
        PluginRegistry.shutdownPlugins(TestPhases.TEST3);
        assertPlugin(plugins, "BasicPlugin", State.INITIALIZED, TestPhases.TEST1, TestPhases.TEST2);
        assertPlugin(plugins, "LifecyclePlugin", State.SHUTDOWN, TestPhases.TEST2, TestPhases.TEST3);
        assertPlugin(plugins, "FailPlugin2", State.SHUTDOWN, TestPhases.TEST2, TestPhases.TEST3);
        assertPlugin(plugins, "FailPlugin3", State.INITIALIZED, TestPhases.TEST1, TestPhases.TEST2);
        
        PluginRegistry.shutdownPlugins(TestPhases.TEST2);
        // stopped as basic plugin
        assertPlugin(plugins, "BasicPlugin", State.STOPPED, TestPhases.TEST1, TestPhases.TEST2);
        assertPlugin(plugins, "LifecyclePlugin", State.SHUTDOWN, TestPhases.TEST2, TestPhases.TEST3);
        assertPlugin(plugins, "FailPlugin2", State.SHUTDOWN, TestPhases.TEST2, TestPhases.TEST3);
        // remains initialized due to exception
        assertPlugin(plugins, "FailPlugin3", State.INITIALIZED, TestPhases.TEST1, TestPhases.TEST2);

        PluginRegistry.shutdownPlugins(TestPhases.TEST1);
        // nothing changes
        assertPlugin(plugins, "BasicPlugin", State.STOPPED, TestPhases.TEST1, TestPhases.TEST2);
        assertPlugin(plugins, "LifecyclePlugin", State.SHUTDOWN, TestPhases.TEST2, TestPhases.TEST3);
        assertPlugin(plugins, "FailPlugin2", State.SHUTDOWN, TestPhases.TEST2, TestPhases.TEST3);
        assertPlugin(plugins, "FailPlugin3", State.INITIALIZED, TestPhases.TEST1, TestPhases.TEST2);
    }

    /**
     * Asserts plugin information.
     * 
     * @param plugins the plugins
     * @param name the name of the plugin
     * @param state the expected state
     * @param layers expected layers for plugins actions
     */
    private static void assertPlugin(Map<String, IPluginDescriptor> plugins, String name, State state, 
        ILayerDescriptor... layers) {
        Assert.assertTrue(plugins.containsKey(name));
        IPluginDescriptor desc = plugins.get(name);
        Assert.assertNotNull(desc);
        Assert.assertEquals(desc.getState(), state);
        IPlugin.Action[] actions = IPlugin.Action.values();
        for (int a = 0; a < Math.max(actions.length, layers.length); a++) {
            ILayerDescriptor res = desc.assignedTo(actions[a]);
            Assert.assertTrue("Descriptor " + name + " must be assigned to " + layers[a] + " rather than " + res 
                + " for " + actions[a], layers[a] == res);
        }
    }
    
}
