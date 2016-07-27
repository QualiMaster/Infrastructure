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
import eu.qualimaster.monitoring.AbstractMonitoringTask;
import eu.qualimaster.monitoring.MonitoringManager;
import eu.qualimaster.monitoring.hardware.HardwareConfigurationAccess;
import eu.qualimaster.monitoring.hardware.MaxelerDfeMonitor;
import eu.qualimaster.monitoring.hardware.HardwareConfigurationAccess.HardwareMonitoringInfo;
import eu.qualimaster.monitoring.systemState.SystemState;

/**
 * Tests external infrastructure connections.
 * 
 * @author Holger Eichelberger
 */
public class HwMonitoringTest {

    private static final Logger LOGGER = LogManager.getLogger(HwMonitoringTest.class);
    
    /**
     * Implements a simple empty monitoring task to take up the hardware piggyback tasks.
     * 
     * @author Holger Eichelberger
     */
    private static class TestMonitoringTask extends AbstractMonitoringTask  {

        /**
         * Creates the task.
         * 
         * @param state the system state to fill with
         */
        protected TestMonitoringTask(SystemState state) {
            super(state);
        }

        @Override
        public int getFrequency() {
            return 1000;
        }

        @Override
        protected void failover(Throwable th) {
        }

        @Override
        protected void monitor() {
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
        final TestMonitoringTask task = new TestMonitoringTask(state);
        HardwareMonitoringInfo[] info = HardwareConfigurationAccess.getHardwareClusterInfo();
        if (0 == info.length) {
            System.out.println("No cluster information available. Stopping.");
        } else {
            for (int i = 0; i < info.length; i++) {
                MaxelerDfeMonitor mTask = new MaxelerDfeMonitor(info[i], state);
                mTask.setDebug(true);
                task.add(mTask);
            }
    
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

}
