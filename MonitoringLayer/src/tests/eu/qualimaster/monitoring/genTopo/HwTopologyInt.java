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
import eu.qualimaster.monitoring.events.SubTopologyMonitoringEvent;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineSystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.observables.TimeBehavior;
import tests.eu.qualimaster.storm.ReceivingSpout;
import tests.eu.qualimaster.storm.SendingBolt;

/**
 * Creates a (generated) HW topology - loose integration.
 * 
 * @author Holger Eichelberger
 */
public class HwTopologyInt extends AbstractTopology {

    public static final String PIP = "testHwPip";
    private static final boolean SEND_EVENTS = true;
    private boolean withSink;
    
    /**
     * Creates a HW topology with loose integration.
     * 
     * @param withSink with sink or without sink
     */
    public HwTopologyInt(boolean withSink) {
        this.withSink = withSink;
    }

    @Override
    public SubTopologyMonitoringEvent createTopology(Config config, RecordingTopologyBuilder builder) {
        // Source - HW Bolt ... HW Spout
        builder.setSpout(getTestSourceName(), 
            new TestSourceSource(getTestSourceName(), PIP, SEND_EVENTS), 1)
            .setNumTasks(1);
        
        builder.setBolt(getTestFamilyName(), 
            new TestFamilyHwIntFamilyElement(getTestFamilyName(), PIP, SEND_EVENTS, getAlgorithmName(), true, 9993), 1)
           .setNumTasks(1).shuffleGrouping(getTestSourceName());

        builder.startRecording("GenTopoHardwareCorrelationFinancial");
        builder.setSpout(getTestIntermediaryBoltName(), 
            new ReceivingSpout(getTestIntermediaryBoltName(), PIP, SEND_EVENTS, true, 9993), 1)
            .setNumTasks(1);

        builder.setBolt(getTestHwSendingBoltName(), 
            new SendingBolt(getTestHwSendingBoltName(), PIP, SEND_EVENTS, true, 9992), 1)
           .setNumTasks(1).shuffleGrouping(getTestIntermediaryBoltName());
        // HW
        builder.setSpout(getTestHwReceivingSpoutName(), 
            new ReceivingSpout(getTestHwReceivingSpoutName(), PIP, SEND_EVENTS, true, 9992), 1)
            .setNumTasks(1);

        builder.setBolt(getOutSenderName(), 
            new SendingBolt(getOutSenderName(), PIP, SEND_EVENTS, true, 9991), 1)
            .setNumTasks(1).shuffleGrouping(getTestHwReceivingSpoutName());

        builder.setSpout(getOutReceiverName(), 
            new ReceivingSpout(getOutReceiverName(), PIP, SEND_EVENTS, true, 9991), 1)
            .setNumTasks(1);
        builder.endRecording();
        
        if (withSink) {
            builder.setBolt(getSinkName(), 
                new SinkBolt(getSinkName(), PIP, SEND_EVENTS, true), 1)
                .setNumTasks(1).shuffleGrouping(getOutReceiverName());
        }
        
        return builder.createClosingEvent(PIP, config);
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
     * Returns the name of the intermediary receiving bolt.
     * 
     * @return the name of the intermediary receiving
     */
    protected String getTestIntermediaryBoltName() {
        return TestSwitchFamilyElement.INTERMEDIARY;
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
        return TestFamilyHwIntFamilyElement.HW_BOLT;
    }
    
    /**
     * Returns the name of the test HW receiving spout.
     * 
     * @return the name of the test HW receiving spout
     */
    protected String getTestHwReceivingSpoutName() {
        return TestFamilyHwIntFamilyElement.HW_SPOUT;
    }
    
    /**
     * Returns the name of the out sender.
     * 
     * @return the name of the out sender
     */
    protected String getOutSenderName() {
        return TestFamilyHwIntFamilyElement.OUT_INTERMEDIARY;
    }

    /**
     * Returns the name of the out receiver.
     * 
     * @return the name of the out receiver
     */
    protected String getOutReceiverName() {
        return TestFamilyHwIntFamilyElement.OUT_RECEIVER;
    }

    /**
     * Returns the name of the sink.
     * 
     * @return the name of the sink
     */
    protected String getSinkName() {
        return "snk";
    }

    @Override
    public String getName() {
        return PIP;
    }

    @Override
    public void assertState(SystemState state, INameMapping mapping, long pipRunTime) {
        System.out.println(state.format());
        PipelineSystemPart pip = state.getPipeline(PIP);
        Assert.assertNotNull(pip);
        PipelineNodeSystemPart source = pip.getNode(getTestSourceName());
        Assert.assertNotNull(source);
        assertGreater(0, source, TimeBehavior.LATENCY);
        assertGreater(1, source, TimeBehavior.THROUGHPUT_ITEMS);
        PipelineNodeSystemPart end;
        if (withSink) {
            end = pip.getNode(getSinkName());    
        } else {
            end = pip.getNode(getOutReceiverName());
        }
        
        Assert.assertNotNull(end);
        assertGreater(0, end, TimeBehavior.LATENCY);
        assertGreater(1, end, TimeBehavior.THROUGHPUT_ITEMS);
    }
    
    @Override
    public String getMappingFileName() {
        return "testHwPipInt.xml"; 
    }
    
}
