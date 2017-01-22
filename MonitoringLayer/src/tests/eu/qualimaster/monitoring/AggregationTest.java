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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import eu.qualimaster.coordination.CoordinationManager;
import eu.qualimaster.coordination.INameMapping;
import eu.qualimaster.coordination.NameMapping;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.monitoring.MonitoringManager;
import eu.qualimaster.monitoring.events.AlgorithmChangedMonitoringEvent;
import eu.qualimaster.monitoring.events.ComponentKey;
import eu.qualimaster.monitoring.events.PipelineElementMultiObservationMonitoringEvent;
import eu.qualimaster.monitoring.events.SubTopologyMonitoringEvent;
import eu.qualimaster.monitoring.systemState.NodeImplementationSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineSystemPart;
import eu.qualimaster.monitoring.systemState.StateUtils;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.monitoring.topology.PipelineTopology;
import eu.qualimaster.monitoring.topology.PipelineTopology.Processor;
import eu.qualimaster.monitoring.topology.PipelineTopology.Stream;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.ResourceUsage;
import eu.qualimaster.observables.Scalability;
import eu.qualimaster.observables.TimeBehavior;
import tests.eu.qualimaster.coordination.AbstractCoordinationTests;
import tests.eu.qualimaster.coordination.Utils;
import tests.eu.qualimaster.monitoring.LogTest.Proc;
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
        INameMapping mapping = registerMapping(PIPELINE_NAME, "testAggSimple.xml");
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
        SystemStateTest.assertEquals(items, alg, Scalability.ITEMS, 10); // different time frames, jenkins
        SystemStateTest.assertEquals(items, node, Scalability.ITEMS, 10);
        
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
        INameMapping mapping = registerMapping(PIPELINE_NAME, "testAggSub.xml");
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
     * Splits a sub-topology monitoring event string into a list.
     * 
     * @param string the string to be splitted
     * @return the splitted list
     */
    private static List<String> split(String string) {
        String[] tmp = string.split(", ");
        ArrayList<String> result = new ArrayList<String>();
        for (String t : tmp) {
            result.add(t);
        }
        return result;
    }

    /**
     * Tests one-side integrated sub-algorithms (hand-crafted).
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testOneSideIntegratedSubAlgorithm() throws IOException {
        final String pipelineName = "RandomPip";
        final String processorName = "processor";
        final String alg1Name = "RandomProcessor1";
        final String proc1Name = "RandomProcessor1processor1";
        final String alg2Name = "RandomProcessor2";
        final String proc2Name = "RandomProcessor2processor1";

        INameMapping mapping = registerMapping(pipelineName, "oneSideMapping.xml");
        Map<String, List<String>> structure = new HashMap<String, List<String>>();
        structure.put(alg1Name, split(proc1Name + ";eu.qualimaster.algorithms.Process1Bolt"));
        structure.put(alg2Name, split(proc2Name + ";eu.qualimaster.algorithms.Process2Bolt"));
        mapping.considerSubStructures(new SubTopologyMonitoringEvent(pipelineName, structure, null));
        SystemState state = MonitoringManager.getSystemState();
        PipelineSystemPart pipeline = state.obtainPipeline(pipelineName);
        pipeline.setTopology(createOneSideRandomPipTopo());
        pipeline.changeStatus(PipelineLifecycleEvent.Status.STARTING, false, null);
        ComponentKey keyProc1 = new ComponentKey("host-1", 6073, 1);
        ComponentKey keyProc2 = new ComponentKey("host-2", 6073, 2);
        ComponentKey keyProc = new ComponentKey("host-3", 6073, 3);
        IObservable[] observables = new IObservable[] {TimeBehavior.THROUGHPUT_ITEMS, Scalability.ITEMS, 
            TimeBehavior.THROUGHPUT_VOLUME, TimeBehavior.LATENCY, ResourceUsage.CAPACITY, ResourceUsage.USED_MEMORY};
        String[] nodes = new String[] {processorName, proc1Name};
        sendAlgorithmChangedEvent(pipelineName, processorName, alg1Name);
        sendObservationEvent(pipelineName, processorName, keyProc, 10, 0.01, 500, 11);
        sleepAndPrint(500, pipeline, nodes, observables);
        
        sendObservationEvent(pipelineName, processorName, keyProc, 20, 0.02, 600, 21);
        sendObservationEvent(pipelineName, proc1Name, keyProc1, 5, 0.05, 100, 6);
        sleepAndPrint(500, pipeline, nodes, observables);
        sendObservationEvent(pipelineName, processorName, keyProc, 30, 0.02, 600, 31);
        sendObservationEvent(pipelineName, proc1Name, keyProc1, 15, 0.05, 200, 16);
        sleepAndPrint(500, pipeline, nodes, observables);
        
        PipelineNodeSystemPart procNode = pipeline.getNode(processorName);
        PipelineNodeSystemPart proc1Node = pipeline.getNode(proc1Name);
        SystemStateTest.assertEquals(15, procNode, TimeBehavior.THROUGHPUT_ITEMS); // sink propagated
        SystemStateTest.assertEquals(16, procNode, TimeBehavior.THROUGHPUT_VOLUME); // sink propagated
        SystemStateTest.assertEquals(0.07, procNode, TimeBehavior.LATENCY); // topology sum aggregated
        SystemStateTest.assertEquals(800, procNode, ResourceUsage.USED_MEMORY); // topology sum aggregated
        SystemStateTest.assertEquals(15, proc1Node, TimeBehavior.THROUGHPUT_ITEMS);
        
        System.out.println("Algorithm change");
        sendAlgorithmChangedEvent(pipelineName, processorName, alg2Name);
        sleepAndPrint(500, pipeline, nodes, observables);
        // keep with 0 for testing
        sleepAndPrint(500, pipeline, nodes, observables);
        sleepAndPrint(500, pipeline, nodes, observables);
        sendObservationEvent(pipelineName, processorName, keyProc, 35, 0.02, 600, 36);
        sendObservationEvent(pipelineName, proc2Name, keyProc2, 5, 0.05, 50, 6);
        sleepAndPrint(500, pipeline, nodes, observables);
        sendObservationEvent(pipelineName, processorName, keyProc, 45, 0.02, 600, 46);
        sendObservationEvent(pipelineName, proc2Name, keyProc2, 15, 0.05, 50, 16);
        sleepAndPrint(500, pipeline, nodes, observables);
        sendObservationEvent(pipelineName, processorName, keyProc, 55, 0.02, 600, 56);
        sendObservationEvent(pipelineName, proc2Name, keyProc2, 25, 0.08, 50, 26);
        sleepAndPrint(500, pipeline, nodes, observables);

        PipelineNodeSystemPart proc2Node = pipeline.getNode(proc2Name);
        SystemStateTest.assertEquals(25, procNode, TimeBehavior.THROUGHPUT_ITEMS); // sink propagated
        SystemStateTest.assertEquals(26, procNode, TimeBehavior.THROUGHPUT_VOLUME); // sink propagated
        SystemStateTest.assertEquals(0.1, procNode, TimeBehavior.LATENCY); // topology sum aggregated
        SystemStateTest.assertEquals(650, procNode, ResourceUsage.USED_MEMORY); // topology sum aggregated
        SystemStateTest.assertEquals(25, proc2Node, TimeBehavior.THROUGHPUT_ITEMS);

        state.clear();
        CoordinationManager.unregisterNameMapping(mapping);
    }
    
    /**
     * Sleeps for <code>ms</code> and prints then the results of <code>pipeline</code>.
     * 
     * @param ms the ms to sleep
     * @param pipeline the pipeline to report on
     * @param nodes the nodes in pipelines
     * @param observables the observables
     */
    private void sleepAndPrint(int ms, PipelineSystemPart pipeline, String[] nodes, IObservable[] observables) {
        AbstractCoordinationTests.sleep(500);
        //System.out.println(pipeline.format("") + "\n");
        for (int n = 0; n < nodes.length; n++) {
            PipelineNodeSystemPart node = pipeline.getNode(nodes[n]);
            System.out.print(node.getName());
            for (int o = 0; o < observables.length; o++) {
                System.out.print(" ");
                System.out.print(observables[o]);
                System.out.print(" ");
                System.out.printf("%.2f", node.getObservedValue(observables[o]));
            }
            System.out.print(" ");
        }
        System.out.println();
    }
    
    // checkstyle: stop parameter number check
    
    /**
     * Sends an observation monitoring event.
     * 
     * @param pipeline the pipeline
     * @param pipelineElement the pipeline element
     * @param key the component key
     * @param throughput the actual throughput
     * @param latency the actual latency
     * @param memory the actual memory consumption
     * @param volume the actual volume
     */
    private void sendObservationEvent(String pipeline, String pipelineElement, ComponentKey key, 
        double throughput, double latency, double memory, double volume) {
        Map<IObservable, Double> observations = new HashMap<>();
        observations.put(TimeBehavior.THROUGHPUT_ITEMS, throughput);
        observations.put(TimeBehavior.LATENCY, latency);
        observations.put(ResourceUsage.USED_MEMORY, memory);
        observations.put(TimeBehavior.THROUGHPUT_VOLUME, volume);
        MonitoringManager.handleEvent(new PipelineElementMultiObservationMonitoringEvent(pipeline, 
            pipelineElement, key, observations));
    }

    // checkstyle: resume parameter number check

    /**
     * Sends an algorithm change event as if an algorithm has been changed.
     * 
     * @param pipeline the pipeline
     * @param pipelineElement the pipeline element
     * @param algorithm the algorithm
     */
    private void sendAlgorithmChangedEvent(String pipeline, String pipelineElement, String algorithm) {
        MonitoringManager.handleEvent(new AlgorithmChangedMonitoringEvent(pipeline, pipelineElement, algorithm));
    }
    
    /**
     * Creates a monitoring topology for the 1-sided integrated random pipeline.
     * 
     * @return the topology
     */
    static PipelineTopology createOneSideRandomPipTopo() {
        final List<Processor> processors = new ArrayList<Processor>();
        Proc src = new Proc("src", 1, new int[]{4}, processors);
        Proc proc = new Proc("processor", 1, new int[]{3}, processors);
        Proc p1i = new Proc("RandomProcessor1Intermediary", 1, new int[]{1}, processors);
        Proc p1p = new Proc("RandomProcessor1processor1", 1, new int[]{2}, processors);
        Proc p1e = new Proc("RandomProcessor1EndBolt", 1, new int[]{2}, processors);
        Proc p2i = new Proc("RandomProcessor2Intermediary", 1, new int[]{1}, processors);
        Proc p2p = new Proc("RandomProcessor2processor1", 1, new int[]{2}, processors);
        Proc p2e = new Proc("RandomProcessor2EndBolt", 1, new int[]{2}, processors);
        Proc snk = new Proc("snk", 1, new int[]{4}, processors);
        Stream srcproc = new Stream("", src, proc);
        Stream procp1i = new Stream("", proc, p1i);
        Stream p1ip1p = new Stream("", p1i, p1p);
        Stream p1pp1e = new Stream("", p1p, p1e);
        Stream procp2i = new Stream("", proc, p2i);
        Stream p2ip2p = new Stream("", p2i, p2p);
        Stream p2pp2e = new Stream("", p2p, p2e);        
        Stream p1esnk = new Stream("", p1e, snk);
        Stream p2esnk = new Stream("", p1e, snk);
        src.setOutputs(srcproc);
        proc.setInputs(srcproc);
        proc.setOutputs(procp1i, procp2i);
        p1i.setInputs(procp1i);
        p2i.setInputs(procp2i);
        p1i.setOutputs(p1ip1p);
        p2i.setOutputs(p2ip2p);
        p1p.setInputs(p1ip1p);
        p2p.setInputs(p2ip2p);
        p1p.setOutputs(p1pp1e);
        p2p.setOutputs(p2pp2e);        
        p1e.setInputs(p1pp1e);
        p2e.setInputs(p2pp2e);
        p1e.setOutputs(p1esnk);
        p2e.setOutputs(p2esnk);
        snk.setInputs(p1esnk, p2esnk);
        return new PipelineTopology(processors);
    }
    
    /**
     * Loads and registers the given name mapping.
     * 
     * @param pipelineName the name of the pipeline to load
     * @param fileName the file name within folder testdata/aggregation
     * @return the name mapping
     * @throws IOException if loading the mapping file fails, shall not occur
     */
    private INameMapping registerMapping(String pipelineName, String fileName) throws IOException {
        FileInputStream fin = new FileInputStream(new File(Utils.getTestdataDir(), "aggregation/" + fileName));
        NameMapping mapping = new NameMapping(pipelineName, fin);
        fin.close();
        CoordinationManager.registerTestMapping(mapping);
        return mapping;
    }

}
