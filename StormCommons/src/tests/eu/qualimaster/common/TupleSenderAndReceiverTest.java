package tests.eu.qualimaster.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import backtype.storm.Config;
import eu.qualimaster.base.algorithm.GeneralTuple;
import eu.qualimaster.base.algorithm.IGeneralTuple;
import eu.qualimaster.base.algorithm.ISwitchTuple;
import eu.qualimaster.base.algorithm.SwitchTuple;
import eu.qualimaster.base.serializer.IGeneralTupleSerializer;
import eu.qualimaster.base.serializer.ISwitchTupleSerializer;
import eu.qualimaster.base.serializer.KryoGeneralTupleSerializer;
import eu.qualimaster.base.serializer.KryoSwitchTupleSerializer;
import eu.qualimaster.common.switching.ITupleReceiverHandler;
import eu.qualimaster.common.switching.SynchronizedQueue;
import eu.qualimaster.common.switching.TupleReceiverHandler;
import eu.qualimaster.common.switching.TupleReceiverServer;
import eu.qualimaster.common.switching.TupleSender;
import tests.eu.qualimaster.common.KryoTupleSerializerTest.DataItem;
import tests.eu.qualimaster.common.KryoTupleSerializerTest.DataItemSerializer;
import tests.eu.qualimaster.common.KryoTupleSerializerTest.IDataItem;
/**
 * Test the tuple receiver server.
 * @author Cui Qin
 *
 */
public class TupleSenderAndReceiverTest {
    private transient SynchronizedQueue<IGeneralTuple> syn = null;
 
    /**
    * A thread consumes tuples.
    **/
    public class ConsumeTuple implements Runnable {           
        @Override
        public void run() {
            while (true) {
                IGeneralTuple tuple = syn.consume();
                //assert the received data
                assertReceivedData(tuple);
            }
        }
    }
    
    /**
     * Test.
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void test() {
        int port = 8999;

        //get the storm kryo map
        Map conf = StormTestUtils.createStormKryoConf();
        
        //register the custom serializer
        Config.registerSerialization(conf, DataItem.class, DataItemSerializer.class);
        
        //serializers
        IGeneralTupleSerializer genSer = new KryoGeneralTupleSerializer(conf);
        ISwitchTupleSerializer swiSer = new KryoSwitchTupleSerializer(conf);
        
        //queue to store the tuple
        Queue<IGeneralTuple> queue = new ConcurrentLinkedQueue<IGeneralTuple>();
        syn = new SynchronizedQueue<IGeneralTuple>(queue, 20); 
        
        //creates the handler for receiving tuples, including general and switch tuple
        TupleReceiverHandler handler = new TupleReceiverHandler(genSer, swiSer, syn);
        
        TupleReceiverServer server = new TupleReceiverServer(handler, port);
        server.start();
        System.out.println("Server is started...");
        
        TupleSender client = new TupleSender("localhost", port);
        
        Thread consumeTupleThread = new Thread(new ConsumeTuple());
        consumeTupleThread.start();
        
        int count = 10; //sends 10 general tuples
        while (count > 0) {           
            //create a general tuple
            IGeneralTuple generalTuple = createGeneralTuple();
            byte[] byteSer = genSer.serialize(generalTuple);
            
            client.send(byteSer);
            count--;            
        }
        
        //send a switch flag
        String flag = "switch";
        client.send(flag.getBytes());
        
        count = 10; //sends 10 switch tuples
        while (count > 0) {
            //create a switch tuple
            ISwitchTuple switchTuple = createSwitchTuple();
            byte[] byteSer = swiSer.serialize(switchTuple);
            
            client.send(byteSer);
            count--;            
        }
        
        try {
            Thread.sleep(100);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }

        client.stop();
        
        try {
            handler.stop();
            //server.stop();
        } catch (IOException e) {
            e.printStackTrace();
        }
      
    }
    /**
     * Creates a general tuple.
     * @return a general tuple
     */
    private IGeneralTuple createGeneralTuple() {
        IGeneralTuple tuple = new GeneralTuple(createTupleValues());
        return tuple;
    }
    /**
     * Creates a switch tuple.
     * @return a switch tuple
     */
    private ISwitchTuple createSwitchTuple() {
        ISwitchTuple tuple = new SwitchTuple(0, createTupleValues());
        return tuple;
    }
    /**
     * Creates the tuple values.
     * @return the tuple values
     */
    private List<Object> createTupleValues() {
        //create a data item
        DataItem dataItem = new DataItem(1, "data");
        List<Object> tupleValues = new ArrayList<Object>();
        tupleValues.add(dataItem);
        return tupleValues;
    }
    /**
     * Asserts the received data.
     * @param tuple the received data
     */
    private void assertReceivedData(IGeneralTuple tuple) {
        IDataItem item = (IDataItem) tuple.getValue(0);
        System.out.println("Received Data - IS GENERAL: " + tuple.isGeneralTuple() + ", id: " + item.getId() 
            + ", value: " + item.getValue());
        Assert.assertEquals(1, item.getId());
        Assert.assertEquals("data", item.getValue());
    }
    

}
