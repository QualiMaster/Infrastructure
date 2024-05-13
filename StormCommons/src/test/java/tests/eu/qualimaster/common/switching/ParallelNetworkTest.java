/*
 * Copyright 2009-2016 University of Hildesheim, Software Systems Engineering
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
package tests.eu.qualimaster.common.switching;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import backtype.storm.Config;
import eu.qualimaster.base.algorithm.GeneralTuple;
import eu.qualimaster.base.algorithm.IGeneralTuple;
import eu.qualimaster.base.serializer.KryoGeneralTupleSerializer;
import eu.qualimaster.common.switching.IGeneralTupleSerializerCreator;
import eu.qualimaster.common.switching.ITupleReceiveCreator;
import eu.qualimaster.common.switching.SynchronizedQueue;
import eu.qualimaster.common.switching.TupleReceiverHandlerCreator;
import eu.qualimaster.common.switching.TupleReceiverServer;
import eu.qualimaster.common.switching.TupleSender;
import tests.eu.qualimaster.common.StormTestUtils;
import tests.eu.qualimaster.common.KryoTupleSerializerTest.DataItem;
import tests.eu.qualimaster.common.KryoTupleSerializerTest.DataItemSerializer;

/**
 * Tests the switch network connections in parallel threads.
 * 
 * @author Holger Eichelberger
 */
public class ParallelNetworkTest implements IGeneralTupleSerializerCreator {
    
    private static final boolean REUSE_SERIALIZER = false; // just for debugging, true does not work
    
    private GeneralTuple tuple;
    @SuppressWarnings("rawtypes")
    private Map conf;
    private KryoGeneralTupleSerializer serializer;
    private SynchronizedQueue<IGeneralTuple> syn;

    /**
     * Sets up the test.
     */
    @Before
    public void setup() {
        // the tuple does not matter for this test, we just send always the same
        tuple = new GeneralTuple();
        tuple.setValues(new ArrayList<Object>());

        conf = StormTestUtils.createStormKryoConf();
        Config.registerSerialization(conf, DataItem.class, DataItemSerializer.class);

        ConcurrentLinkedQueue<IGeneralTuple> queue = new ConcurrentLinkedQueue<IGeneralTuple>();
        syn = new SynchronizedQueue<>(queue, 20); 
    }

    /**
     * Creates a general tuple serializer.
     * 
     * @return the general tuple serializer
     */
    public KryoGeneralTupleSerializer createGeneralTupleSerializer() {
        KryoGeneralTupleSerializer result;
        if (REUSE_SERIALIZER) {
            if (null == serializer) {
                serializer = new KryoGeneralTupleSerializer(conf);
            }
            result = serializer;
        } else {
            result = new KryoGeneralTupleSerializer(conf);
        }
        return result;
    }
    
    /**
     * Performs the test.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testNetwork() throws IOException {
        ITupleReceiveCreator creator = new TupleReceiverHandlerCreator(this, syn);
        TupleReceiverServer receiver = new TupleReceiverServer(creator, 8025);
        receiver.start();
        ReceiverRunnable queueReceiver = new ReceiverRunnable();
        new Thread(queueReceiver).start();
        
        SenderTestRunnable[] sender = new SenderTestRunnable[10];
        for (int i = 0; i < sender.length; i++) {
            sender[i] = new SenderTestRunnable(i);
            new Thread(sender[i]).start();
        }

        sleep(2000); // process some tuples
        
        int sent = 0;
        for (int i = 0; i < sender.length; i++) {
            sender[i].stop();
            sent += sender[i].getSentCount();
        }        

        sleep(100); // process rest queues
        
        queueReceiver.stop();
        receiver.stop();
        
        String summary = "sent " + sent + " received " + queueReceiver.getReceivedCount();
        System.out.println(summary);
        Assert.assertTrue(summary + " difference > 5", 
            Math.abs(queueReceiver.getReceivedCount() - sent) < 5); // shall be 0 but
    }

    /**
     * A receiving runnable emptying the receive queue.
     * 
     * @author Holger Eichelberger
     */
    private class ReceiverRunnable implements Runnable {

        private boolean run = true;
        private int received = 0;

        @Override
        public void run() {
            while (run) {
                if (syn.currentSize() > 0) {
                    syn.consume();
                    received++;
                }
            }
        }
        
        /**
         * Returns the number of tuples received.
         * 
         * @return the number of tuples received
         */
        private int getReceivedCount() {
            return received;
        }

        /**
         * Stop the runnable/thread.
         */
        private void stop() {
            run = false;
        }

    }
    
    /**
     * Runnable for parallelized sending.
     * 
     * @author Holger Eichelberger
     */
    private class SenderTestRunnable implements Runnable {
        
        private TupleSender sender;
        private boolean run = true;
        @SuppressWarnings("unused")
        private int id;
        private int sent = 0;
        private KryoGeneralTupleSerializer genSer = createGeneralTupleSerializer();
        
        /**
         * Creates a server runnable.
         * 
         * @param id the runnable id for debugging
         */
        private SenderTestRunnable(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            sender = new TupleSender("localhost", 8025);
            
            while (run) {
                sender.send(genSer.serialize(tuple));
                sent++;
                sleep(10);
            }
        }
        
        /**
         * Stop the runnable/thread.
         */
        private void stop() {
            run = false;
        }
        
        /**
         * Returns the number of sent items.
         * 
         * @return the number of sent items
         */
        private int getSentCount() {
            return sent;
        }
        
    }
    
    /**
     * Sleeps for a given time.
     * 
     * @param ms the time to sleep in ms
     */
    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }

}
