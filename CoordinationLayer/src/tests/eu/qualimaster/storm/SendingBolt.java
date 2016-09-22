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
package tests.eu.qualimaster.storm;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import eu.qualimaster.common.signal.AbstractMonitor;
import eu.qualimaster.common.signal.BaseSignalBolt;
import eu.qualimaster.common.signal.ShutdownSignal;
import eu.qualimaster.common.signal.SignalException;

/**
 * A hardware simulating bolt (sender).
 * 
 * @author Holger Eichelberger
 */
@SuppressWarnings("serial")
public class SendingBolt extends BaseSignalBolt {

    public static final String STREAM_NAME = "number";
    private boolean sendMonitoringEvents;
    private int port;
    private transient BlockingQueue<Integer> toSend;
    private transient OutputCollector collector;
    private transient Socket spoutSocket;
    private transient ObjectOutputStream out;
    private transient boolean running;

    /**
     * Creates a HW bolt.
     * 
     * @param name the name of the processor
     * @param namespace the containing namespace
     * @param sendMonitoringEvents do send monitoring events
     * @param sendRegular whether this monitor shall care for sending regular events (<code>true</code>) or 
     *     not (<code>false</code>, for thrift-based monitoring)
     * @param port the connection port
     */
    public SendingBolt(String name, String namespace, boolean sendMonitoringEvents, boolean sendRegular, int port) {
        super(name, namespace, sendRegular);
        this.sendMonitoringEvents = sendMonitoringEvents;
        this.port = port;
    }
    
    @SuppressWarnings("rawtypes")
    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        super.prepare(stormConf, context, collector);
        this.collector = collector;
        this.toSend = new LinkedBlockingQueue<Integer>();
        connect();
    }
    
    /**
     * Connects to the hw spout if not already connected.
     */
    private void connect() {
        if (null == spoutSocket) {
            try {
                spoutSocket = new Socket(InetAddress.getLocalHost(), port);
                out = new ObjectOutputStream(spoutSocket.getOutputStream());
                out.flush();
                running = true;
                System.err.println(getName() + " server created on " + port);
                new Thread(new Sender()).start();
            } catch (IOException e) {
                System.err.println(getName() + " " + e.getMessage());
                closeNet();
            }
        }
    }
    
    /**
     * Sends data to the hw spout.
     * 
     * @author Holger Eichelberger
     */
    private class Sender implements Runnable {

        private AbstractMonitor monitor = createThreadMonitor();
        
        @Override
        public void run() {
            while (running) {
                if (!toSend.isEmpty()) {
                    try {
                        Integer v = toSend.take();
                        monitor.startMonitoring();
                        out.writeInt(v);
                        out.flush();
                        monitor.endMonitoring(v); // @Cui monitoring, as in sink!
                    } catch (InterruptedException e) {
                    } catch (IOException e) {
                        System.err.println(getName() + " " + e.getMessage());
                        closeNet();
                    }
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                }
            }
        }
        
    }
    
    /**
     * Closes the network connection.
     */
    private void closeNet() {
        if (running) {
            running = false;
            close(out);
            close(spoutSocket);
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
                System.err.println(getName() + " " + e.getMessage());
            }
        }        
    }

    
    @Override
    protected void prepareShutdown(ShutdownSignal signal) {
        closeNet();
    }

    @Override
    public void execute(Tuple input) {
        startMonitoring();
        connect();
        toSend.add(input.getInteger(0));
        collector.ack(input);
        if (sendMonitoringEvents) {
            endMonitoring();
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields(STREAM_NAME));
    }
    
    /**
     * Sends a shutdown signal.
     * 
     * @param signal the signal
     */
    protected static void send(ShutdownSignal signal) {
        try {
            signal.sendSignal();
        } catch (SignalException e) {
            System.out.println(e.getMessage());
        }
    }

}
