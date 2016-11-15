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

import java.io.File;
import java.util.Map;

import org.junit.Assert;

import backtype.storm.Config;
import eu.qualimaster.base.pipeline.RecordingTopologyBuilder;
import eu.qualimaster.coordination.INameMapping;
import eu.qualimaster.coordination.StormUtils.TopologyTestInfo;
import eu.qualimaster.coordination.commands.AlgorithmChangeCommand;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.PipelineOptions;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineSystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.observables.TimeBehavior;
import tests.eu.qualimaster.coordination.AbstractCoordinationTests;
import tests.eu.qualimaster.storm.ReceivingSpout;
import tests.eu.qualimaster.storm.SendingBolt;

/**
 * Creates a HY testing topology with sub-topologies.
 * 
 * @author Holger Eichelberger
 */
public class SubTopology extends AbstractTopology {

    public static final String PIP = "RandomPip";
    private static final boolean SEND_EVENTS = true;

    /**
     * Returns the name of the source.
     * 
     * @return the name of the source
     */
    protected String getSourceName() {
        return "PipelineVar_1_Source0";
    }

    /**
     * Returns the name of the family.
     * 
     * @return the name of the family
     */
    protected String getFamilyName() {
        return "PipelineVar_1_FamilyElement0";
    }

    /**
     * Returns the name of the algorithm.
     * 
     * @return the name of the algorithm
     */
    protected String getAlgorithmName() {
        return "RandomSubPipelineAlgorithm1";
    }
    
    /**
     * Returns the name of the sink.
     * 
     * @return the name of the sink
     */
    protected String getSinkName() {
        return "PipelineVar_1_Sink0";
    }

    /**
     * Returns the name of the sub-topology receiver.
     * 
     * @return the name of the sub-topology receiver
     */
    protected String getSubReceiverName() {
        return "RandomSubPipeline1Intermediary";
    }

    /**
     * Returns the name of the sub-topology processor.
     * 
     * @return the name of the sub-topology processor
     */
    protected String getSubProcessorName() {
        return "SubPipelineVar_11_FamilyElement0";
    }
    
    /**
     * Returns the name of the sub-topology sender.
     * 
     * @return the name of the sub-topology sender
     */
    protected String getSubSenderName() {
        return "RandomSubPipeline1EndBolt";
    }

    @Override
    public void createTopology(Config config, RecordingTopologyBuilder builder) {
        builder.setSpout(getSourceName(), 
            new TestSourceSource(getSourceName(), PIP, SEND_EVENTS), 1)
            .setNumTasks(1);
        SendingFamily sf = new SendingFamily(getFamilyName(), PIP, SEND_EVENTS, true, 9891);
        builder.setBolt(getFamilyName(), sf, 1)
            .setNumTasks(1).shuffleGrouping(getSourceName());

        builder.setSpout(getSinkName(), 
            new ReceivingSpout(getSinkName(), PIP, SEND_EVENTS, true, 9890), 1)
            .setNumTasks(1);
        builder.close(PIP, config);
    }

    @Override
    public String getName() {
        return PIP;
    }

    @Override
    public String getMappingFileName() {
        return "randomSubTopo/mapping.xml";
    }

    @Override
    public void assertState(SystemState state, INameMapping mapping, long pipRunTime) {
        PipelineSystemPart pip = state.getPipeline(getName());
        Assert.assertNotNull(pip);
        PipelineNodeSystemPart source = getNode(getSourceName(), pip, mapping, true);
        PipelineNodeSystemPart family = getNode(getFamilyName(), pip, mapping, true);
        PipelineNodeSystemPart subProcessor = getNode(getSubProcessorName(), family, null, true);
        
        assertGreaterEquals(1, source, TimeBehavior.THROUGHPUT_ITEMS);
        assertGreaterEquals(1, family, TimeBehavior.THROUGHPUT_ITEMS);
        assertGreaterEquals(1, subProcessor, TimeBehavior.THROUGHPUT_ITEMS);
    }

    @Override
    public void registerSubTopologies(Map<String, TopologyTestInfo> info) {
        String subTopoName = PIP; // passed by generated code
        PipelineOptions opt = new PipelineOptions();
        RecordingTopologyBuilder builder = new RecordingTopologyBuilder(opt);

        builder.setSpout(getSubReceiverName(), new ReceivingSpout(getSubReceiverName(), subTopoName, 
            true, true, 9891), 1).setNumTasks(1);
        builder.setBolt(getSubProcessorName(), new SubProcessor(getSubProcessorName(), subTopoName, 
            true, true), 1).setNumTasks(1).shuffleGrouping(getSubReceiverName());
        builder.setBolt(getSubSenderName(), new SendingBolt(getSubSenderName(), subTopoName, 
            true, true, 9890), 1).setNumTasks(1).shuffleGrouping(getSubProcessorName());
        
        info.put("RandomSubPipeline1", new TopologyTestInfo(builder.createTopology(), 
            new File(""), AbstractCoordinationTests.createTopologyConfiguration()));
    }

    /**
     * A simple sub-processor.
     * 
     * @author Holger Eichelberger
     */
    @SuppressWarnings("serial")
    private static class SubProcessor extends AbstractProcessor {

        /**
         * Creates an abstract processor.
         * 
         * @param name the name of the processor
         * @param namespace the containing namespace
         * @param sendMonitoringEvents do send monitoring events
         * @param sendRegular whether this monitor shall care for sending regular events (<code>true</code>) or 
         *     not (<code>false</code>, for thrift-based monitoring)
         */
        public SubProcessor(String name, String namespace, boolean sendMonitoringEvents, boolean sendRegular) {
            super(name, namespace, sendMonitoringEvents, sendRegular);
        }
        
    }

    @Override
    public void started() {
        // the other names are implementation names
        EventManager.send(new AlgorithmChangeCommand(PIP, "processor", "RandomSubPipelineAlgorithm1"));
    }
    
    @Override
    public int plannedExecutionTime() {
        return 20000;
    }
    
}
