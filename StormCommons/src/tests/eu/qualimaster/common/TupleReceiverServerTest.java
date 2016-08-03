package tests.eu.qualimaster.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import backtype.storm.Config;
import eu.qualimaster.base.algorithm.GeneralTuple;
import eu.qualimaster.base.algorithm.IGeneralTuple;
import eu.qualimaster.base.serializer.KryoGeneralTupleSerializer;
import eu.qualimaster.common.switching.ITupleReceiverHandler;
import eu.qualimaster.common.switching.TupleReceiverServer;
import tests.eu.qualimaster.common.KryoTupleSerializerTest.DataItem;
import tests.eu.qualimaster.common.KryoTupleSerializerTest.DataItemSerializer;
import tests.eu.qualimaster.common.KryoTupleSerializerTest.IDataItem;
/**
 * Test the tuple receiver server.
 * @author Cui Qin
 *
 */
public class TupleReceiverServerTest {
    private KryoGeneralTupleSerializer genSer = null;
    /**
     * Tuple receiver handler.
     * @author Cui Qin
     *
     */
    private class TupleReceiverHandler implements ITupleReceiverHandler {
        private final Logger logger = Logger.getLogger(TupleReceiverHandler.class);
        private Socket socket;
        private InputStream in; 
        private Input kryoInput = null;
        private boolean cont = true;
        
        @Override
        public void run() {
            while (cont) {
                try {
                    if (null != kryoInput && kryoInput.canReadInt()) {
                        int len = kryoInput.readInt();
                        byte[] ser = new byte[len];
                        kryoInput.readBytes(ser);
                        IGeneralTuple tuple = genSer.deserialize(ser);
                        if (tuple != null) {
                            assertReceivedData(tuple);
                        }
                    }
                } catch (KryoException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void setSocket(Socket socket) {
            this.socket = socket;
            try {
                in = socket.getInputStream();
                kryoInput = new Input(in);
            } catch (IOException e) {
                e.printStackTrace();
            }
            
        }

        @Override
        public void stop() throws IOException {
            if (null != socket) {
                logger.info("Stopping handler");
                cont = false;
                in.close();
                in = null;
                kryoInput.close();
                kryoInput = null;
                socket.close();
                socket = null;
                logger.info("Stopped handler");
            }
            
        }

        @Override
        public boolean isStopped() {
            return null == socket;
        }
        
    }
    
    /**
     * A client for sending tuple.
     * @author Cui Qin
     *
     */
    public class TupleSenderClient {
        private String host;
        private int port;
        private Socket socket;
        private Output output;
        /**
         * Creates a client for sending tuple.
         * @param host the host  to connect to
         * @param port the port to connect to
         */
        public TupleSenderClient(String host, int port) {
            this.host = host;
            this.port = port;
        }
        /**
         * Connects with the server.
         * @return true if connected, otherwise false
         */
        private boolean connect() {
            Socket s = null;
            if (null == socket) {
                try {
                    s = new Socket(host, port);
                    output = new Output(s.getOutputStream());
                    socket = s;
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null != socket;
        }
        /**
         * Sends the serialized byte data.
         * @param bytes the byte data
         */
        public void send(byte[] bytes) {
            if (connect()) {
                try {
                    output.writeInt(bytes.length);
                    output.writeBytes(bytes);
                    output.flush();
                } catch (KryoException e) {
                    e.printStackTrace();
                }
            }
        }
        /**
         * Stops the client.
         */
        public void stop() {
            System.out.println("Stopping the client...");
            if (null != output) {
                output.close();
            }
            if ( null != socket) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Stopped the client...");
        }
    }
    
    /**
     * Test.
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void test() {
        int port = 8999;
        TupleReceiverHandler handler = new TupleReceiverHandler();
        TupleReceiverServer server = new TupleReceiverServer(handler, port);
        server.start();
        System.out.println("Server is started...");
        
        TupleSenderClient client = new TupleSenderClient("localhost", port);
        
        int count = 10; //sends 10 tuples
        while (count > 0) {
            //create a data item
            DataItem dataItem = new DataItem(1, "data");
            List<Object> tupleValues = new ArrayList<Object>();
            tupleValues.add(dataItem);
            
            //create a general tuple
            IGeneralTuple generalTuple = new GeneralTuple(tupleValues);
            //get the storm kryo map
            Map conf = StormTestUtils.createStormKryoConf();
            //register the custom serializer
            Config.registerSerialization(conf, DataItem.class, DataItemSerializer.class);
            
            //serialize a general tuple
            genSer = new KryoGeneralTupleSerializer(conf);
            byte[] byteSer = genSer.serialize(generalTuple);
            
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
     * Asserts the received data.
     * @param tuple the received data
     */
    private void assertReceivedData(IGeneralTuple tuple) {
        IDataItem item = (IDataItem) tuple.getValue(0);
        System.out.println("Received Data - id: " + item.getId() + ", value: " + item.getValue());
        Assert.assertEquals(1, item.getId());
        Assert.assertEquals("data", item.getValue());
    }
    

}
