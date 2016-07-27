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

import backtype.storm.Config;
import eu.qualimaster.base.pipeline.RecordingTopologyBuilder;
import eu.qualimaster.coordination.INameMapping;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineSystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.observables.TimeBehavior;

/**
 * Creates a (generated) HW topology - direct integration.
 * 
 * @author Holger Eichelberger
 */
public class HwTopology extends AbstractTopology {

    public static final String PIP = "testHwPip";
    private static final boolean SEND_EVENTS = true;
    
    @Override
    public void createTopology(Config config, RecordingTopologyBuilder builder) {
        // Source - HW Bolt ... HW Spout
        builder.setSpout(getTestSourceName(), 
            new TestSourceSource(getTestSourceName(), PIP, SEND_EVENTS), 1)
            .setNumTasks(1);
        
        builder.setBolt(getTestFamilyName(), 
            new TestFamilyHwFamilyElement(getTestFamilyName(), PIP, SEND_EVENTS, getAlgorithmName(), true), 1)
           .setNumTasks(1).shuffleGrouping(getTestSourceName());

        builder.setBolt(getTestHwSendingBoltName(), 
            new SendingBolt(getTestHwSendingBoltName(), PIP, SEND_EVENTS, true, 9990), 1)
           .setNumTasks(1).shuffleGrouping(getTestFamilyName());
        // HW
        builder.setSpout(getTestHwReceivingSpoutName(), 
            new ReceivingSpout(getTestHwReceivingSpoutName(), PIP, SEND_EVENTS, true, 9990), 1)
            .setNumTasks(1);

        builder.close(PIP, config);
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
     * Returns the name of the test algorithm.
     * 
     * @return the name of the test algorithm
     */
    protected String getAlgorithmName() {
        return "GenTopoHardwareCorrelationFinancial";
    }

    /**
     * Returns the name of the test HW sending bolt.
     * 
     * @return the name of the test HW sending bolt
     */
    protected String getTestHwSendingBoltName() {
        return TestFamilyHwFamilyElement.HW_BOLT;
    }
    
    /**
     * Returns the name of the test HW receiving spout.
     * 
     * @return the name of the test HW receiving spout
     */
    protected String getTestHwReceivingSpoutName() {
        return TestFamilyHwFamilyElement.HW_SPOUT;
    }

    @Override
    public String getName() {
        return PIP;
    }

    @Override
    public void assertState(SystemState state, INameMapping mapping, long pipRunTime) {
        PipelineSystemPart pip = state.getPipeline(PIP);
        Assert.assertNotNull(pip);
        PipelineNodeSystemPart source = pip.getNode(getTestSourceName());
        Assert.assertNotNull(source);
        assertGreater(0, source, TimeBehavior.LATENCY);
        assertGreater(1, source, TimeBehavior.THROUGHPUT_ITEMS);
        PipelineNodeSystemPart sender = pip.getNode(getTestHwSendingBoltName());
        Assert.assertNotNull(sender);
        //assertGreater(0, sender, TimeBehavior.LATENCY);
        assertGreater(1, sender, TimeBehavior.THROUGHPUT_ITEMS);
        PipelineNodeSystemPart receiver = pip.getNode(getTestHwReceivingSpoutName());
        Assert.assertNotNull(receiver);
        assertGreater(0, receiver, TimeBehavior.LATENCY);
        assertGreater(1, receiver, TimeBehavior.THROUGHPUT_ITEMS);
        
        double pipThrough = pip.getObservedValue(TimeBehavior.THROUGHPUT_ITEMS);
        double recvThrough = pip.getObservedValue(TimeBehavior.THROUGHPUT_ITEMS);
        Assert.assertEquals("recv throughput is not pip throughput", pipThrough, recvThrough, 0.005);
    }

    @Override
    public String getMappingFileName() {
        return "testHwPip.xml"; 
    }
    
}
