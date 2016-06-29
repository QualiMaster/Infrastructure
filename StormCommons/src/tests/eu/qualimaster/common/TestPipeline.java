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

import backtype.storm.Config;
import backtype.storm.generated.StormTopology;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Tuple;
import eu.qualimaster.base.algorithm.IMainTopologyCreate;
import eu.qualimaster.base.algorithm.TopologyOutput;
import eu.qualimaster.base.pipeline.RecordingTopologyBuilder;
import eu.qualimaster.common.signal.BaseSignalBolt;
import eu.qualimaster.infrastructure.PipelineOptions;

/**
 * Used as a separate class for testing. Constructed according to a generated topology.
 * 
 * @author Holger Eichelberger
 */
public class TestPipeline {

    static final String TEST_BOLT_IMPLNAME = "PipelineVar_2_FamilyElement0";
    
    private static PipelineOptions options;
    private static StormTopology topology;
    private static Config config;

    /**
     * A Bolt.
     * 
     * @author Holger Eichelberger
     */
    @SuppressWarnings("serial")
    private static class PipelineVar2FamilyElement0FamilyElement extends BaseSignalBolt {

        /**
         * Creates the bolt.
         * 
         * @param name the bolt name
         * @param namespace the surrounding namespace
         */
        public PipelineVar2FamilyElement0FamilyElement(String name, String namespace) {
            super(name, namespace);
        }
        
        @Override
        public void execute(Tuple input) {
        }

        @Override
        public void declareOutputFields(OutputFieldsDeclarer declarer) {
        }
        
    }
    
    /**
     * Creates the topology.
     * 
     * @author Holger Eichelberger
     */
    public static class MainTopologyCreator implements IMainTopologyCreate {

        @Override
        public TopologyOutput createMainTopology() {
            int numWorkers = 1;
            Config config = new Config();
            RecordingTopologyBuilder builder = new RecordingTopologyBuilder(options);
            
            builder.setBolt("PipelineVar_2_FamilyElement0", 
                new PipelineVar2FamilyElement0FamilyElement("PipelineVar_2_FamilyElement0", "SwitchPip"), 1)
                .setNumTasks(1);
            
            return new TopologyOutput(config, builder, numWorkers);
        }
        
    }
    
    /**
     * Creates the pipeline (but does not submit it).
     * 
     * @param args the pipeline arguments
     */
    public static void main(String[] args) {
        options = new PipelineOptions(args);
        MainTopologyCreator topoCreator = new MainTopologyCreator();
        TopologyOutput topo = topoCreator.createMainTopology();
        config = topo.getConfig();
        topology = topo.getBuilder().createTopology();
        config.setNumWorkers(options.getNumberOfWorkers(1));
    }
    
    /**
     * Returns the topology. Valid only after {@link #main(String[])}.
     * 
     * @return the topology
     */
    public static StormTopology getTopology() {
        return topology;
    }
    
    /**
     * Returns the config. Valid only after {@link #main(String[])}.
     * 
     * @return the config
     */
    public static Config getConfig() {
        return config;
    }
    
}
