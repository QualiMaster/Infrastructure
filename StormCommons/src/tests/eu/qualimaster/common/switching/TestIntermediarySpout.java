package tests.eu.qualimaster.common.switching;  

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import eu.qualimaster.base.algorithm.IGeneralTuple;
import eu.qualimaster.base.algorithm.ISwitchTuple;
import eu.qualimaster.common.switching.AbstractSwitchMechanism;
import eu.qualimaster.common.switching.AbstractSwitchStrategy;
import eu.qualimaster.common.switching.BaseSwitchSpout;
import eu.qualimaster.common.switching.ParallelTrackSwitchMechanism;
import eu.qualimaster.common.switching.SeparateIntermediaryStrategy;
import eu.qualimaster.common.switching.TupleReceiverServer;
import tests.eu.qualimaster.common.KryoTupleSerializerTest.IDataItem;
import eu.qualimaster.common.switching.IState.SwitchState;
/**
 * Provides a test intermediary Spout.
 * @author Cui Qin
 *
 */
@SuppressWarnings("serial")
public class TestIntermediarySpout extends BaseSwitchSpout {
    protected static final int PORT = 8999;
    private static final Logger LOGGER = Logger.getLogger(TestIntermediarySpout.class);
    @SuppressWarnings("unused")
    private SpoutOutputCollector collector;
    private String streamId;
    private AbstractSwitchMechanism mechanism;
    private AbstractSwitchStrategy strategy;
    private TupleReceiverServer server;
    private int count = 0;
    private boolean isClosed = false;
    
    /**
     * Creates an intermediary Spout with streamId.
     * @param name the name of the intermediary Spout
     * @param namespace the namespace
     * @param streamId the stream id used to emit the data streams
     */
    public TestIntermediarySpout(String name, String namespace, String streamId) {
        super(name, namespace, true);
        this.streamId = streamId;
    }
    
    @SuppressWarnings("rawtypes")
    @Override
    public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
        super.open(conf, context, collector);
        this.collector = collector;
        count = 0;
        strategy = new SeparateIntermediaryStrategy(conf, SwitchState.ACTIVE_DEFAULT);
        mechanism = new ParallelTrackSwitchMechanism(strategy);
        setSwitchMechanism(mechanism);
        server = new TupleReceiverServer(strategy.getTupleReceiverHandler(), PORT);
        server.start();
    }
    
    @Override
    public void nextTuple() {
        if (!isClosed) {
            IGeneralTuple tuple = mechanism.getNextTuple();
            if (tuple != null) {
                IDataItem item = (IDataItem) tuple.getValue(0);
                LOGGER.info("id: " + item.getId() + ", value: " + item.getValue());
                count++;
                if (tuple.isGeneralTuple()) {
                    collector.emit(streamId, tuple.getValues());
                } else {
                    ISwitchTuple swiTuple = (ISwitchTuple) tuple;
                    Object msgId = swiTuple.getId();
                    collector.emit(streamId, tuple.getValues(), msgId);
                }
                if (count == BaseSwitchSpoutTest.TUPLE_SIZE * 2) {
                    isClosed = true;
                }
            }
        }
    }
    
    @Override
    public void ack(Object msgId) {
        LOGGER.info("Acking the processed tuple: " + msgId);
        super.ack(msgId);
    }
    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declareStream(streamId, new Fields("tuple"));
    }
    
    @Override
    public void close() {
        super.close();
        isClosed = true;
        try {
            server.stop();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
