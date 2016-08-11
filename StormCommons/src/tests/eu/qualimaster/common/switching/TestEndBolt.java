package tests.eu.qualimaster.common.switching;

import java.util.Map;

import org.apache.log4j.Logger;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import eu.qualimaster.common.signal.BaseSignalBolt;
/**
 * A test ending Bolt.
 * @author Cui Qin
 *
 */
@SuppressWarnings("serial")
public class TestEndBolt extends BaseSignalBolt {
    private static final Logger LOGGER = Logger.getLogger(TestEndBolt.class);
    private transient OutputCollector collector;
    private String streamId;
    /**
     * Creates a test ending Bolt.
     * @param name the name of the ending Bolt
     * @param pipeline the pipeline it belongs to
     * @param streamId the stream id used to emit data streams
     */
    public TestEndBolt(String name, String pipeline, String streamId) {
        super(name, pipeline, true);
        this.streamId = streamId;
    }
    
    @Override
    @SuppressWarnings("rawtypes")
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector collector) {
        super.prepare(map, topologyContext, collector);
        this.collector = collector;
    }
    
    @Override
    public void execute(Tuple tuple) {
        LOGGER.info("Received data and emitting...");
        collector.ack(tuple);
    }
    
    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declareStream(streamId, new Fields("endBolt"));
    }

}
