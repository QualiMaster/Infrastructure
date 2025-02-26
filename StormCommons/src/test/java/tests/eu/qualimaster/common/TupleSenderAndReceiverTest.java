package tests.eu.qualimaster.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.Assert;
import org.junit.Test;

import backtype.storm.Config;
import eu.qualimaster.base.algorithm.GeneralTuple;
import eu.qualimaster.base.algorithm.IGeneralTuple;
import eu.qualimaster.base.algorithm.ISwitchTuple;
import eu.qualimaster.base.algorithm.SwitchTuple;
import eu.qualimaster.base.serializer.IGeneralTupleSerializer;
import eu.qualimaster.base.serializer.ISwitchTupleSerializer;
import eu.qualimaster.common.switching.IGeneralTupleSerializerCreator;
import eu.qualimaster.common.switching.ISwitchTupleSerializerCreator;
import eu.qualimaster.common.switching.ITupleReceiveCreator;
import eu.qualimaster.common.switching.KryoGeneralTupleSerializerCreator;
import eu.qualimaster.common.switching.KryoSwitchTupleSerializerCreator;
import eu.qualimaster.common.switching.SynchronizedQueue;
import eu.qualimaster.common.switching.TupleReceiverHandlerCreator;
import eu.qualimaster.common.switching.TupleReceiverServer;
import eu.qualimaster.common.switching.TupleSender;
import eu.qualimaster.common.switching.tupleReceiving.ITupleReceiverHandler;
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
    private transient SynchronizedQueue<IGeneralTuple> tmpSyn = null;
    private transient Queue<IGeneralTuple> queue = null;
    private transient Queue<IGeneralTuple> tmpQueue = null;
 
    /**
    * A thread consumes tuples.
    **/
    public class ConsumeTuple implements Runnable {           
        @Override
        public void run() {
            while (true) {
                IGeneralTuple tuple;
                if (syn.currentSize() > 0) {
                    tuple = syn.consume();
                    //assert the received data
                    assertReceivedData(tuple);
                }
                if (tmpSyn.currentSize() > 0) {
                    tuple = tmpSyn.consume();
                    //assert the received data
                    assertReceivedData(tuple);
                }
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
        IGeneralTupleSerializerCreator genSerC = new KryoGeneralTupleSerializerCreator(conf);
        ISwitchTupleSerializerCreator swiSerC = new KryoSwitchTupleSerializerCreator(conf);
        
        //queue to store the tuple
        queue = new ConcurrentLinkedQueue<IGeneralTuple>();
        tmpQueue = new ConcurrentLinkedQueue<IGeneralTuple>();
        syn = new SynchronizedQueue<IGeneralTuple>(queue, 20);
        tmpSyn = new SynchronizedQueue<IGeneralTuple>(tmpQueue, 5);
        
        //creates the handler for receiving tuples, including general and switch tuple
        ITupleReceiveCreator creator = new TupleReceiverHandlerCreator(genSerC, swiSerC, syn, tmpSyn);        
        TupleReceiverServer server = new TupleReceiverServer(creator, port);
        server.start();
        System.out.println("Server is started...");
        
        IGeneralTupleSerializer genSer = genSerC.createGeneralTupleSerializer();
        ISwitchTupleSerializer swiSer = swiSerC.createSwitchTupleSerializer();
        TupleSender client = new TupleSender("localhost", port);
        
        Thread consumeTupleThread = new Thread(new ConsumeTuple());
        consumeTupleThread.start();
        
        //send 10 general tuples
        sendGeneralTuple(client, genSer, 10);
        
        //send a switch flag
        client.sendSwitchTupleFlag();
        //send 10 switch tuples
        sendSwitchTuple(client, swiSer, 10);
       
        try {
            Thread.sleep(100);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        
        //send a switch flag
        client.sendSwitchTupleFlag();
        //send a temporary queue flag
        client.sendTemporaryQueueFlag();
        //send 10 switch tuples
        sendSwitchTuple(client, swiSer, 10);
        
        try {
            Thread.sleep(100);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        
        client.stop();
        
        try {
            server.stop();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        testSwitchTupleReceiver(creator, swiSer);
    }
    
    /**
     * Test sending only switch tuple {@link ISwitchTuple}.
     * @param creator the creator
     * @param swiSer the {@link ISwitchTuple} serializer
     */
    public void testSwitchTupleReceiver(ITupleReceiveCreator creator, ISwitchTupleSerializer swiSer) {
        int port = 8998;
        //send only ISwitchTuple
        ITupleReceiverHandler swiHandler = creator.create(true);        
        TupleReceiverServer swiServer = new TupleReceiverServer(creator, port, true);
        swiServer.start();
        System.out.println("Switch Server is started...");
        
        TupleSender client = new TupleSender("localhost", port);
        
        sendSwitchTuple(client, swiSer, 10);
        
        try {
            Thread.sleep(100);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        
        client.stop();
        
        try {
            swiHandler.stop();
            //swiServer.stop();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Sends general tuples based on the given amount.
     * @param client the client to send the tuple
     * @param genSer the serializer for the general tuple
     * @param amount the amount of tuple to send
     */
    public static void sendGeneralTuple(TupleSender client, IGeneralTupleSerializer genSer, int amount) {
        while (amount > 0) {
            //create a switch tuple
            IGeneralTuple generalTuple = createGeneralTuple();
            byte[] byteSer = genSer.serialize(generalTuple);
            
            client.send(byteSer);
            amount--;            
        }
    }
    /**
     * Sends switch tuples based on the given amount.
     * @param client the client to send the tuple
     * @param swiSer the serializer for the switch tuple
     * @param amount the amount of tuple to send
     */
    public static void sendSwitchTuple(TupleSender client, ISwitchTupleSerializer swiSer, int amount) {
        while (amount > 0) {
            //create a switch tuple
            ISwitchTuple switchTuple = createSwitchTuple(amount);
            byte[] byteSer = swiSer.serialize(switchTuple);
            
            client.send(byteSer);
            amount--;            
        }
    }
    /**
     * Creates a general tuple.
     * @return a general tuple
     */
    private static IGeneralTuple createGeneralTuple() {
        IGeneralTuple tuple = new GeneralTuple(createTupleValues());
        return tuple;
    }
    /**
     * Creates a switch tuple.
     * @param id the msgId used to trace the tuple for acknowledgement
     * @return a switch tuple
     */
    private static ISwitchTuple createSwitchTuple(int id) {
        ISwitchTuple tuple = new SwitchTuple(id, createTupleValues());
        return tuple;
    }
    /**
     * Creates the tuple values.
     * @return the tuple values
     */
    private static List<Object> createTupleValues() {
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
