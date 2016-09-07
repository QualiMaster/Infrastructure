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
package tests.eu.qualimaster.coordination;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import backtype.storm.generated.StormTopology;
import eu.qualimaster.base.pipeline.RecordingTopologyBuilder;
import eu.qualimaster.coordination.CoordinationManager;
import eu.qualimaster.coordination.StormUtils.TopologyTestInfo;
import eu.qualimaster.coordination.commands.PipelineCommand;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent.Status;
import tests.eu.qualimaster.storm.Naming;
import tests.eu.qualimaster.storm.Topology;

/**
 * Coordination manager tests for decomposed pipelines. Set environment variable 
 * "STORM_TEST_TIMEOUT_MS" to a value greater than 15.000 (ms).
 */
public class SubPipelineTests extends AbstractCoordinationTests {

    private PipelineLifecycleEventHandler handler;
    
    /**
     * Executed before a single test.
     * 
     * @see #configure()
     */
    @Before
    public void setUp() {
        Utils.setModelProvider(Utils.INFRASTRUCTURE_TEST_MODEL_PROVIDER);
        Utils.configure();
        super.setUp();
        handler = new PipelineLifecycleEventHandler(PipelineLifecycleEvent.Status.CHECKING, 
            PipelineLifecycleEvent.Status.STARTING);
        EventManager.register(handler);
    }
    
    /**
     * Executed after a single test.
     */
    @After
    public void tearDown() {
        EventManager.unregister(handler);
        super.tearDown();
        Utils.dispose();
    }

    /**
     * Tests sub-pipelines.
     */
    @Test
    public void testSubpipeline() {
        if (CoordinationManager.HANDLE_SUBPIPELINES_ON_STARTSTOP) {
            LocalStormEnvironment env = new LocalStormEnvironment();
            @SuppressWarnings("rawtypes")
            Map config = createTopologyConfiguration();
    
            RecordingTopologyBuilder builder = new RecordingTopologyBuilder();
            Topology.createTopology(builder, Naming.PIPELINE_NAME);
            StormTopology topology = builder.createTopology();
            builder.close(Naming.PIPELINE_NAME, config);
    
            builder = new RecordingTopologyBuilder();
            Topology.createTopology(builder, Naming.SUB_PIPELINE_NAME);
            StormTopology subTopology = builder.createTopology();
            builder.close(Naming.SUB_PIPELINE_NAME, config);
    
            Map<String, TopologyTestInfo> topologies = new HashMap<String, TopologyTestInfo>();
            topologies.put(Naming.PIPELINE_NAME, new TopologyTestInfo(topology, 
                new File(Utils.getTestdataDir(), "sub/mainPipeline.xml"), config));
            topologies.put(Naming.SUB_PIPELINE_NAME, new TopologyTestInfo(subTopology, 
                new File(Utils.getTestdataDir(), "sub/subPipeline.xml"), config));
            env.setTopologies(topologies);
            clear();
            
            new PipelineCommand(Naming.PIPELINE_NAME, PipelineCommand.Status.START).execute();
    
            getPipelineStatusTracker().waitFor(Naming.PIPELINE_NAME, Status.STARTED);
    
            sleep(4000); // let Storm run for a while and start curator
            // we have no monitoring layer - jump over lifecylce
            PipelineLifecycleEvent fake = new PipelineLifecycleEvent(Naming.PIPELINE_NAME, 
                PipelineLifecycleEvent.Status.CREATED, null);
            EventManager.send(fake);
            sleep(1000); // let Storm run for a while and start curator
            
            new PipelineCommand(Naming.PIPELINE_NAME, PipelineCommand.Status.STOP).execute();
            getPipelineStatusTracker().waitFor(Naming.SUB_PIPELINE_NAME, Status.STOPPED);
    
            env.shutdown();
            env.cleanup();
        }
    }

}
