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
package tests.eu.qualimaster.adaptation;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import eu.qualimaster.adaptation.AdaptationConfiguration;
import eu.qualimaster.adaptation.AdaptationManager;
import eu.qualimaster.coordination.CoordinationManager;
import eu.qualimaster.coordination.StormUtils.TopologyTestInfo;
import eu.qualimaster.coordination.commands.PipelineCommand;
import eu.qualimaster.dataManagement.DataManager;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.PipelineOptions;
import eu.qualimaster.monitoring.MonitoringManager;
import tests.eu.qualimaster.TestHelper;
import tests.eu.qualimaster.coordination.AbstractCoordinationTests;
import tests.eu.qualimaster.coordination.LocalStormEnvironment;
import tests.eu.qualimaster.coordination.Utils;
import tests.eu.qualimaster.monitoring.ManualTopologyCreator;
import tests.eu.qualimaster.monitoring.ManualTopologyCreator.TopologyDescriptor;

/**
 * Helper to start a pipeline.
 * 
 * @author Holger Eichelberger
 */
public class StartPipeline {
    
    private static boolean debugRtVil = false;
    
    /**
     * Enables or disables debugging.
     * 
     * @param enable enable or disable
     */
    public static void enableDebug(boolean enable) {
        debugRtVil = enable;
    }
    
    /**
     * Starts the infrastructure, Storm and the pipeline.
     * 
     * @param pipelineName the name of the pipeline
     * @param jar the implementing Jar
     * @param easy the folder with the IVML/VIL/VTL models
     * @param repo the URL of the pipeline repository
     * @param run the time to run the pipeline
     * 
     * @throws ClassNotFoundException in case that the topology class was not found
     * @throws IllegalAccessException in case that the topology class cannot be accessed
     * @throws InstantiationException in case that the topology class cannot be instantiated
     * @throws IOException in case of I/O problems
     * @throws NoSuchFieldException if the options field in the topology creator does not exist
     */
    public static void loadAndRun(String pipelineName, File jar, File easy, String repo, int run) 
        throws InstantiationException, ClassNotFoundException, IllegalAccessException, NoSuchFieldException, 
        IOException {
        PipelineOptions options = new PipelineOptions();
        TopologyDescriptor desc = ManualTopologyCreator.loadTopology(jar, pipelineName, options);

        Utils.configure(TestHelper.LOCAL_ZOOKEEPER_PORT);
        Properties prop = new Properties();
        if (debugRtVil) {
            prop.put(AdaptationConfiguration.ADAPTATION_RTVIL_LOGGING, "true");
        }
        if (null != repo) {
            prop.put(AdaptationConfiguration.PIPELINE_ELEMENTS_REPOSITORY, 
                "https://projects.sse.uni-hildesheim.de/qm/maven/");
        }
        prop.put(AdaptationConfiguration.LOCAL_CONFIG_MODEL_ARTIFACT_LOCATION, easy.getAbsolutePath());
        AdaptationConfiguration.configure(prop);
        
        LocalStormEnvironment env = new LocalStormEnvironment();

        EventManager.startServer();
        DataManager.start();
        CoordinationManager.start();
        MonitoringManager.start();
        AdaptationManager.start();
        
        @SuppressWarnings("rawtypes")
        Map topoCfg = AbstractCoordinationTests.createTopologyConfiguration();
        options.toConf(topoCfg);
        
        CoordinationManager.registerTestMapping(desc.getNameMapping());
        TopologyTestInfo testInfo = new TopologyTestInfo(desc.getTopology(), null, topoCfg);
        
        Map<String, TopologyTestInfo> topologies = new HashMap<String, TopologyTestInfo>();
        topologies.put(pipelineName, testInfo);
        env.setTopologies(topologies);

        new PipelineCommand(pipelineName, PipelineCommand.Status.START).execute();

        AbstractCoordinationTests.sleep(run);

        new PipelineCommand(pipelineName, PipelineCommand.Status.STOP).execute();
        
        AdaptationManager.stop();
        MonitoringManager.stop();
        CoordinationManager.stop();
        DataManager.stop();
        EventManager.stop();
        
        env.shutdown();
        env.cleanup();
        
        CoordinationManager.unregisterNameMapping(desc.getNameMapping());
    }

}
