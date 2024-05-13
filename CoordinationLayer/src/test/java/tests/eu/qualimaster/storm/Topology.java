package tests.eu.qualimaster.storm;

import eu.qualimaster.base.pipeline.RecordingTopologyBuilder;
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
public class Topology {

    /**
     * Creates the testing topology.
     * 
     * @param builder the topology builder
     */
    public static void createTopology(TopologyBuilder builder) {
        createTopology(builder, Naming.PIPELINE_NAME);
    }

    /**
     * Creates the testing topology.
     * 
     * @param builder the topology builder
     * @param pipelineName the name of the pipeline
     */
    public static void createTopology(TopologyBuilder builder, String pipelineName) {
        Source<Src> source = new Source<Src>(Src.class, pipelineName); // use Src2.class for fixed rate source
        builder.setSpout(Naming.NODE_SOURCE, source, 1).setNumTasks(1);
        Process process = new Process(Naming.NODE_PROCESS, pipelineName);
        builder.setBolt(Naming.NODE_PROCESS, process, 1).setNumTasks(3).shuffleGrouping(Naming.NODE_SOURCE);
        Sink sink = new Sink(pipelineName);
        builder.setBolt(Naming.NODE_SINK, sink, 1).setNumTasks(1).shuffleGrouping(Naming.NODE_PROCESS);
    }
    
    /**
     * Creates the testing topology with replay sink instead of usual sink.
     * 
     * @param builder the topology builder
     * @param pipelineName the name of the pipeline
     */
    public static void createReplayTopology(TopologyBuilder builder, String pipelineName) {
        Source<Src> source = new Source<Src>(Src.class, pipelineName); // use Src2.class for fixed rate source
        builder.setSpout(Naming.NODE_SOURCE, source, 1).setNumTasks(1);
        Process process = new Process(Naming.NODE_PROCESS, pipelineName);
        builder.setBolt(Naming.NODE_PROCESS, process, 1).setNumTasks(3).shuffleGrouping(Naming.NODE_SOURCE);
        RSink sink = new RSink(Naming.NODE_SINK, pipelineName, false);
        builder.setBolt(Naming.NODE_SINK, sink, 1).setNumTasks(1).shuffleGrouping(Naming.NODE_PROCESS);
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
            cluster.submitTopology(Naming.PIPELINE_NAME, config, b.createTopology());
        }
    }

    // checkstyle: resume exception type check
    
}
