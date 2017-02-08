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
package tests.eu.qualimaster.monitoring;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import eu.qualimaster.common.signal.SignalMechanism;
import eu.qualimaster.coordination.CoordinationManager;
import eu.qualimaster.coordination.INameMapping;
import eu.qualimaster.coordination.NameMapping;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineSystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.observables.Scalability;
import tests.eu.qualimaster.coordination.Utils;
import tests.eu.qualimaster.storm.Naming;

/**
 * Tests the configuration observation.
 * 
 * @author Holger Eichelberger
 */
public class ConfigurationObservationTest {

    //private static final String pipName = Naming.PIPELINE_NAME;
    private INameMapping mapping;
    
    /**
     * Executed before a single test.
     */
    @Before
    public void setUp() {
        SignalMechanism.setTestMode(true);
        Utils.setModelProvider(Utils.INFRASTRUCTURE_TEST_MODEL_PROVIDER);
        Utils.configure();
        File f = new File(Utils.getTestdataDir(), "pipeline.xml");
        try {
            FileInputStream fis = new FileInputStream(f);
            mapping = new NameMapping(Naming.PIPELINE_NAME, fis);
            fis.close();
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        CoordinationManager.registerTestMapping(mapping);
    }

    /**
     * Executed after a single test.
     */
    @After
    public void tearDown() {
        CoordinationManager.unregisterNameMapping(mapping);
        Utils.dispose();
        mapping = null;
    }

    /**
     * Tests the configuration constant observation.
     */
    @Test
    public void testConfigurationConstantObservation() {
        SystemState state = new SystemState();
        PipelineSystemPart pip = state.obtainPipeline(Naming.PIPELINE_NAME);
        PipelineNodeSystemPart node = pip.obtainPipelineNode(Naming.NODE_PROCESS);
        Double tmp = node.getObservedValue(Scalability.PREDICTED_ITEMS_THRESHOLD);
        Assert.assertNotNull(tmp);
        Assert.assertEquals(500, tmp, 0.005); // 500 default value from model
        
        node.setValue(Scalability.PREDICTED_ITEMS_THRESHOLD, 510, null); // override default, create foreground
        tmp = node.getObservedValue(Scalability.PREDICTED_ITEMS_THRESHOLD);
        Assert.assertEquals(510, tmp, 0.005); // 500 default value from model
    }
    
}
