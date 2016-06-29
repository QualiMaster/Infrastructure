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
package tests.eu.qualimaster.common;

import java.util.Map;

import org.json.simple.JSONValue;
import org.junit.Assert;
import org.junit.Test;

import backtype.storm.Config;
import backtype.storm.generated.Bolt;
import backtype.storm.generated.ComponentCommon;
import backtype.storm.generated.StormTopology;
import eu.qualimaster.infrastructure.PipelineOptions;

/**
 * Tests the {@link RecordingTopologyBuilder}.
 * 
 * @author Holger Eichelberger
 */
public class RecordingTopologyBuilderTest {

    /**
     * Tests the recording topology builder.
     */
    @Test
    @SuppressWarnings("rawtypes")
    public void testWithPipelineOptions() {
        final int numWorkers = 2;
        final String pipName = "TestPip";
        final int boltParallel = 3;
        final int boltTasks = 4;

        PipelineOptions options = new PipelineOptions();
        
        options.setNumberOfWorkers(numWorkers);
        options.setExecutorParallelism(TestPipeline.TEST_BOLT_IMPLNAME, boltParallel);
        options.setTaskParallelism(TestPipeline.TEST_BOLT_IMPLNAME, boltTasks);
        TestPipeline.main(options.toArgs(pipName));

        StormTopology topology = TestPipeline.getTopology();
        Assert.assertEquals(1, topology.get_bolts_size());
        Bolt bolt = topology.get_bolts().get(TestPipeline.TEST_BOLT_IMPLNAME);
        ComponentCommon common = bolt.get_common();
        Assert.assertEquals(boltParallel, common.get_parallelism_hint());
        Map jmap = (Map) JSONValue.parse(common.get_json_conf());
        Assert.assertEquals(Long.valueOf(boltTasks), jmap.get("topology.tasks"));
        
        Config conf = TestPipeline.getConfig();
        Assert.assertEquals(numWorkers, conf.get(Config.TOPOLOGY_WORKERS));
    }
    
}
