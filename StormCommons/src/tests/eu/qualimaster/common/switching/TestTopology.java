package tests.eu.qualimaster.common.switching;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.generated.AlreadyAliveException;
import backtype.storm.generated.InvalidTopologyException;
import backtype.storm.topology.TopologyBuilder;
import eu.qualimaster.base.algorithm.IMainTopologyCreate;
import eu.qualimaster.base.algorithm.TopologyOutput;
import eu.qualimaster.base.pipeline.RecordingTopologyBuilder;
import eu.qualimaster.infrastructure.PipelineOptions;
import tests.eu.qualimaster.common.KryoTupleSerializerTest.DataItem;
import tests.eu.qualimaster.common.KryoTupleSerializerTest.DataItemSerializer;
/**
 * A test topology.
 * @author Cui Qin
 *
 */
public class TestTopology {
    protected static Config config = null;
    private static final String TOPOLOGY_NAME = "TestPip";
    private static PipelineOptions options = null;
    
    /**
     * Define a class for creating the main topology.
     */
    public static class MainTopologyCreator implements IMainTopologyCreate {

        @Override
        public TopologyOutput createMainTopology() {
            Config config = new Config();
            config.setMessageTimeoutSecs(100);
            Config.registerSerialization(config, DataItem.class, DataItemSerializer.class);
            RecordingTopologyBuilder builder = new RecordingTopologyBuilder(options);
            builder.setSpout("IntermediarySpout", new TestIntermediarySpout("IntermediarySpout", TOPOLOGY_NAME, 
                    "IntermediarySpoutStreamId"), 1).setNumTasks(1);
            return new TopologyOutput(config, builder, 1);
        }
        
    }
    /**
     * Returns the storm config.
     * @return the storm config
     */
    public static Config getConfig() {
        return config;
    }
    
    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {
      //create the main topology.
        options = new PipelineOptions(args);
        MainTopologyCreator topoCreator = new MainTopologyCreator();
        TopologyOutput topo = topoCreator.createMainTopology();
        //get the topology information
        config = topo.getConfig();
        TopologyBuilder builder = topo.getBuilder();
        int defNumWorkers = topo.getNumWorkers();
        options.toConf(config);
        
        
        if (args != null && args.length > 0) {
            config.setNumWorkers(defNumWorkers);
            try {
                StormSubmitter.submitTopology(args[0], config, builder.createTopology());
            } catch (AlreadyAliveException | InvalidTopologyException e) {
                e.printStackTrace();
            }
        } else {
            final LocalCluster cluster = new LocalCluster();
            cluster.submitTopology(TOPOLOGY_NAME, config, builder.createTopology());
        }
    }
}
