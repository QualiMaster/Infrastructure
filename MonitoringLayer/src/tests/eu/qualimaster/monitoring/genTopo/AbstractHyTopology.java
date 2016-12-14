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
import eu.qualimaster.monitoring.systemState.NodeImplementationSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineSystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;
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
            assertGreaterEquals(1, processor, TimeBehavior.THROUGHPUT_ITEMS);
            
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
