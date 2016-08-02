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
package tests.eu.qualimaster.monitoring.genTopo;

import org.junit.Assert;

import eu.qualimaster.coordination.INameMapping;
import eu.qualimaster.coordination.INameMapping.Algorithm;
import eu.qualimaster.monitoring.systemState.NodeImplementationSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineSystemPart;
import eu.qualimaster.monitoring.systemState.SystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.Scalability;
import eu.qualimaster.observables.TimeBehavior;

/**
 * Abstract functions for the Hy-Profiling topology.
 * 
 * @author Holger Eichelberger
 */
public abstract class AbstractHyTopology extends AbstractTopology {

    @Override
    public void assertState(SystemState state, INameMapping mapping, long pipRunTime) {
        System.out.println(state.format());
        PipelineSystemPart pip = state.getPipeline(getName());
        Assert.assertNotNull(pip);
        PipelineNodeSystemPart source = getNode(getTestSourceName(), pip, mapping, true);
        PipelineNodeSystemPart family = getNode(getTestFamilyName(), pip, mapping, true);
        PipelineNodeSystemPart mapper = getNode(getHyMapperName(), family, null, true);
        PipelineNodeSystemPart processor = getNode(getHyProcessorName(), family, null, true);
        NodeImplementationSystemPart algorithm = getAlgorithm(getAlgorithmName(), pip, null, true);
        
        assertValue(TestSourceSource.LATENCY, source, TimeBehavior.LATENCY, 0.1);
        assertValue(AbstractProcessor.LATENCY, mapper, TimeBehavior.LATENCY, 0.1);
        // avg, 2 nodes aggregated
        assertValue(AbstractProcessor.LATENCY, processor, TimeBehavior.LATENCY, 0.1);
        // mapper, avg processor
        assertValue(2 * AbstractProcessor.LATENCY, algorithm, TimeBehavior.LATENCY, 0.1);
        // fam, mapper, avg processor
        assertValue(3 * AbstractProcessor.LATENCY, family, TimeBehavior.LATENCY, 0.1);
        // fam, mapper, avg processor
        assertValue(TestSourceSource.LATENCY + 3 * AbstractProcessor.LATENCY, pip, TimeBehavior.LATENCY, 0.1);
        
        // sometimes thrift values are not yet available, in particular for TestFamily - whyever
        //assertGreaterEquals(0.8, source, ResourceUsage.CAPACITY);
        //assertGreaterEquals(0.5, mapper, ResourceUsage.CAPACITY);
        //assertGreaterEquals(0.5, processor, ResourceUsage.CAPACITY);
        //assertGreaterEquals(0.8, pip, ResourceUsage.CAPACITY);
        
        double processorThroughput = processor.getObservedValue(TimeBehavior.THROUGHPUT_ITEMS);
        if ((isThrift() && processorThroughput > 0) || !isThrift()) { // currently, sometimes there, sometimes not
            assertGreaterEquals(10, processor, TimeBehavior.THROUGHPUT_ITEMS);
            
            assertEquals(processorThroughput, algorithm, TimeBehavior.THROUGHPUT_ITEMS, 1);
            assertEquals(processorThroughput, pip, TimeBehavior.THROUGHPUT_ITEMS, 1);
            
            // just be careful...
            assertGreaterEquals(1, source, TimeBehavior.THROUGHPUT_ITEMS);
            //assertGreaterEquals(1, mapper, TimeBehavior.THROUGHPUT_ITEMS);
            
            // throughput/latency reporting delay
            assertGreaterEquals(0.5, processor, Scalability.ITEMS);
            assertGreaterEquals(0.5, family, Scalability.ITEMS);
            assertGreaterEquals(0.5, pip, Scalability.ITEMS);
        }
    }
    
    /**
     * Whether we use thrift for monitoring.
     * 
     * @return <code>true</code> for thrift, <code>false</code> else
     */
    protected abstract boolean isThrift();
    
    /**
     * Asserts the <code>expected</code> value for the given <code>part</code> and <code>observable</code> within
     * a given tolerance range around <code>expected</code>.
     * 
     * @param expected the expected value
     * @param part the system part to check
     * @param observable the observable to check
     * @param absTolerance the absolute tolerance in (0-1)
     */
    private void assertValue(double expected, SystemPart part, IObservable observable, double absTolerance) {
        Double actual = part.getObservedValue(observable);
        Assert.assertNotNull(actual);
        double tolerance = expected * absTolerance;
        Assert.assertTrue(actual + " for " + observable + " not within " + (absTolerance * 100) + "%/" + tolerance 
            + " tolerance around " + expected + " - " + part, 
            expected - tolerance <= actual && actual <= expected + tolerance);
    }

    /**
     * Asserts that <code>min</code> is less or equal than the value of <code>part</code> and <code>observable</code>.
     * 
     * @param min the minimum expected value to pass
     * @param part the system part to check
     * @param observable the observable to check
     */
    private void assertGreaterEquals(double min, SystemPart part, IObservable observable) {
        Double actual = part.getObservedValue(observable);
        Assert.assertNotNull(actual);
        Assert.assertTrue("not " + actual + ">=" + min, actual >= min);
    }
    
    /**
     * Asserts that <code>min</code> is less or equal than the value of <code>part</code> and <code>observable</code>.
     * 
     * @param expected the expected value to pass
     * @param part the system part to check
     * @param observable the observable to check
     * @param tolerance the tolerance in comparison
     */
    private void assertEquals(double expected, SystemPart part, IObservable observable, double tolerance) {
        Double actual = part.getObservedValue(observable);
        Assert.assertNotNull(actual);
        Assert.assertEquals(expected, actual.doubleValue(), tolerance);
    }

    /**
     * Returns a pipeline node for a given implementation name.
     * 
     * @param name the name
     * @param pip the pipeline
     * @param mapping the name mapping (<b>null</b> for no mapping if <code>name</code> is already a logical name)
     * @param assertExistence assert the existence of the node or not
     * @return the node
     */
    private PipelineNodeSystemPart getNode(String name, PipelineSystemPart pip, INameMapping mapping, 
        boolean assertExistence) {
        String tmp = name;
        if (null != mapping) {
            tmp = mapping.getPipelineNodeByImplName(name);
            Assert.assertNotNull(tmp);
        }
        PipelineNodeSystemPart node = pip.getNode(tmp);
        if (assertExistence) {
            Assert.assertNotNull(node);
        }
        return node;
    }
    
    /**
     * Returns a pipeline node for a given implementation name.
     * 
     * @param name the name
     * @param pip the pipeline
     * @param mapping the name mapping (<b>null</b> for no mapping if <code>name</code> is already a logical name)
     * @param assertExistence assert the existence of the node or not
     * @return the node
     */
    private PipelineNodeSystemPart getNode(String name, PipelineNodeSystemPart pip, INameMapping mapping, 
        boolean assertExistence) {
        String tmp = name;
        if (null != mapping) {
            tmp = mapping.getPipelineNodeByImplName(name);
            Assert.assertNotNull(tmp);
        }
        PipelineNodeSystemPart node = pip.getNode(tmp);
        if (assertExistence) {
            Assert.assertNotNull(node);
        }
        return node;
    }

    /**
     * Returns a pipeline node for a given implementation name.
     * 
     * @param name the name
     * @param pip the pipeline
     * @param mapping the name mapping (<b>null</b> for no mapping if <code>name</code> is already a logical name)
     * @param assertExistence assert the existence of the node or not
     * @return the algorithm
     */
    private NodeImplementationSystemPart getAlgorithm(String name, PipelineSystemPart pip, INameMapping mapping, 
        boolean assertExistence) {
        String tmp = name;
        if (null != mapping) {
            Algorithm alg = mapping.getAlgorithmByImplName(name);
            Assert.assertNotNull(alg);
            tmp = alg.getName();
        }
        NodeImplementationSystemPart node = pip.getAlgorithm(tmp);
        if (assertExistence) {
            Assert.assertNotNull(node);
        }
        return node;
    }
    
    /**
     * Returns the name of the test source.
     * 
     * @return the name of the test source
     */
    protected String getTestSourceName() {
        return "TestSource";
    }

    /**
     * Returns the name of the test family.
     * 
     * @return the name of the test family
     */
    protected String getTestFamilyName() {
        return "TestFamily";
    }

    /**
     * Returns the name of the Hy-Mapper bolt.
     * 
     * @return the name
     */
    protected String getHyMapperName() {
        return "SubTopology_FamilyElement0";
    }

    /**
     * Returns the name of the Hy-Processor bolt.
     * 
     * @return the name
     */
    protected String getHyProcessorName() {
        return "SubTopology_FamilyElement1";
    }
    
    /**
     * Returns the name of the algorithm to test.
     * 
     * @return the name of the algorithm
     */
    protected abstract String getAlgorithmName();

}
