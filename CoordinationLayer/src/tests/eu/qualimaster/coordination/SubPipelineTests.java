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
import eu.qualimaster.coordination.StormUtils.TopologyTestInfo;
import eu.qualimaster.coordination.commands.AlgorithmChangeCommand;
import eu.qualimaster.coordination.commands.PipelineCommand;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent.Status;
import tests.eu.qualimaster.storm.Naming;
import tests.eu.qualimaster.storm.Process;
import tests.eu.qualimaster.storm.ReceivingSpout;
import tests.eu.qualimaster.storm.SendingBolt;
import tests.eu.qualimaster.storm.Sink;
import tests.eu.qualimaster.storm.Source;
import tests.eu.qualimaster.storm.Src;

/**
 * Coordination manager tests for decomposed pipelines. Set environment variable 
 * "STORM_TEST_TIMEOUT_MS" to a value greater than 15.000 (ms).
 */
public class SubPipelineTests extends AbstractCoordinationTests {

    private static final String SUB_RECEIVER = "SubReceiver";
    private static final String SUB_SENDER = "SubSender";
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
        // we have no monitoring layer - jump over lifecycle
        handler = new PipelineLifecycleEventHandler(PipelineLifecycleEvent.Status.CHECKING, 
            // CHECKED->STARTING by Coordination Layer
            PipelineLifecycleEvent.Status.STARTING, PipelineLifecycleEvent.Status.INITIALIZED);
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
     * Creates and registers the main topology.
     * 
     * @param topologies the topologies structure to register within
     * @param config the configuration settings
     */
    @SuppressWarnings("rawtypes")
    private void registerMainTopology(Map<String, TopologyTestInfo> topologies, Map config) {
        RecordingTopologyBuilder builder = new RecordingTopologyBuilder();

        final String senderName = "MainSender";
        final String receiverName = "MainReceiver";
        
        Source<Src> source = new Source<Src>(Src.class, Naming.PIPELINE_NAME);
        builder.setSpout(Naming.NODE_SOURCE, source, 1).setNumTasks(1);
        // emulate family, here we just put the sending bolt after -> algorithm change
        Process process = new Process(Naming.NODE_PROCESS, Naming.PIPELINE_NAME);
        builder.setBolt(Naming.NODE_PROCESS, process, 1).setNumTasks(3).shuffleGrouping(Naming.NODE_SOURCE);
        SendingBolt sender = new SendingBolt(senderName, Naming.PIPELINE_NAME, true, false, 9000);
        builder.setBolt(senderName, sender, 1).setNumTasks(1).shuffleGrouping(Naming.NODE_PROCESS);
        
        ReceivingSpout receiver = new ReceivingSpout(receiverName, Naming.PIPELINE_NAME, true, false, 9001);
        builder.setSpout(receiverName, receiver);
        Sink sink = new Sink(Naming.PIPELINE_NAME);
        builder.setBolt(Naming.NODE_SINK, sink, 1).setNumTasks(1).shuffleGrouping(receiverName);

        StormTopology topology = builder.createTopology();
        builder.close(Naming.PIPELINE_NAME, config);
        topologies.put(Naming.PIPELINE_NAME, new TopologyTestInfo(topology, 
            new File(Utils.getTestdataDir(), "sub/mainPipeline.xml"), config));
    }

    /**
     * Creates and registers the sub topology.
     * 
     * @param topologies the topologies structure to register within
     * @param config the configuration settings
     */
    @SuppressWarnings("rawtypes")
    private void registerSubTopology(Map<String, TopologyTestInfo> topologies, Map config) {
        RecordingTopologyBuilder builder = new RecordingTopologyBuilder();

        final String sourceName = SUB_RECEIVER;
        final String sinkName = SUB_SENDER;
        
        ReceivingSpout source = new ReceivingSpout(sourceName, Naming.SUB_PIPELINE_NAME, true, false, 9000);
        builder.setSpout(sourceName, source, 1).setNumTasks(1);
        SendingBolt end = new SendingBolt(sinkName, Naming.SUB_PIPELINE_NAME, true, false, 9001);
        builder.setBolt(sinkName, end, 1).setNumTasks(1).shuffleGrouping(sourceName);

        StormTopology subTopology = builder.createTopology();
        builder.close(Naming.SUB_PIPELINE_NAME, config);
        topologies.put(Naming.SUB_PIPELINE_NAME, new TopologyTestInfo(subTopology, 
            new File(Utils.getTestdataDir(), "sub/subPipeline.xml"), config)); // ignore settings, shall come from infra
    }
    
    /**
     * Tests sub-pipelines.
     */
    @Test
    public void testSubpipeline() {
        LocalStormEnvironment env = new LocalStormEnvironment();
        @SuppressWarnings("rawtypes")
        Map config = createTopologyConfiguration();

        Map<String, TopologyTestInfo> topologies = new HashMap<String, TopologyTestInfo>();
        registerMainTopology(topologies, config);
        registerSubTopology(topologies, config);
        env.setTopologies(topologies);
        clear();
        
        new PipelineCommand(Naming.PIPELINE_NAME, PipelineCommand.Status.START).execute();

        sleep(1000);
        getPipelineStatusTracker().waitFor(Naming.PIPELINE_NAME, Status.CREATED, 5000);
        sleep(5000);
        // we have no monitoring layer - let nodes come up
        EventManager.send(new PipelineLifecycleEvent(Naming.PIPELINE_NAME, 
            PipelineLifecycleEvent.Status.INITIALIZED, null));

        AlgorithmChangeCommand cmd = new AlgorithmChangeCommand(Naming.PIPELINE_NAME, Naming.NODE_PROCESS, 
            Naming.NODE_PROCESS_ALG2);
        EventManager.send(cmd);

        sleep(1000);
        getPipelineStatusTracker().waitFor(Naming.SUB_PIPELINE_NAME, Status.CREATED, 5000);
        sleep(5000);
        EventManager.send(new PipelineLifecycleEvent(Naming.SUB_PIPELINE_NAME, 
            PipelineLifecycleEvent.Status.INITIALIZED, null));

        sleep(5000); // let Storm run for a while
        
        new PipelineCommand(Naming.PIPELINE_NAME, PipelineCommand.Status.STOP).execute();
        sleep(4000);

        env.shutdown();
        env.cleanup();
    }

}
