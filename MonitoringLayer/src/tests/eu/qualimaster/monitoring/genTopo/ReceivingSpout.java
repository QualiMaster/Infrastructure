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
package tests.eu.qualimaster.monitoring.genTopo;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Map;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import eu.qualimaster.common.signal.BaseSignalSpout;
import eu.qualimaster.common.signal.ShutdownSignal;

/**
 * A hardware receiving spout.
 * 
 * @author Holger Eichelberger
 */
@SuppressWarnings("serial")
public class ReceivingSpout extends BaseSignalSpout {

    public static final String STREAM_NAME = "number";
    private boolean sendMonitoringEvents;
    private int port;
    private transient ServerSocket serverSocket;
    private transient Socket boltSocket;
    private transient ObjectInputStream in;
    private transient boolean running = false;
    private transient SpoutOutputCollector collector;
    
    /**
     * Creates a HW spout.
     * 
     * @param name the name of the processor
     * @param namespace the containing namespace
     * @param sendMonitoringEvents do send monitoring events
     * @param sendRegular whether this monitor shall care for sending regular events (<code>true</code>) or 
     *     not (<code>false</code>, for thrift-based monitoring)
     * @param port the conncection port
     */
    public ReceivingSpout(String name, String namespace, boolean sendMonitoringEvents, boolean sendRegular, int port) {
        super(name, namespace, sendRegular);
        this.sendMonitoringEvents = sendMonitoringEvents;
        this.port = port;
    }
    
    @SuppressWarnings("rawtypes")
    @Override
    public void open(Map stormConf, TopologyContext context, SpoutOutputCollector collector) {
        super.open(stormConf, context, collector);
        this.collector = collector;
        open();
    }

    /**
     * Opens the network connection.
     */
    private void open() {
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(1000);
            running = true;
            new Thread(new ServerRunnable()).start();
        } catch (IOException e) {
            System.err.println(getName() + " " + e.getMessage());
        }
    }

    /**
     * Implements the accepting server runnable.
     * 
     * @author Holger Eichelberger
     */
    private class ServerRunnable implements Runnable {

        @Override
        public void run() {
            while (running) {
                try {
                    boltSocket = serverSocket.accept();
                    in = new ObjectInputStream(boltSocket.getInputStream());
                    new Thread(new Receiver()).start();
                } catch (SocketTimeoutException e) {
                } catch (IOException e) {
                    System.err.println(getName() + " " + e.getMessage());
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                }
            }
        }
        
    }
    
    /**
     * Implements a receiver.
     * 
     * @author Holger Eichelberger
     */
    private class Receiver implements Runnable {

        @Override
        public void run() {
            while (running) {
                try {
                    int val = in.readInt();
                    startMonitoring(); // not before read, just that what can be done with the data
                    collector.emit(new Values(val));
                    System.err.println(getName() + " received " + val);
                    if (sendMonitoringEvents) {
                        endMonitoring();
                    }
                } catch (SocketTimeoutException e) {
                } catch (IOException e) {
                    System.err.println("Error " + getName() + " " + e.getMessage());
                    closeNet();
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                }
            }
        }
        
    }

    /**
     * Closes the network.
     */
    private void closeNet() {
        if (running) {
            running = false;
            close(in);
            close(boltSocket);
            close(serverSocket);
        }
    }
    
    /**
     * Closes a closeable.
     * 
     * @param closeable the closeable to close (may be <b>null</b>)
     */
    private void close(Closeable closeable) {
        if (null != closeable) {
            try {
                closeable.close();
            } catch (IOException e) {
                System.err.println("Error " + getName() + " " + e.getClass().getName() + " " + e.getMessage());
            }
        }        
    }
    
    @Override
    protected void prepareShutdown(ShutdownSignal signal) {
        closeNet();
    }

    @Override
    public void nextTuple() {
        // done asynchronously
    }
    
    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields(STREAM_NAME));
    }

}
