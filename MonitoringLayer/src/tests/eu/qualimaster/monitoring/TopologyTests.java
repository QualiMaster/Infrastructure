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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import tests.eu.qualimaster.coordination.AbstractCoordinationTests;
import tests.eu.qualimaster.coordination.Utils;
import eu.qualimaster.coordination.CoordinationManager;
import eu.qualimaster.coordination.INameMapping;
import eu.qualimaster.coordination.NameMapping;
import eu.qualimaster.monitoring.events.ComponentKey;
import eu.qualimaster.monitoring.systemState.NodeImplementationSystemPart;
import eu.qualimaster.monitoring.systemState.IAggregationFunction;
import eu.qualimaster.monitoring.systemState.ObservationAggregator;
import eu.qualimaster.monitoring.systemState.ObservationAggregatorFactory;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineSystemPart;
import eu.qualimaster.monitoring.systemState.StatisticsWalker;
import eu.qualimaster.monitoring.systemState.SystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.monitoring.topology.ITopologyVisitor;
import eu.qualimaster.monitoring.topology.PipelineTopology;
import eu.qualimaster.monitoring.topology.PipelineTopology.Processor;
import eu.qualimaster.monitoring.topology.PipelineTopology.Stream;
import eu.qualimaster.monitoring.topology.TopologyWalker;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.ResourceUsage;
import eu.qualimaster.observables.Scalability;
import eu.qualimaster.observables.TimeBehavior;

/**
 * Tests the {@link PipelineTopology}.
 * 
 * @author Holger Eichelberger
 */
public class TopologyTests {

    /**
     * Projects an observed value.
     * 
     * @author Holger Eichelberger
     */
    private interface IValueProjector {

        /**
         * Returns the projected value.
         * 
         * @param part the part
         * @param observable the observable
         * @return the value
         */
        public double getValue(SystemPart part, IObservable observable);
        
    }
    
    private static final IValueProjector VALUE = new IValueProjector() {

        @Override
        public double getValue(SystemPart part, IObservable observable) {
            return part.getObservedValue(observable);
        }
        
        @Override
        public String toString() {
            return "VALUE";
        }
        
    };

    private static final IValueProjector LOCAL_VALUE = new IValueProjector() {

        @Override
        public double getValue(SystemPart part, IObservable observable) {
            return part.getObservedValue(observable, true);
        }

        @Override
        public String toString() {
            return "LOCAL_VALUE";
        }

    };
    
    /**
     * Defines the interface of a path aggregator.
     * 
     * @author Holger Eichelberger
     */
    private interface IPathAggregator {
        
        /**
         * Aggregates <code>parts</code> over <code>observable</code>.
         * 
         * @param observable the observable to obtain the individual values for
         * @param pathAverage whether an average over <code>parts</code> shall be calculated as result
         * @param parts the parts representing the path
         * @param projector projects the observed value
         * @return the aggregated result
         */
        public double aggregate(IObservable observable, boolean pathAverage, IValueProjector projector, 
            SystemPart... parts);
        
    }

    private static final double ASSERT_DELTA = 0.005;
    private static final Map<IAggregationFunction, IPathAggregator> AGGREGATOR_MAPPING 
        = new HashMap<IAggregationFunction, IPathAggregator>();
    
    
    private static final IPathAggregator SUM = new IPathAggregator() {

        @Override
        public double aggregate(IObservable observable, boolean pathAverage, IValueProjector projector, 
            SystemPart... parts) {
            double result = 0;
            int count = 0;
            for (SystemPart part : parts) {
                if (VALUE == projector || part.getComponentCount(observable) > 0) {
                    double obs = projector.getValue(part, observable);
                    result += obs;
                    count++;
                }
            }
            if (pathAverage && count > 0) {
                result /= count;
            }
            return result;
        }
        
        @Override
        public String toString() {
            return "SUM";
        }
        
    };

    private static final IPathAggregator MIN = new IPathAggregator() {

        @Override
        public double aggregate(IObservable observable, boolean pathAverage, IValueProjector projector, 
            SystemPart... parts) {
            double result;
            if (parts.length > 0) {
                result = Double.MAX_VALUE;
                for (SystemPart part : parts) {
                    if (part.getComponentCount(observable) > 0) {
                        double obs = projector.getValue(part, observable);
                        result = Math.min(result, obs);
                    }
                }
                if (pathAverage) {
                    result /= parts.length;
                }
            } else {
                result = 0;
            }
            return result;
        }

        @Override
        public String toString() {
            return "MIN";
        }

    };

    private static final IPathAggregator MAX = new IPathAggregator() {

        @Override
        public double aggregate(IObservable observable, boolean pathAverage, IValueProjector projector, 
            SystemPart... parts) {
            double result;
            if (parts.length > 0) {
                result = Double.MIN_VALUE;
                for (SystemPart part : parts) {
                    if (part.getComponentCount(observable) > 0) {
                        double obs = projector.getValue(part, observable);
                        result = Math.max(result, obs);
                    }
                }
                if (pathAverage) {
                    result /= parts.length;
                }
            } else {
                result = 0;
            }
            return result;
        }
        
        @Override
        public String toString() {
            return "MAX";
        }
        
    };

    /**
     * Specializes the processor for use.
     * 
     * @author Holger Eichelberger
     */
    private static class TestProcessor extends Processor {

        /**
         * Creates a processor without streams, one executor and no tasks. [incremental]
         * 
         * @param name the name of the processor
         */
        protected TestProcessor(String name) {
            super(name, 1, null);
        }
        
        /**
         * Creates a processor without streams. [incremental]
         * 
         * @param name the name of the processor
         * @param parallelization the configured thread parallelization
         * @param tasks the identifiers for the logical parallelization (may be <b>null</b> if not present)
         */
        protected TestProcessor(String name, int parallelization, int[] tasks) {
            super(name, parallelization, tasks);
        }

        @Override
        protected void addInput(Stream input) {
            super.addInput(input);
        }
        
        @Override
        protected void addOutput(Stream output) {
            super.addOutput(output);
        }
        
    }
    
    static {
        AGGREGATOR_MAPPING.put(IAggregationFunction.SUM, SUM);
        AGGREGATOR_MAPPING.put(IAggregationFunction.MAX, MAX);
        AGGREGATOR_MAPPING.put(IAggregationFunction.MIN, MIN);
    }
    
    /**
     * Returns the path aggregator for the element aggegator of <code>oAggregator</code>.
     * 
     * @param aggregator the observation aggregator
     * @return the path aggregator corresponding to {@link ObservationAggregator#getElementAggregator()}
     */
    private static IPathAggregator getPathElementAggregator(ObservationAggregator aggregator) {
        return getPathAggregator(aggregator.getElementAggregator());
    }

    /**
     * Returns the path aggregator for the given aggregation function.
     * 
     * @param aggregator the aggregator to return the mapping for
     * @return the corresponding path aggregator
     */
    private static IPathAggregator getPathAggregator(IAggregationFunction aggregator) {
        IPathAggregator result = AGGREGATOR_MAPPING.get(aggregator);
        Assert.assertNotNull("unknown aggregator mapping", result);
        return result;
    }
    
    /**
     * Some information about a testing topology for doing assertions.
     * 
     * @author Holger Eichelberger
     */
    private static class TopologyInfo {
        private PipelineTopology topo;
        private List<Processor> processors = new ArrayList<Processor>();
        private List<Stream> streams = new ArrayList<Stream>();
        private List<Stream> streamsFwd = new ArrayList<Stream>();
        private List<Processor> enterDepth = new ArrayList<Processor>();
        private List<Processor> exitDepth = new ArrayList<Processor>();
        private List<Processor> endNodes = new ArrayList<Processor>();
        private List<Processor> loopNodes = new ArrayList<Processor>();
        
    }
    
    /**
     * Creates a test topology and asserts its contents.
     * 
     * @return the topology
     */
    private TopologyInfo createTopology() {
        TopologyInfo result = new TopologyInfo();
        
        TestProcessor src = createProcessor(result.processors, "src", 1, 1);
        TestProcessor prc = createProcessor(result.processors, "prc", 1, 2);
        createStream(result.streams, "f0", src, prc);
        TestProcessor prc11 = createProcessor(result.processors, "prc1.1", 2, 3, 4);
        createStream(result.streams, "f1.1", prc, prc11);
        TestProcessor prc12 = createProcessor(result.processors, "prc1.2", 2, 5, 6);
        createStream(result.streams, "f1.2", prc11, prc12);        
        
        TestProcessor prc21 = createProcessor(result.processors, "prc2.1", 2, 7, 8);
        createStream(result.streams, "f2.1", prc, prc21);
        TestProcessor prc22 = createProcessor(result.processors, "prc2.2", 2, 9, 10);
        createStream(result.streams, "f2.1", prc21, prc22);        
        
        TestProcessor snk = createProcessor(result.processors, "snk", 2, 11, 12);
        createStream(result.streams, "f3", prc12, snk);
        createStream(result.streams, "f4", prc22, snk);
        // and a feedback stream ;)
        Stream ff = createStream(result.streams, "ff", snk, prc);
        
        // just for assertion
        TestProcessor noTasks = createProcessor(null, "noTasks", 3);
        Assert.assertFalse(noTasks.handlesTask(4));
        
        result.topo = new PipelineTopology(result.processors);
        Assert.assertEquals(result.processors.size(), result.topo.getProcessorCount());
        Assert.assertEquals(1, result.topo.getSourceCount());
        Assert.assertTrue(src.isSource());
        Assert.assertFalse(src.isSink());
        Assert.assertTrue(src == result.topo.getSource(0));
        Assert.assertEquals(0, result.topo.getSinkCount()); // feedback stream
        Assert.assertFalse(snk.isSource());
        Assert.assertFalse(snk.isSink());        
        for (Processor p : result.processors) {
            Assert.assertTrue(p == result.topo.getProcessor(p.getName()));
        }
        
        result.loopNodes.add(prc);
        result.endNodes.clear(); // none
        Collections.addAll(result.enterDepth, src, prc, prc11, prc12, snk, prc21, prc22, snk);
        Collections.addAll(result.exitDepth, snk, prc12, prc11, snk, prc22, prc21, prc, src);
        
        result.streamsFwd.addAll(result.streams);
        result.streamsFwd.remove(ff);
        
        return result;
    }
    
    /**
     * Creates a processor node.
     * 
     * @param procs the list of all processors (to be modified as a side effect).
     * @param name the name of the processor
     * @param parallelism the parallelism degree of the processor
     * @param tasks the tasks assigned to the processor
     * @return the created processor
     */
    private static TestProcessor createProcessor(List<Processor> procs, String name, int parallelism, int... tasks) {
        TestProcessor result = new TestProcessor(name, parallelism, tasks);
        
        Assert.assertEquals(name, result.getName());
        Assert.assertEquals(parallelism, result.getParallelization());
        for (int t : tasks) {
            Assert.assertTrue(result.handlesTask(t));
        }

        if (null != procs) { 
            procs.add(result);
        }
        
        return result;
    }

    /**
     * Creates a stream from <code>origin</code> to <code>target</code> and asserts the result.
     * 
     * @param streams set of streams to be modified as a side effect
     * @param name the name of the stream
     * @param origin the origin processor
     * @param target the target processor
     * @return the created stream
     */
    private static Stream createStream(List<Stream> streams, String name, TestProcessor origin, TestProcessor target) {
        Stream result = new Stream(name, origin, target);
        origin.addOutput(result);
        target.addInput(result);
        
        Assert.assertEquals(name, result.getName());
        Assert.assertTrue(origin == result.getOrigin());
        Assert.assertTrue(target == result.getTarget());

        boolean found = false;
        for (int o = 0; !found && o < origin.getOutputCount(); o++) {
            found = origin.getOutput(o) == result;
        }
        Assert.assertTrue(found);
        found = false;
        for (int i = 0; !found && i < target.getInputCount(); i++) {
            found = target.getInput(i) == result;
        }
        Assert.assertTrue(found);
        Assert.assertEquals(target.getInputCount() + target.getOutputCount(), target.getStreamCount());
        
        Assert.assertTrue(target.hasInputFrom(origin));
        Assert.assertTrue(origin.hasOutputTo(target));
        if (origin != target) {
            Assert.assertFalse(origin.hasOutputTo(origin));
            Assert.assertFalse(origin.hasInputFrom(target));
        }
        
        if (null != streams) {
            streams.add(result);
        }
        
        return result;
    }
    
    /**
     * Basic topology creation/ access tests.
     */
    @Test
    public void topologyTest() {
        createTopology(); // asserts happen inside
    }
    
    /**
     * Implements a test visitor.
     * 
     * @author Holger Eichelberger
     */
    private static class TestTopologyVisitor implements ITopologyVisitor {

        private int taskCount = 0;
        private int parallCount = 0;
        private List<Processor> loopNodes = new ArrayList<Processor>();
        private List<Processor> endNodes = new ArrayList<Processor>();
        private Set<Processor> visitedNodes = new HashSet<Processor>();
        private Set<Stream> visitedStreams = new HashSet<Stream>();
        private List<Processor> enterVisitSequence = new ArrayList<Processor>();
        private List<Processor> exitVisitSequence = new ArrayList<Processor>();
        
        @Override
        public void exit(Processor node, boolean isEnd, boolean isLoop) {
            if (!isLoop) {
                exitVisitSequence.add(node);
            }
        }
        
        @Override
        public boolean enter(Processor node, boolean isEnd, boolean isLoop) {
            if (!isLoop) {
                enterVisitSequence.add(node);
                if (!visitedNodes.contains(node)) {
                    taskCount += node.getTaskCount();
                    parallCount += node.getParallelization();
                }
            } else {
                loopNodes.add(node);
            }
            if (isEnd) {
                endNodes.add(node);
            }
            visitedNodes.add(node);
            return false;
        }

        @Override
        public boolean visit(Stream stream) {
            visitedStreams.add(stream);
            return false;
        }

    }
    
    /**
     * Basic topology creation/ access tests.
     */
    @Test
    public void topologyDepthFirstTest() {
        TopologyInfo info = createTopology(); // asserts happen inside
        TestTopologyVisitor testVisitor = new TestTopologyVisitor();
        TopologyWalker walker = new TopologyWalker(new TopologyWalker.DepthFirstVisitingStrategy(), testVisitor);
        walker.visit(info.topo);
        
        int taskCount = 0;
        int parallel = 0;
        for (Processor proc : info.processors) {
            taskCount += proc.getTaskCount();
            parallel += proc.getParallelization();
        }
        
        Assert.assertEquals(info.streamsFwd.size(), testVisitor.visitedStreams.size());
        Assert.assertTrue(testVisitor.visitedStreams.containsAll(info.streamsFwd));
        
        Assert.assertEquals(taskCount, testVisitor.taskCount);
        Assert.assertEquals(parallel, testVisitor.parallCount);
        Assert.assertEquals(info.enterDepth, testVisitor.enterVisitSequence);
        Assert.assertEquals(info.exitDepth, testVisitor.exitVisitSequence);
    }

    /**
     * Tests the statistics walker.
     * 
     * @throws IOException shall not happen
     */
    @Test
    public void statisticsWalkerTest() throws IOException {
        FileInputStream nmIn = new FileInputStream(new File(Utils.getTestdataDir(), "statisticsWalker.xml"));
        INameMapping nameMapping = new NameMapping("pip", nmIn);
        CoordinationManager.registerTestMapping(nameMapping); // avoid init of EASy
        nmIn.close();
        
        SystemState state = new SystemState();
        PipelineSystemPart pipeline = state.obtainPipeline("pip");
        TopologyInfo info = createTopology();
        pipeline.setTopology(info.topo);
        PipelineNodeSystemPart pSrc = pipeline.obtainPipelineNode("src");
        setValues(pSrc, 10, 10000, 1000, 0.01, new ComponentKey("localhost", 1234, 1));
        PipelineNodeSystemPart pPrc = pipeline.obtainPipelineNode("prc");
        setValues(pPrc, 15, 9000, 900, 0.03, new ComponentKey("localhost", 1234, 2));
        PipelineNodeSystemPart pSnk = pipeline.obtainPipelineNode("snk");
        setValues(pSnk, 10, 7500, 750, 0.02, new ComponentKey("localhost", 1234, 3));
        NodeImplementationSystemPart alg1 = pipeline.getAlgorithm("alg1");
        PipelineNodeSystemPart pPrc11 = alg1.obtainPipelineNode("prc1.1");
        setValues(pPrc11, 30, 8000, 800, 0.1, new ComponentKey("localhost", 1234, 5));
        PipelineNodeSystemPart pPrc12 = alg1.obtainPipelineNode("prc1.2");
        setValues(pPrc12, 50, 6000, 600, 0.2, new ComponentKey("localhost", 1234, 7));
        NodeImplementationSystemPart alg2 = pipeline.getAlgorithm("alg2");
        PipelineNodeSystemPart pPrc21 = alg2.obtainPipelineNode("prc2.1");
        setValues(pPrc21, 29, 8150, 875, 0.3, new ComponentKey("localhost", 1234, 9));
        PipelineNodeSystemPart pPrc22 = alg2.obtainPipelineNode("prc2.2");
        setValues(pPrc22, 46, 8050, 850, 0.4, new ComponentKey("localhost", 1234, 11));
        pPrc.setCurrent(alg2);

        ObservationAggregator throughputVolume 
            = ObservationAggregatorFactory.getAggregator(TimeBehavior.THROUGHPUT_VOLUME);
        ObservationAggregator throughputItems 
            = ObservationAggregatorFactory.getAggregator(TimeBehavior.THROUGHPUT_ITEMS);
        ObservationAggregator latency 
            = ObservationAggregatorFactory.getAggregator(TimeBehavior.LATENCY);
        ObservationAggregator capacity 
            = ObservationAggregatorFactory.getAggregator(ResourceUsage.CAPACITY);        
        ObservationAggregator[] aggregators = new ObservationAggregator[] {
            throughputVolume, throughputItems, latency, capacity};
        
        StatisticsWalker walker = StatisticsWalker.POOL.getInstance();
        walker.visit(alg1, aggregators); // just the nodes making up alg2
        assertEqualsAll(toArray(pPrc11, pPrc12), aggregators);
        StatisticsWalker.clear(aggregators);
        walker.visit(alg2, aggregators); // just the nodes making up alg2
        assertEqualsAll(toArray(pPrc21, pPrc22), aggregators);
        StatisticsWalker.clear(aggregators);
        walker.visit(pPrc21, aggregators); // shall be just pPrc21
        assertEqualsAll(toArray(pPrc21), aggregators);
        StatisticsWalker.clear(aggregators);
        walker.visit(pPrc, aggregators);
        assertEqualsAll(toArray(pPrc, pPrc21, pPrc22), aggregators, LOCAL_VALUE); // due to active, test aggregation
        StatisticsWalker.clear(aggregators);
        // algorithm corresponds to active nodes (via implicit topology aggregation)
        assertEqualsAll(toArray(pPrc11, pPrc12), alg1, toObservables(aggregators));
        walker.visit(pipeline, aggregators);
        if (!AbstractCoordinationTests.isJenkins()) {
            assertEqualsAll(toArray(pSrc, pPrc, pSnk), aggregators); // assert the aggregators
            assertEqualsAll(toArray(pSrc, pPrc, pSnk), pipeline, toObservables(aggregators)); // assert the system part
        }
        StatisticsWalker.clear(aggregators);
        if (!AbstractCoordinationTests.isJenkins()) {
            assertEquals(pSnk, pipeline, Scalability.ITEMS); // through ITEMS aggregation, setting in basic data
        }
        for (ObservationAggregator aggregator : aggregators) {
            ObservationAggregatorFactory.releaseAggregator(aggregator);
        }
        StatisticsWalker.POOL.releaseInstance(walker);
        state.clear();
        CoordinationManager.unregisterNameMapping(nameMapping);
    }

    /**
     * Asserts equality between parts and all individual aggregators on the aggregated values.
     * 
     * @param parts the parts
     * @param aggregators the aggregators
     */
    private static void assertEqualsAll(SystemPart[] parts, ObservationAggregator[] aggregators) {
        assertEqualsAll(parts, aggregators, VALUE);
    }
    
    /**
     * Asserts equality between parts and all individual aggregators.
     * 
     * @param parts the parts
     * @param aggregators the aggregators
     * @param projector the value projector to apply
     */
    private static void assertEqualsAll(SystemPart[] parts, ObservationAggregator[] aggregators, 
        IValueProjector projector) {
        for (ObservationAggregator aggregator : aggregators) {
            assertEquals(parts, aggregator, projector);
        }
    }
    
    /**
     * Just turns the vararg into an array.
     * 
     * @param parts the individual parts
     * @return the array containing the parts
     */
    private static SystemPart[] toArray(SystemPart... parts) {
        return parts;
    }
    
    // checkstyle: stop parameter number check
    
    /**
     * Sets part observable values.
     * 
     * @param part the system part to modify as a side effect
     * @param latency the latency value
     * @param throughputVolume the throughput volume value
     * @param throughputItems the throughput items value
     * @param capacity the capacity
     * @param key the key to use
     */
    private static void setValues(SystemPart part, double latency, double throughputVolume, double throughputItems, 
        double capacity, Object key) {
        part.setValue(TimeBehavior.LATENCY, latency, key);
        part.setValue(TimeBehavior.THROUGHPUT_VOLUME, throughputVolume, key);
        part.setValue(TimeBehavior.THROUGHPUT_ITEMS, throughputItems, key);
        part.setValue(Scalability.ITEMS, throughputItems, key); // as done in MonitoringEventHandler
        part.setValue(ResourceUsage.CAPACITY, capacity, key);
    }
    
    // checkstyle: resume parameter number check
    
    /**
     * Asserts equality between the sum of the observations for <code>actual</code>'s observable over 
     * <code>expected</code> and <code>actual</code>'s value.
     * 
     * @param expected the expected parts
     * @param actual the actual value and observable
     * @param projector the value projector
     */
    private static void assertEquals(SystemPart[] expected, ObservationAggregator actual, IValueProjector projector) {
        IPathAggregator pAgg = getPathElementAggregator(actual);
        Assert.assertEquals(actual.getObservable() + " " + toString(expected, actual.getObservable()) + " ", 
            pAgg.aggregate(actual.getObservable(), actual.doPathAverage(), projector, expected), 
                actual.getValue(), ASSERT_DELTA);
    }
    
    /**
     * Turns the values of <code>observable</code> for the given <code>parts</code> into text.
     * 
     * @param parts the parts
     * @param observable the observable
     * @return the text representation
     */
    private static String toString(SystemPart[] parts, IObservable observable) {
        String result = "";
        for (SystemPart p : parts) {
            if (result.length() > 0) {
                result += " ";
            }
            result += p.getName() + " " + p.getObservedValue(observable);
        }
        return result;
    }
    
    /**
     * Turns aggregators into their assigned observables.
     * 
     * @param aggregators the aggregators
     * @return the observables
     */
    private static IObservable[] toObservables(ObservationAggregator[] aggregators) {
        IObservable[] result = new IObservable[aggregators.length];
        for (int o = 0; o < aggregators.length; o++) {
            result[o] = aggregators[o].getObservable();
        }
        return result;
    }

    /**
     * Asserts equality between the sum of the observations for <code>actual</code>'s observable over all 
     * <code>observables</code>.
     * 
     * @param expected the expected parts
     * @param actual the actual value
     * @param observables the observables to compare
     */
    private static void assertEqualsAll(SystemPart[] expected, SystemPart actual, IObservable[] observables) {
        for (IObservable observable : observables) {
            if (actual.supportsObservation(observable)) {
                ObservationAggregator obsAgg = ObservationAggregatorFactory.getAggregator(observable);
                IPathAggregator agg = getPathElementAggregator(obsAgg);
                double expectedValue = agg.aggregate(observable, obsAgg.doPathAverage(), VALUE, expected);
                double actualValue = actual.getObservedValue(observable);
                ObservationAggregatorFactory.releaseAggregator(obsAgg);
                Assert.assertEquals(observable + " in " + actual.getName(), expectedValue, actualValue, ASSERT_DELTA);
            }
        }
    }
    
    /**
     * Asserts equality of <code>observable</code> on <code>expected</code> and <code>actual</code>.
     * 
     * @param expected the part carrying the expected value
     * @param actual the part carrying the actual value
     * @param observable the observable to assert
     */
    private static void assertEquals(SystemPart expected, SystemPart actual, IObservable observable) {
        Assert.assertEquals(observable + " in " + actual.getName(), expected.getObservedValue(observable), 
            actual.getObservedValue(observable), ASSERT_DELTA);
    }
    
}
