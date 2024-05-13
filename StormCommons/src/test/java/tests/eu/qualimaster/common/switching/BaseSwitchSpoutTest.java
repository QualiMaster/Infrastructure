package tests.eu.qualimaster.common.switching;

import java.util.Map;

import org.junit.Test;

import backtype.storm.Config;
import eu.qualimaster.base.serializer.IGeneralTupleSerializer;
import eu.qualimaster.base.serializer.ISwitchTupleSerializer;
import eu.qualimaster.base.serializer.KryoGeneralTupleSerializer;
import eu.qualimaster.base.serializer.KryoSwitchTupleSerializer;
import tests.eu.qualimaster.common.StormTestUtils;
import tests.eu.qualimaster.common.TupleSenderAndReceiverTest;
import tests.eu.qualimaster.common.KryoTupleSerializerTest.DataItem;
import tests.eu.qualimaster.common.KryoTupleSerializerTest.DataItemSerializer;
import eu.qualimaster.common.switching.TupleSender;
/**
 * Test base switch spout.
 * @author Cui Qin
 *
 */
public class BaseSwitchSpoutTest {
    protected static final int TUPLE_SIZE = 10;
    /**
     * Sends test tuples to the running switch spout. Run after {@link TestTopology} is executing.
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void test() {
        //get the storm kryo map
        Map conf = StormTestUtils.createStormKryoConf();
        
        //register the custom serializer
        Config.registerSerialization(conf, DataItem.class, DataItemSerializer.class);
        
        //serializers
        IGeneralTupleSerializer genSer = new KryoGeneralTupleSerializer(conf);
        ISwitchTupleSerializer swiSer = new KryoSwitchTupleSerializer(conf);
        
        TupleSender client = new TupleSender("localhost", TestIntermediarySpout.PORT);
        
        //send 10 general tuples to the running pipeline
        System.out.println("Sending " + TUPLE_SIZE + " general tuples with the id: 1 and the string value: data.");
        TupleSenderAndReceiverTest.sendGeneralTuple(client, genSer, TUPLE_SIZE);   
        
        try {
            Thread.sleep(100);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        
        //send the switch flag
        client.sendSwitchTupleFlag();
        //send 10 switch tuples to the running pipeline
        System.out.println("Sending " + TUPLE_SIZE + " switch tuples with the id: 1 and the string value: data.");
        TupleSenderAndReceiverTest.sendSwitchTuple(client, swiSer, TUPLE_SIZE);
        
        client.stop();
    }

}
