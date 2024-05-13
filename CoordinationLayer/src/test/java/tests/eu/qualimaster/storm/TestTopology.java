package tests.eu.qualimaster.storm;

import eu.qualimaster.base.pipeline.RecordingTopologyBuilder;
import eu.qualimaster.easy.extension.internal.AlgorithmProfileHelper;
import eu.qualimaster.infrastructure.PipelineOptions;
import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.topology.TopologyBuilder;

/**
 * Creates the testing topology.
 * 
 * @author Holger Eichelberger
 */
public class TestTopology {

    public static final String PIP_NAME = TestSrc.PIP_NAME;
    private static boolean defaultInitAlgorithms = true;
    
    /**
     * Changes the behavior of initializing default algorithms.
     * 
     * @param init whether algorithms shall be initialized by default (true)
     */
    public static void setDefaultInitAlgorithms(boolean init) {
        defaultInitAlgorithms = init;
    }

    /**
     * Creates the testing topology.
     * 
     * @param builder the topology builder
     */
    public static void createTopology(TopologyBuilder builder) {
        TestSource source = new TestSource(PIP_NAME);
        builder.setSpout(AlgorithmProfileHelper.SRC_NAME, source, 1).setNumTasks(1);
        Process process = new Process(AlgorithmProfileHelper.FAM_NAME, PIP_NAME);
        builder.setBolt(AlgorithmProfileHelper.FAM_NAME, process, 1).setNumTasks(3)
            .shuffleGrouping(AlgorithmProfileHelper.SRC_NAME);
        // no sink
    }
    
    // checkstyle: stop exception type check

    /**
     * Creates a standalone topology.
     * 
     * @param args the topology arguments
     * @throws Exception in case of creation problems
     */
    public static void main(String[] args) throws Exception {
        Config config = new Config();
        Naming.setDefaultInitializeAlgorithms(config, defaultInitAlgorithms);
        config.setMessageTimeoutSecs(100);
        PipelineOptions options = new PipelineOptions(args);
        RecordingTopologyBuilder b = new RecordingTopologyBuilder(options);
        createTopology(b);
        b.close(args[0], config);
        
        // main topology: int numWorkers = options.getNumberOfWorkers(2);
        options.toConf(config);
        
        if (args != null && args.length > 0) {
            config.setNumWorkers(2);
            StormSubmitter.submitTopology(args[0], config, b.createTopology());
        } else {
            config.setMaxTaskParallelism(2);
            final LocalCluster cluster = new LocalCluster();
            cluster.submitTopology(PIP_NAME, config, b.createTopology());
        }
    }

    // checkstyle: resume exception type check
    
}
