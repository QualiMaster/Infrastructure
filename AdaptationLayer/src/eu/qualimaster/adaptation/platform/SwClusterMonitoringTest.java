/*
 * Copyright 2009-2015 University of Hildesheim, Software Systems Engineering
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
import java.util.Timer;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import eu.qualimaster.adaptation.AdaptationConfiguration;
import eu.qualimaster.coordination.RepositoryConnector;
import eu.qualimaster.coordination.RepositoryHelper;
import eu.qualimaster.monitoring.MonitoringManager;
import eu.qualimaster.monitoring.storm.StormClusterMonitoringTask;
import eu.qualimaster.monitoring.storm.StormConnection;
import eu.qualimaster.monitoring.systemState.SystemState;

/**
 * A simple test for software cluster monitoring.
 * 
 * @author Holger Eichelberger
 */
public class SwClusterMonitoringTest {

    private static final Logger LOGGER = LogManager.getLogger(SwClusterMonitoringTest.class);
    
    /**
     * Slightly extend cluster monitoring task for testing.
     * 
     * @author Holger Eichelberger
     */
    protected class TestStormClusterMonitoringTask extends StormClusterMonitoringTask {

        /**
         * Creates a monitoring task for a storm cluster.
         * 
         * @param connection the Storm connection
         * @param state the system state to be modified as part of the monitoring
         */
        public TestStormClusterMonitoringTask(StormConnection connection, SystemState state) {
            super(connection, state);
        }
        
        @Override
        public void monitor() {
            super.monitor();
            System.out.println(getState());
        }
        
    }
    
    /**
     * Performs the monitoring test.
     * 
     * @param args ignored
     */
    public static void main(String[] args) {
        File configFile = Main.obtainConfigurationFile(Main.CFG_FILE);
        if (!configFile.exists()) {
            configFile = new File("scripts/qm.infrastructure.cfg");
        } else {
            RepositoryHelper.setOverrideIfExists(false);
        }
        if (null != configFile && configFile.exists()) {
            LOGGER.info("Reading platform configuration " + configFile + " ...");
            AdaptationConfiguration.configure(configFile);
        }
        RepositoryConnector.initialize();
        SystemState state = MonitoringManager.getSystemState();
        StormConnection connection = new StormConnection();
        final StormClusterMonitoringTask task = new StormClusterMonitoringTask(connection, state);
        
        final Timer timer = new Timer();
        timer.schedule(task, 0, task.getFrequency());

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            
            @Override
            public void run() {
                task.cancel();
                timer.cancel();
            }
        }));
    }

}
