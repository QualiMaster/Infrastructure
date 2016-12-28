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
package tests.eu.qualimaster.monitoring;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import eu.qualimaster.coordination.CoordinationManager;
import eu.qualimaster.coordination.INameMapping;
import eu.qualimaster.coordination.NameMapping;
import eu.qualimaster.monitoring.systemState.NodeImplementationSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineSystemPart;
import eu.qualimaster.monitoring.systemState.StateUtils;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.monitoring.topology.PipelineTopology;
import eu.qualimaster.monitoring.topology.PipelineTopology.Processor;
import eu.qualimaster.monitoring.topology.PipelineTopology.Stream;
import eu.qualimaster.observables.Scalability;
import eu.qualimaster.observables.TimeBehavior;
import tests.eu.qualimaster.coordination.Utils;
import tests.eu.qualimaster.monitoring.genTopo.TestProcessor;

/**
 * Some system state tests.
 * 
 * @author Holger Eichelberger
 */
public class AggregationTest {

    private static final String PIPELINE_NAME = "pip";
    private static final String PROCESSOR_NODE_NAME = "processor";

    /**
     * Tests algorithm aggregation for items on a simple Java algorithm. Monitoring happens in pipeline node and 
     * is propagated to algorithm.
     * 
     * @throws InterruptedException shall not occur
     * @throws IOException if loading the mapping file fails, shall not occur
     */
    @Test
    public void testAggregateAlgorithm() throws InterruptedException, IOException {
        INameMapping mapping = registerMapping("testAggSimple.xml");
        TestProcessor p = new TestProcessor(PROCESSOR_NODE_NAME);
        List<Processor> procs = new ArrayList<Processor>();
        procs.add(p);

        SystemState state = new SystemState();
        PipelineSystemPart pip = state.obtainPipeline(PIPELINE_NAME);
        PipelineNodeSystemPart node = pip.obtainPipelineNode(PROCESSOR_NODE_NAME);
        pip.setTopology(new PipelineTopology(procs));
        NodeImplementationSystemPart alg = pip.getAlgorithm("myAlg");
        node.setCurrent(alg);

        long start = System.currentTimeMillis();
        StateUtils.setValue(node, TimeBehavior.THROUGHPUT_ITEMS, 100, null);
        Thread.sleep(1000);
        StateUtils.setValue(node, TimeBehavior.THROUGHPUT_ITEMS, 200, null);
        Thread.sleep(1000);
        double allItems = 300;
        StateUtils.setValue(node, TimeBehavior.THROUGHPUT_ITEMS, allItems, null);
        double items = allItems / ((double) System.currentTimeMillis() - start) * 1000;
        
        SystemStateTest.assertEquals(allItems, alg, TimeBehavior.THROUGHPUT_ITEMS);
        SystemStateTest.assertEquals(allItems, node, TimeBehavior.THROUGHPUT_ITEMS);
        SystemStateTest.assertEquals(items, alg, Scalability.ITEMS, 5); // different time frames
        SystemStateTest.assertEquals(items, node, Scalability.ITEMS, 5);
        
        // copy and cause different state -> links
        SystemState copy = new SystemState(state);
        StateUtils.setValue(node, TimeBehavior.THROUGHPUT_ITEMS, 1000, null);
        PipelineSystemPart pipC = copy.obtainPipeline(PIPELINE_NAME);
        PipelineNodeSystemPart nodeC = pipC.obtainPipelineNode(PROCESSOR_NODE_NAME);
        NodeImplementationSystemPart algC = pipC.getAlgorithm("myAlg");
        SystemStateTest.assertEquals(allItems, algC, TimeBehavior.THROUGHPUT_ITEMS);
        SystemStateTest.assertEquals(allItems, nodeC, TimeBehavior.THROUGHPUT_ITEMS);
        SystemStateTest.assertEquals(items, algC, Scalability.ITEMS, 2);
        SystemStateTest.assertEquals(items, nodeC, Scalability.ITEMS, 2);

        //change algorithm
        NodeImplementationSystemPart alg1 = pip.getAlgorithm("myAlg1");
        node.setCurrent(alg1);
        StateUtils.setValue(node, TimeBehavior.THROUGHPUT_ITEMS, 500, null);
        Thread.sleep(1000);
        StateUtils.setValue(node, TimeBehavior.THROUGHPUT_ITEMS, 500, null);
        double allThrough1 = 500;
        double items1 = 500;
        SystemStateTest.assertEquals(allThrough1, alg1, TimeBehavior.THROUGHPUT_ITEMS);
        SystemStateTest.assertEquals(allThrough1, node, TimeBehavior.THROUGHPUT_ITEMS);
        SystemStateTest.assertEquals(items1, alg1, Scalability.ITEMS, 5);
        SystemStateTest.assertEquals(items1, node, Scalability.ITEMS, 5);
        
        CoordinationManager.unregisterNameMapping(mapping);
    }

    /**
     * Tests algorithm aggregation for items on a complex algorithm with nodes. Monitoring happens in pipeline node and 
     * in nodes of algorithm.
     * 
     * @throws InterruptedException shall not occur
     * @throws IOException if loading the mapping file fails, shall not occur
     */
    @Test
    public void testAggregateAlgorithmSub() throws InterruptedException, IOException {
        INameMapping mapping = registerMapping("testAggSub.xml");
        TestProcessor p = new TestProcessor(PROCESSOR_NODE_NAME);
        TestProcessor pn = new TestProcessor("algNode");
        TestProcessor pn1 = new TestProcessor("algNode1");
        Stream s = new Stream("", p, pn);
        p.setOutputs(s);
        p.setInputs(s);
        Stream s1 = new Stream("", p, pn1);
        p.setOutputs(s1);
        p.setInputs(s1);
        List<Processor> procs = new ArrayList<Processor>();
        procs.add(p);
        procs.add(pn);
        procs.add(pn1);

        SystemState state = new SystemState();
        PipelineSystemPart pip = state.obtainPipeline(PIPELINE_NAME);
        PipelineNodeSystemPart node = pip.obtainPipelineNode(PROCESSOR_NODE_NAME);
        pip.setTopology(new PipelineTopology(procs));
        NodeImplementationSystemPart alg = pip.getAlgorithm("myAlg");
        PipelineNodeSystemPart algNode = alg.obtainPipelineNode("algNode");
        node.setCurrent(alg);
        long start = System.currentTimeMillis();
        StateUtils.setValue(algNode, TimeBehavior.THROUGHPUT_ITEMS, 100, null);
        Thread.sleep(1000);
        StateUtils.setValue(algNode, TimeBehavior.THROUGHPUT_ITEMS, 200, null);
        Thread.sleep(1000);
        double allThrough = 300;
        StateUtils.setValue(algNode, TimeBehavior.THROUGHPUT_ITEMS, allThrough, null);
        double items = allThrough / ((double) System.currentTimeMillis() - start) * 1000;
        SystemStateTest.assertEquals(allThrough, algNode, TimeBehavior.THROUGHPUT_ITEMS);
        SystemStateTest.assertEquals(allThrough, alg, TimeBehavior.THROUGHPUT_ITEMS);
        SystemStateTest.assertEquals(allThrough, node, TimeBehavior.THROUGHPUT_ITEMS);
        SystemStateTest.assertEquals(items, algNode, Scalability.ITEMS, 5);
        SystemStateTest.assertEquals(items, alg, Scalability.ITEMS, 5);
        SystemStateTest.assertEquals(items, node, Scalability.ITEMS, 5);
        
        // copy and cause different state -> links
        SystemState copy = new SystemState(state);
        StateUtils.setValue(algNode, TimeBehavior.THROUGHPUT_ITEMS, 1000, null);
        PipelineSystemPart pipC = copy.obtainPipeline(PIPELINE_NAME);
        PipelineNodeSystemPart nodeC = pipC.obtainPipelineNode(PROCESSOR_NODE_NAME);
        NodeImplementationSystemPart algC = pipC.getAlgorithm("myAlg");
        PipelineNodeSystemPart algNodeC = algC.obtainPipelineNode("algNode");
        SystemStateTest.assertEquals(allThrough, algNodeC, TimeBehavior.THROUGHPUT_ITEMS);
        SystemStateTest.assertEquals(allThrough, algC, TimeBehavior.THROUGHPUT_ITEMS);
        SystemStateTest.assertEquals(allThrough, nodeC, TimeBehavior.THROUGHPUT_ITEMS);
        SystemStateTest.assertEquals(items, algNodeC, Scalability.ITEMS, 5);
        SystemStateTest.assertEquals(items, algC, Scalability.ITEMS, 5);
        SystemStateTest.assertEquals(items, nodeC, Scalability.ITEMS, 5);
        CoordinationManager.unregisterNameMapping(mapping);

        // switch algorithm
        NodeImplementationSystemPart alg1 = pip.getAlgorithm("myAlg1");
        PipelineNodeSystemPart algNode1 = alg1.obtainPipelineNode("algNode1");
        node.setCurrent(alg1);
        StateUtils.setValue(algNode1, TimeBehavior.THROUGHPUT_ITEMS, 500, null);
        Thread.sleep(1000);
        StateUtils.setValue(algNode1, TimeBehavior.THROUGHPUT_ITEMS, 500, null);
        double allThrough1 = 500;
        double items1 = 500;
        SystemStateTest.assertEquals(allThrough1, algNode1, TimeBehavior.THROUGHPUT_ITEMS);
        SystemStateTest.assertEquals(allThrough1, alg1, TimeBehavior.THROUGHPUT_ITEMS);
        SystemStateTest.assertEquals(allThrough1, node, TimeBehavior.THROUGHPUT_ITEMS);
        SystemStateTest.assertEquals(items1, algNode1, Scalability.ITEMS, 5);
        SystemStateTest.assertEquals(items1, alg1, Scalability.ITEMS, 5);
        SystemStateTest.assertEquals(items1, node, Scalability.ITEMS, 5);
    }
    
    /**
     * Loads and registers the given name mapping.
     * 
     * @param fileName the file name within folder testdata/aggregation
     * @return the name mapping
     * @throws IOException if loading the mapping file fails, shall not occur
     */
    private INameMapping registerMapping(String fileName) throws IOException {
        FileInputStream fin = new FileInputStream(new File(Utils.getTestdataDir(), "aggregation/" + fileName));
        NameMapping mapping = new NameMapping(PIPELINE_NAME, fin);
        fin.close();
        CoordinationManager.registerTestMapping(mapping);
        return mapping;
    }

}
