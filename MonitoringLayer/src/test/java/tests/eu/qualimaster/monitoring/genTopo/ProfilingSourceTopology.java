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
import eu.qualimaster.infrastructure.PipelineOptions;
import eu.qualimaster.monitoring.events.SubTopologyMonitoringEvent;
import eu.qualimaster.monitoring.systemState.PipelineSystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;
import tests.eu.qualimaster.coordination.Utils;
import tests.eu.qualimaster.monitoring.genTopo.profiling.TestSourceSource;

/**
 * Creates a HY testing topology with sub-topologies.
 * 
 * @author Holger Eichelberger
 */
public class ProfilingSourceTopology extends AbstractTopology {

    public static final String PIP = "TestPip";

    /**
     * Returns the name of the source.
     * 
     * @return the name of the source
     */
    protected String getSourceName() {
        return "TestSource";
    }

    @Override
    public SubTopologyMonitoringEvent createTopology(Config config, RecordingTopologyBuilder builder) {
        builder.setSpout(getSourceName(), 
            new TestSourceSource(getSourceName(), PIP), 1)
            .setNumTasks(1);
        return builder.createClosingEvent(PIP, config);
    }

    @Override
    public String getName() {
        return PIP;
    }

    @Override
    public String getMappingFileName() {
        return "testSource/mapping.xml";
    }

    @Override
    public void assertState(SystemState state, INameMapping mapping, long pipRunTime) {
        PipelineSystemPart pip = state.getPipeline(getName());
        Assert.assertNotNull(pip);
        //PipelineNodeSystemPart source = getNode(getSourceName(), pip, mapping, true);
        //assertGreaterEquals(1, source, TimeBehavior.THROUGHPUT_ITEMS);
        System.out.println(state.format());
    }

    @Override
    public void started() {
        // the other names are implementation names
        //EventManager.send(new AlgorithmChangeCommand(PIP, "processor", "RandomSubPipelineAlgorithm1"));
    }
    
    @Override
    public int plannedExecutionTime() {
        return 20000;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void handleOptions(Map conf, PipelineOptions opt) {
        // as done by profile control
        File dataFile = new File(Utils.getTestdataDir(), "testSource/profile.data");
        opt.setExecutorArgument(getSourceName(), "dataFile", dataFile.getAbsolutePath()); 
        opt.toConf(conf);
        conf.put(Config.TOPOLOGY_DEBUG, false);
    }
    
    @Override
    public boolean installGenericEoDEventHandler() {
        return true;
    }

}
