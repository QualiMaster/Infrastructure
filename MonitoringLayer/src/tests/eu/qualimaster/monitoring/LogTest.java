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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.qualimaster.coordination.CoordinationManager;
import eu.qualimaster.coordination.NameMapping;
import eu.qualimaster.coordination.events.AlgorithmProfilingEvent.DetailMode;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.monitoring.MonitoringManager;
import eu.qualimaster.monitoring.events.MonitoringEvent;
import eu.qualimaster.monitoring.events.SubTopologyMonitoringEvent;
import eu.qualimaster.monitoring.storm.PipelineStatistics;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineSystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.monitoring.topology.PipelineTopology;
import eu.qualimaster.monitoring.topology.PipelineTopology.Processor;
import eu.qualimaster.monitoring.topology.PipelineTopology.Stream;
import eu.qualimaster.monitoring.tracing.Tracing;
import eu.qualimaster.monitoring.tracing.TracingTask;
import eu.qualimaster.observables.ResourceUsage;
import tests.eu.qualimaster.coordination.Utils;
import tests.eu.qualimaster.logReader.LogReader;
import tests.eu.qualimaster.logReader.LogReader.EventProcessor;

/**
 * Performs tests of the monitoring layer based on infrastructure logs.
 * 
 * @author Holger Eichelberger
 */
public class LogTest {

    private static final String SWITCHPIP_NAME = "SwitchPip";

    /**
     * Executed before a single test.
     */
    @Before
    public void setUp() {
        MonitoringManager.start(false);
    }
    
    /**
     * Executed after a single test.
     */
    @After
    public void tearDown() {
        MonitoringManager.stop();
        MonitoringManager.clearState();
    }
    
    /**
     * Sets up a switch pipeline test environment.
     * 
     * @param maxEventCount the maximum number of events to be parsed and processed
     * @param processor the event processor
     * @return the log reader for further processing (close the reader finally)
     * @throws IOException in case of an I/O problem
     */
    private LogReader setupSwitchPip(int maxEventCount, EventProcessor<?> processor) throws IOException {
        File folder = new File(Utils.getTestdataDir(), "switchPip");
        FileInputStream mappingFile = new FileInputStream(new File(folder, "mapping.xml"));
        NameMapping nameMapping = new NameMapping(SWITCHPIP_NAME, mappingFile);
        CoordinationManager.registerTestMapping(nameMapping);

        // fake the start
        SystemState state = MonitoringManager.getSystemState();
        PipelineSystemPart pipeline = state.obtainPipeline(SWITCHPIP_NAME);
        pipeline.changeStatus(PipelineLifecycleEvent.Status.STARTING, false, null);

        // fake topology creation via RecordingTopologyBuilder - currently not supported by LogReader
        final String sep = SubTopologyMonitoringEvent.SEPARATOR;
        Map<String, List<String>> structure = new HashMap<String, List<String>>();
        put(structure, "SimpleStateTransferSW", 
            "Switch1IntermediarySpout" + sep + "eu.qualimaster.test.algorithms.IntermediarySpoutSW", 
            "Switch1ProcessBolt" + sep + "eu.qualimaster.test.algorithms.ProcessBolt");
        put(structure, "SimpleStateTransferSW2", 
            "Switch2IntermediarySpout" + sep + "eu.qualimaster.test.algorithms.IntermediarySpoutSW2", 
            "Switch2ProcessBolt" + sep + "eu.qualimaster.test.algorithms.ProcessBoltSW2");
        SubTopologyMonitoringEvent stMonEvt = new SubTopologyMonitoringEvent(SWITCHPIP_NAME, structure);
        MonitoringManager.handleEvent(stMonEvt);
        
        LogReader reader = new LogReader(new File(folder, "qm.log"), processor);
        reader.setErr(null);
        reader.read(maxEventCount);
        
        PipelineStatistics pipStat = new PipelineStatistics(pipeline);
        for (PipelineNodeSystemPart node : pipeline.getNodes()) {
            pipStat.collect(node);
        }
        pipStat.commit();
        return reader;
    }
    
    /**
     * Tests the correct processing of <code>subTopology.log</code>.
     * 
     * @throws IOException in case of an I/O problem
     */
    @Test
    public void switchPipLogTest() throws IOException {
        final int maxEventCount = 0; // 0 = all, 40 = initial testing
        LogReader reader = setupSwitchPip(maxEventCount, new EventProcessor<MonitoringEvent>(MonitoringEvent.class) {

            @Override
            protected void process(MonitoringEvent event) {
                MonitoringManager.handleEvent(event); // just local, not via Event bus!
            }
        });

        SystemState state = MonitoringManager.getSystemState();
        PipelineSystemPart pipeline = state.obtainPipeline(SWITCHPIP_NAME);

        PipelineNodeSystemPart src = pipeline.obtainPipelineNode("src");
        PipelineNodeSystemPart processor = pipeline.obtainPipelineNode("processor");
        PipelineNodeSystemPart snk = pipeline.obtainPipelineNode("snk");

        //System.err.println(MonitoringManager.getSystemState().format());

        SystemStateTest.assertEquals(1, src, ResourceUsage.TASKS);
        SystemStateTest.assertEquals(1, src, ResourceUsage.EXECUTORS);
        // 1 family, 2 alg1, 2 alg2
        SystemStateTest.assertEquals(1 + 2 + 2, processor, ResourceUsage.TASKS); 
        SystemStateTest.assertEquals(1 + 2 + 2, processor, ResourceUsage.EXECUTORS);
        SystemStateTest.assertEquals(1, snk, ResourceUsage.TASKS);
        SystemStateTest.assertEquals(1, snk, ResourceUsage.EXECUTORS);
        // more is allocated than actually used
        SystemStateTest.assertEquals(7, pipeline, ResourceUsage.TASKS);
        SystemStateTest.assertEquals(7, pipeline, ResourceUsage.EXECUTORS);
        
        reader.close();
    }
    
    /**
     * Just a refined processor for building up the internal topology.
     * 
     * @author Holger Eichelberger
     */
    private static class Proc extends Processor {

        /**
         * Creates a processor.
         * 
         * @param name the name
         * @param parallelization the parallelization degree
         * @param tasks the tasks
         * @param procs the list of processors to add this to
         */
        public Proc(String name, int parallelization, int[] tasks, List<Processor> procs) {
            super(name, parallelization, tasks);
            procs.add(this);
        }

        /**
         * Defines the inputs.
         * 
         * @param inputs the inputs
         */
        protected void setInputs(Stream... inputs) {
            List<Stream> tmp = new ArrayList<Stream>();
            for (Stream s : inputs) {
                tmp.add(s);
            }
            super.setInputs(tmp);
        }

        /**
         * Defines the outputs.
         * 
         * @param outputs the outputs
         */
        protected void setOutputs(Stream... outputs) {
            List<Stream> tmp = new ArrayList<Stream>();
            for (Stream s : outputs) {
                tmp.add(s);
            }
            super.setOutputs(tmp);
        }

    }
    
    /**
     * Just a manual trace test.
     * 
     * @param args the arguments
     * @throws IOException in case of I/O problems
     */
    public static void main(String[] args) throws IOException {
        final String pipName = "TestPip1472558036517";
        final int maxEventCount = 100; // 0 = all, 40 = initial testing
        File folder = new File(Utils.getTestdataDir(), "profileHw");
        FileInputStream mappingFile = new FileInputStream(new File(folder, "mapping.xml"));
        final NameMapping nameMapping = new NameMapping(pipName, mappingFile);
        CoordinationManager.registerTestMapping(nameMapping);
        
        // fake the start
        SystemState state = MonitoringManager.getSystemState();
        final PipelineSystemPart pipeline = state.obtainPipeline(pipName);
        final List<Processor> processors = new ArrayList<Processor>();
        Proc src = new Proc("TestSource", 1, new int[]{4}, processors);
        Proc fam = new Proc("TestFamily", 1, new int[]{3}, processors);
        Proc cb = new Proc("GenTopoHardwareCorrelationFinancialHardwareConnectionBolt", 1, new int[]{1}, processors);
        Proc cs = new Proc("GenTopoHardwareCorrelationFinancialHardwareConnectionSpout", 1, new int[]{2}, processors);
        Stream srcfam1 = new Stream("TestSourceSymbolList", src, fam);
        Stream srcfam2 = new Stream("TestSourcePreprocessedStream", src, fam);
        Stream famcm = new Stream("TestFamilyGenTopoHardwareCorrelationFinancial", fam, cb);
        Stream cmchy = new Stream("", cb, cs);
        src.setOutputs(srcfam1, srcfam2);
        fam.setInputs(srcfam1, srcfam2);
        fam.setOutputs(famcm);
        cb.setInputs(famcm);
        cb.setOutputs(cmchy);
        cs.setInputs(cmchy);
        PipelineTopology topo = new PipelineTopology(processors);
        System.out.println("TOPOLOGY " + topo);
        pipeline.setTopology(topo);
        pipeline.changeStatus(PipelineLifecycleEvent.Status.STARTING, false, null);
        Timer timer = new Timer();
        final TracingTask ttask = new TracingTask(null);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                PipelineStatistics pStat = new PipelineStatistics(pipeline);
                for (Processor p : processors) {
                    PipelineNodeSystemPart nodePart = SystemState.getNodePart(nameMapping, pipeline, p.getName());
                    pStat.collect(nodePart);
                }
                pStat.commit();
                ttask.run();
            }
        }, 0, 1000);
        Tracing.test(pipName, "TestFamily", "GenTopoHardwareCorrelationFinancial", System.err, DetailMode.ALGORITHMS);
        
        LogReader reader = new LogReader(new File(folder, "qmInfra.log"), 
            new EventProcessor<MonitoringEvent>(MonitoringEvent.class) {

                @Override
                protected void process(MonitoringEvent event) {
                    MonitoringManager.handleEvent(event); // just local, not via Event bus!
                }
            });
        reader.considerTime(true);
        reader.setDateFormat(new SimpleDateFormat("SSSSSS"));
        reader.setErr(null);
        reader.read(maxEventCount);
        reader.close();
        timer.cancel();
        
        PipelineStatistics pipStat = new PipelineStatistics(pipeline);
        for (PipelineNodeSystemPart node : pipeline.getNodes()) {
            pipStat.collect(node);
        }
        pipStat.commit();
        
        System.out.println(state.getPipeline(pipName).format(""));
    }

    
    /**
     * Puts the <code>componentData</code> for <code>element</code> into <code>structure</code>.
     * 
     * @param structure the structure to be modified as a side effect
     * @param element the element name
     * @param componentData the component data
     */
    private static void put(Map<String, List<String>> structure, String element, String... componentData) {
        List<String> sub = structure.get(element);
        if (null == sub) {
            sub = new ArrayList<String>();
            structure.put(element, sub);
        }
        for (String cmp : componentData) {
            sub.add(cmp);
        }
    }
}
