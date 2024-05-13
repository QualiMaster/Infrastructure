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
 * Creates an alternative (network integrated) sub-path sub-topology.
 * 
 * @author Holger Eichelberger
 */
public class SwitchTopology extends AbstractTopology {

    public static final String PIP = "pipeline";
    private static final boolean SEND_EVENTS = true;

    private boolean withSink;
    
    /**
     * Creates a switch topology.
     * 
     * @param withSink for the full topology, <code>false</code> for a profiling topology without sink
     */
    public SwitchTopology(boolean withSink) {
        this.withSink = withSink;
    }
    
    @Override
    public SubTopologyMonitoringEvent createTopology(Config config, RecordingTopologyBuilder builder) {
        // Source - Family = Intermediary - Mapper - Processor - Intermediary = Receiver
        
        builder.setSpout(getTestSourceName(), 
            new TestSourceSource(getTestSourceName(), PIP, SEND_EVENTS), 1)
            .setNumTasks(1);
        
        builder.setBolt(getTestFamilyName(), 
            new TestSwitchFamilyElement(getTestFamilyName(), PIP, SEND_EVENTS, true, 9994), 1)
           .setNumTasks(1).shuffleGrouping(getTestSourceName());

        builder.setSpout(getTestIntermediaryBoltName(), 
            new ReceivingSpout(getTestIntermediaryBoltName(), PIP, SEND_EVENTS, true, 9994), 1)
            .setNumTasks(1);

        builder.setBolt(getMapperName(), 
            new SubTopologyFamilyElement0FamilyElement(getMapperName(), PIP, SEND_EVENTS, true))
            .setNumTasks(1).shuffleGrouping(getTestIntermediaryBoltName());
        
        builder.setBolt(getProcessorName(), 
            new SubTopologyFamilyElement1FamilyElement(getProcessorName(), PIP, SEND_EVENTS, true))
            .setNumTasks(1).shuffleGrouping(getMapperName());
        
        builder.setBolt(getOutSenderName(), 
            new SendingBolt(getOutSenderName(), PIP, SEND_EVENTS, true, 9995), 1)
            .setNumTasks(1).shuffleGrouping(getProcessorName());

        builder.setSpout(getOutReceiverName(), 
            new ReceivingSpout(getOutReceiverName(), PIP, SEND_EVENTS, true, 9995), 1)
            .setNumTasks(1);
        
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
        return "src";
    }
    
    /**
     * Returns the name of the test family.
     * 
     * @return the name of the test family
     */
    protected String getTestFamilyName() {
        return TestSwitchFamilyElement.FAMILY;
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
     * Returns the name of the mapper bolt.
     * 
     * @return the name of the mapper
     */
    protected String getMapperName() {
        return TestSwitchFamilyElement.MAPPER;
    }

    /**
     * Returns the name of the mapper bolt.
     * 
     * @return the name of the mapper
     */
    protected String getProcessorName() {
        return TestSwitchFamilyElement.PROCESSOR;
    }

    /**
     * Returns the name of the out sender.
     * 
     * @return the name of the out sender
     */
    protected String getOutSenderName() {
        return TestSwitchFamilyElement.OUT_INTERMEDIARY;
    }

    /**
     * Returns the name of the out receiver.
     * 
     * @return the name of the out receiver
     */
    protected String getOutReceiverName() {
        return TestSwitchFamilyElement.OUT_RECEIVER;
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
        return "testSwitchPip.xml";
    }
    
}
