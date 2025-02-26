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
package eu.qualimaster.common.monitoring;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import backtype.storm.hooks.info.EmitInfo;
import eu.qualimaster.monitoring.events.FrozenSystemState;
import eu.qualimaster.observables.IObservable;

/**
 * Registers monitoring plugins.
 * 
 * @author Holger Eichelberger
 */
public class MonitoringPluginRegistry {

    private static final List<IMonitoringPlugin> PLUGINS = new ArrayList<IMonitoringPlugin>(); 
    
    /**
     * Registers the given monitoring plugin.
     * 
     * @param plugin the plugin, ignored if <b>null</b>
     */
    public static void register(IMonitoringPlugin plugin) {
        if (null != plugin) {
            PLUGINS.add(plugin);
        }
    }

    /**
     * Unregisters the given monitoring plugin.
     * 
     * @param plugin the plugin, ignored if <b>null</b>
     */
    public static void unregister(IMonitoringPlugin plugin) {
        if (null != plugin) {
            PLUGINS.remove(plugin);
        }
    }
    
    /**
     * Starts monitoring for an execution method.
     */
    public static void startMonitoring() {
        for (int p = 0, n = PLUGINS.size(); p < n; p++) {
            PLUGINS.get(p).startMonitoring();
        }
    }
    
    /**
     * Notifies about emitting tuples.
     * 
     * @param info information about the last emit
     */
    public static void emitted(EmitInfo info) {
        for (int p = 0, n = PLUGINS.size(); p < n; p++) {
            PLUGINS.get(p).emitted(info);
        }
    }

    /**
     * Notifies about emitting sink tuples.
     * 
     * @param tuple the emitted tuple
     */
    public static void emitted(Object tuple) {
        for (int p = 0, n = PLUGINS.size(); p < n; p++) {
            PLUGINS.get(p).emitted(tuple);
        }
    }


    /**
     * Ends monitoring for an execution method.
     */
    public static void endMonitoring() {
        for (int p = 0, n = PLUGINS.size(); p < n; p++) {
            PLUGINS.get(p).endMonitoring();
        }
    }
    
    /**
     * Collects the observations for sending them to the infrastructure.
     * 
     * @param observations the observations to be modified as a side effect
     */
    public static void collectObservations(Map<IObservable, Double> observations) {
        for (int p = 0, n = PLUGINS.size(); p < n; p++) {
            PLUGINS.get(p).collectObservations(observations);
        }
    }
    
    /**
     * Returns the number of registered plugins.
     * 
     * @return the number of plugins
     */
    public static int getRegisteredPluginCount() {
        return PLUGINS.size();
    }
    
    /**
     * Called during analysis by the Monitoring Layer.
     * 
     * @param state the actual frozen system state
     */
    public static void analyze(FrozenSystemState state) {
        for (int p = 0, n = PLUGINS.size(); p < n; p++) {
            PLUGINS.get(p).analyze(state);
        }
    }

}
