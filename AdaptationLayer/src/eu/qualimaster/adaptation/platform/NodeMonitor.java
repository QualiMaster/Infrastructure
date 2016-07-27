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
package eu.qualimaster.adaptation.platform;

import java.io.File;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import eu.qualimaster.adaptation.AdaptationConfiguration;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.events.EventManager.EventSender;
import eu.qualimaster.monitoring.events.ComponentKey;
import eu.qualimaster.monitoring.events.PlatformMultiObservationHostMonitoringEvent;
import eu.qualimaster.monitoring.spassMeter.SystemMonitor;
import eu.qualimaster.monitoring.spassMeter.SystemMonitor.ISystemMonitorListener;
import eu.qualimaster.observables.IObservable;

/**
 * Implements a general resource node monitor service linked against the infrastructure.
 * 
 * @author Holger Eichelberger
 */
public class NodeMonitor extends ToolBase implements ISystemMonitorListener {

    private static final Logger LOGGER = LogManager.getLogger(NodeMonitor.class);
    private SystemMonitor monitor;
    private EventSender sender;
    private String host = ComponentKey.getLocalHostName();
    
    /**
     * Creates a node monitor instance.
     */
    public NodeMonitor() {
        sender = new EventSender();
        monitor = new SystemMonitor(this);
    }
    
    /**
     * Starts this node monitor. [testing]
     */
    public void start() {
        monitor.start(AdaptationConfiguration.getMonitoringNodeFrequency());
    }

    /**
     * StopsStarts this node monitor. [testing]
     */
    public void stop() {
        shuttingDown();
    }

    @Override
    protected void shuttingDown() {
        sender.close();
        monitor.stop();
    }

    @Override
    public void updateObservations(Map<IObservable, Double> observations) {
        PlatformMultiObservationHostMonitoringEvent event 
            = new PlatformMultiObservationHostMonitoringEvent(host, observations);
        sender.send(event);
    }

    /**
     * Implements the node monitor.
     * 
     * @param args command line arguments - ignored
     */
    public static void main(String[] args) {
        configureLogging();

        File configFile = obtainConfigurationFile(CFG_FILE);
        if (null != configFile && configFile.exists()) {
            LOGGER.info("Reading platform configuration " + configFile + " ...");
            AdaptationConfiguration.configure(configFile);
        }
        EventManager.disableLoggingFor(AdaptationConfiguration.getEventDisableLogging());

        NodeMonitor instance = new NodeMonitor();
        instance.registerShutdownHook();
        instance.start();
        
        waitEndless();
    }

}
