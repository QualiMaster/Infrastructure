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
package tests.eu.qualimaster.adaptation;

import java.util.Properties;

import eu.qualimaster.adaptation.AdaptationConfiguration;
import eu.qualimaster.adaptation.AdaptationManager;
import eu.qualimaster.coordination.CoordinationManager;
import eu.qualimaster.coordination.commands.PipelineCommand;
import eu.qualimaster.coordination.commands.PipelineCommand.Status;
import eu.qualimaster.events.EventManager;

/**
 * Manual startup of the infrastructure with model loading.
 * 
 * @author Holger Eichelberger
 */
public class ManualTest {
    
    /**
     * Executes the test. This test ignores STORM and does not need it as it is just for the infrastructure.
     * 
     * @param args ignored
     */
    public static void main(String[] args) {
        Properties prop = AdaptationConfiguration.getDefaultProperties();
        prop.put(AdaptationConfiguration.CONFIG_MODEL_ARTIFACT_SPEC, 
            "eu.qualimaster:infrastructureModel:0.2.0-SNAPSHOT");
        prop.put(AdaptationConfiguration.PIPELINE_ELEMENTS_REPOSITORY, 
            "https://projects.sse.uni-hildesheim.de/qm/maven/");
        prop.put(AdaptationConfiguration.LOCAL_ELEMENT_REPOSITORY, "temp");
        AdaptationConfiguration.configure(prop, true);
        
        EventManager.start();
        CoordinationManager.start();
        AdaptationManager.start();
        
        new PipelineCommand("RandomPip", Status.START).execute();
        EventManager.cleanup();
        
        AdaptationManager.stop();
        CoordinationManager.stop();
        EventManager.stop();
    }
    

}
