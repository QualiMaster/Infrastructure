/*
 * Copyright 2009-2015 University of Hildesheim, Software Systems Engineering
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
package tests.eu.qualimaster.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.google.protobuf.ByteString;

import eu.qualimaster.common.hardware.HardwareControlConnection;
import eu.qualimaster.common.hardware.IsRunningAlgorithmIn;
import eu.qualimaster.common.hardware.IsRunningAlgorithmOut;
import eu.qualimaster.common.hardware.MessageTable;
import eu.qualimaster.common.hardware.MessageTable.Code;
import eu.qualimaster.common.hardware.StopMessageIn;
import eu.qualimaster.common.hardware.StopMessageOut;
import eu.qualimaster.common.hardware.UploadMessageIn;
import eu.qualimaster.common.hardware.UploadMessageOut;
import eu.qualimaster.common.hardware.Utils;
import eu.qualimaster.dataManagement.serialization.SerializerRegistry;

/**
 * Tests the hardware connection. This test is mostly based on the way the hardware control connection is working, 
 * e.g., to utilize two ports for communication.
 * 
 * @author Holger Eichelberger
 */
public class HardwareConnectionTest {

    private static List<FakeHandler> senders = Collections.synchronizedList(new ArrayList<FakeHandler>());
    
    /**
     * Implements an abstract fake handler.
     * 
     * @author Holger Eichelberger
     */
    private abstract static class FakeHandler implements Runnable {

        private boolean running = true;
        private Socket socket;

        /**
         * Creates a handler.
         * 
         * @param socket the socket to handle
         */
        protected FakeHandler(Socket socket) {
            this.socket = socket;
        }
        
        /**
         * Stops processing.
         */
        public void stop() {
            running = false;
            if (null != socket) {
                try {
                    socket.close();
                } catch (IOException e) {
                }
                socket = null;
            }
        }
        
        @Override
        public void run() {
            while (running) {
                try {
                    handle();
                } catch (SocketTimeoutException e) {
                    // just go on
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
        /**
         * Sends a chunk of data.
         * 
         * @param data the data to be sent
         * @throws IOException in case that sending fails
         */
        protected void send(byte [] data) throws IOException {
            // from Transmitter
            if (null != socket) {
                socket.getOutputStream().write(data);
            }
        }
        
        /**
         * Receives a chunk of data.
         * 
         * @return the chunk of data
         * @throws IOException if receiving fails
         */
        protected byte [] receiveData() throws IOException {
            // from Receiver
            byte[] msg = new byte[1024];
            byte[] temp = new byte[1];

            int counter = 0;
            InputStream in = socket.getInputStream();
            while (true) {
                if (in.available() > 0) {
                    int len = in.read(temp , 0, 1);
                    if (len > 0) {
                        if (temp[0] == '\0') {
                            break;
                        } else {
                            msg[counter] = temp[0];
                            counter++;
                        }
                    }
                }
            }
            if (0 == counter) {
                msg = null;
            }
            return msg;
        }
        
        /**
         * Called to handle the processing.
         * 
         * @throws IOException in case of I/O problems
         */
        protected abstract void handle() throws IOException;
  
        /**
         * Returns whether this instance shall be started in a thread.
         * 
         * @return <code>true</code> for start, <code>false</code> else
         */
        protected abstract boolean doStart();

    }
    
    /**
     * Creates a fake handler.
     * 
     * @author Holger Eichelberger
     */
    private interface IHandlerCreator {

        /**
         * Creates the handler.
         * 
         * @param sock the socket to create the handler for
         * @return the handler
         */
        public FakeHandler createHandler(Socket sock);
        
    }
    
    /**
     * Implements a fake server.
     * 
     * @author Holger Eichelberger
     */
    public static class FakeServer implements Runnable {

        public static final int BASE_IN_PORT = 1234;
        public static final int BASE_OUT_PORT = 1235;
        private int port;
        private IHandlerCreator creator;
        private ServerSocket serverSocket;
        private boolean cont = true;
        private List<FakeHandler> handlers = new ArrayList<FakeHandler>();

        /**
         * Creates the fake server.
         * 
         * @param port the communication port
         * @param creator the handler creator
         * @throws IOException in case of I/O problems
         */
        private FakeServer(int port, IHandlerCreator creator) throws IOException {
            this.port = port;
            this.creator = creator;
            serverSocket = new ServerSocket(port);
            System.out.println("Server socket created on " + port);            
        }
        
        /**
         * Starts the fake server.
         */
        public void start() {
            new Thread(this).start();
            System.out.println("Server thread started " + port);
        }
        
        /**
         * Stops the fake server.
         * 
         * @throws IOException in case of I/O problems
         */
        public void stop() throws IOException {
            System.out.println("Stopping server");
            cont = false;
            for (FakeHandler handler : handlers) {
                handler.stop();
            }
            handlers.clear();
            serverSocket.close();
            System.out.println("Stopped server");
        }

        /**
         * Executes the server thread, i.e., accepts connections.
         */
        public void run() {
            while (cont) {
                try {
                    Socket sock = serverSocket.accept();
                    //sock.setSoTimeout(200);
                    System.out.println("Socket connection accepted " + port);
                    FakeHandler handler = creator.createHandler(sock);
                    handlers.add(handler);
                    System.out.println("Socket connection handler started");
                    if (handler.doStart()) {
                        new Thread(handler).start();
                    }
                } catch (SocketException e) {
                    try {
                        stop();
                    } catch (IOException e1) {
                        // ignore, stopping anyway
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
    }
    
    /**
     * Implements the sending handler.
     * 
     * @author Holger Eichelberger
     */
    private static class SendingHandler extends FakeHandler {

        /**
         * Creates the handler.
         * 
         * @param socket the socket to work on
         */
        public SendingHandler(Socket socket) {
            super(socket);
            senders.add(this);
        }

        @Override
        protected void handle() {
        }

        @Override
        protected boolean doStart() {
            return false;
        }

    }
    
    /**
     * Implements a receiving handler.
     * 
     * @author Holger Eichelberger
     */
    private static class ReceivingHandler extends FakeHandler {

        private Set<String> running = new HashSet<String>();
        private FakeHandler sender;
        
        /**
         * Creates the handler.
         * 
         * @param socket the socket to work on
         */
        public ReceivingHandler(Socket socket) {
            super(socket);
            sender = catchHandler(senders);
        }
        
        @Override
        protected boolean doStart() {
            return true;
        }

        @Override
        protected void handle() throws IOException {
            byte [] msg = receiveData();
            if (null != msg) {
                ByteArrayInputStream in = new ByteArrayInputStream(msg);
                in.skip(2);
                String token = new String(msg, 0, 2);
                if (token.equalsIgnoreCase("ca") && in != null) {
                    handleUpload(in);
                } else if (token.equalsIgnoreCase("cb")) {
                    handleStop(in);
                } else if (token.equalsIgnoreCase("cc")) {
                    handleIsRunning(in);
                } else if (token.equalsIgnoreCase("cd")) {
                    handleTerminate(in);
                }
            }
        }
        
        /**
         * Handles an upload message.
         * 
         * @param in the input array stream
         * @throws IOException in case of I/O problems
         */
        private void handleUpload(ByteArrayInputStream in) throws IOException {
            UploadMessageIn tmp = SerializerRegistry.getSerializer(UploadMessageIn.class).deserializeFrom(in);
            if (null != tmp) {
                UploadMessageOut response;
                String id = tmp.getId();
                if (!Utils.isValid(id)) {
                    response = new UploadMessageOut(MessageTable.Code.UPLOAD_ERROR.toMsg());
                } else {
                    if (running.contains(id)) {
                        response = new UploadMessageOut(MessageTable.Code.UPLOAD_ERROR.toMsg());
                    } else {
                        int pCount = tmp.getPortCount();
                        int[] ports = new int[pCount];
                        for (int p = 0; p < pCount; p++) {
                            ports[p] = FakeServer.BASE_OUT_PORT + p;
                        }
                        response = new UploadMessageOut(FakeServer.BASE_IN_PORT, ports);
                        running.add(id);
                    }
                }
                send("ra", response, UploadMessageOut.class);
            }
        }

        /**
         * Handles a stop message.
         * 
         * @param in the input array stream
         * @throws IOException in case of I/O problems
         */
        private void handleStop(ByteArrayInputStream in) throws IOException {
            StopMessageIn tmp = SerializerRegistry.getSerializer(StopMessageIn.class).deserializeFrom(in);
            if (null != tmp) {
                StopMessageOut response;
                String id = tmp.getId();
                if (!Utils.isValid(id)) {
                    response = new StopMessageOut(MessageTable.Code.STOP_ERROR.toMsg());
                } else {
                    if (running.contains(id)) {
                        response = new StopMessageOut(MessageTable.Code.SUCCESS.toMsg());
                        running.remove(id);
                    } else {
                        response = new StopMessageOut(MessageTable.Code.STOP_ERROR.toMsg());
                    }
                }
                send("rb", response, StopMessageOut.class);
            }
        }

        /**
         * Handles a is-running message.
         * 
         * @param in the input array stream
         * @throws IOException in case of I/O problems
         */
        private void handleIsRunning(ByteArrayInputStream in) throws IOException {
            IsRunningAlgorithmIn tmp = SerializerRegistry.getSerializer(IsRunningAlgorithmIn.class)
                .deserializeFrom(in);
            if (null != tmp) {
                IsRunningAlgorithmOut response;
                String id = tmp.getId();
                if (!Utils.isValid(id)) {
                    response = new IsRunningAlgorithmOut(false);
                } else {
                    response = new IsRunningAlgorithmOut(running.contains(id));
                }
                send("rc", response, IsRunningAlgorithmOut.class);
            }
        }

        /**
         * Handles a termination message.
         * 
         * @param in the input array stream
         * @throws IOException in case of I/O problems
         */
        private void handleTerminate(ByteArrayInputStream in) throws IOException {
            send("rd", null, Object.class);
            stop();
            if (null != sender) {
                sender.stop();
            }
        }

        /**
         * Sends a token and an object to a sending handler.
         * 
         * @param <T> the object type
         * @param token the token
         * @param object the object (may be <b>null</b>, the only the token is sent)
         * @param cls the type of the object
         * @throws IOException in case of I/O problems
         */
        private <T> void send(String token, T object, Class<T> cls) throws IOException {
            if (null == sender) {
                sender = catchHandler(senders);
            }
            if (null != sender) {
                sender.send(token.getBytes());
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                if (null != object) {
                    SerializerRegistry.getSerializer(cls).serializeTo(object, out);
                }
                out.write(0);
                sender.send(out.toByteArray());
            } else {
                throw new IOException("NO SENDER");
            }
        }

    }
    
    /**
     * Catches the next handler if possible.
     * 
     * @param handlers the handlers
     * @return the obtained handler (may be <b>null</b> if there is none
     */
    private static FakeHandler catchHandler(List<FakeHandler> handlers) {
        FakeHandler result;
        if (handlers.size() > 0) {
            result = handlers.remove(0);
        } else {
            result = null;
        }
        return result;
    }

    /**
     * Returns a fake sending server for a given <code>port</code>.
     * 
     * @param port the port
     * @return the fake sending server
     * @throws IOException in case that the server cannot be created
     */
    public static FakeServer createFakeSendingServer(int port) throws IOException {
        FakeServer sendingServer = new FakeServer(port, new IHandlerCreator() {

            @Override
            public FakeHandler createHandler(Socket sock) {
                return new ReceivingHandler(sock);
            }
            
        });
        return sendingServer;
    }

    /**
     * Returns a fake receiving server for a given <code>port</code>.
     * 
     * @param port the port
     * @return the fake receiving server
     * @throws IOException in case that the server cannot be created
     */
    public static FakeServer createFakeReceivingServer(int port) throws IOException {
        FakeServer receivingServer = new FakeServer(port, new IHandlerCreator() {

            @Override
            public FakeHandler createHandler(Socket sock) {
                return new SendingHandler(sock);
            }
            
        });
        return receivingServer;
    }
    
    /**
     * Tests the hardware control connection.
     * 
     * @throws IOException in case of I/O problems
     */
    @Test(timeout = 5000)
    public void testHardwareConnection() throws IOException {
        FakeServer sendingServer = createFakeSendingServer(9998);
        sendingServer.start();
        FakeServer receivingServer = createFakeReceivingServer(9999);
        receivingServer.start();
        System.out.println("Creating connection control");
        HardwareControlConnection hcc = new HardwareControlConnection("localhost", 9998, 9999);
        sleep(500);
        System.out.println("Control connection created");
        
        System.out.println("Stopping HY");
        String msg = hcc.stopAlgorithm("HY");
        Assert.assertEquals(Code.STOP_ERROR, MessageTable.getCode(msg)); // not started
        
        System.out.println("Querying HY");
        Assert.assertFalse(hcc.isRunning("HY"));

        System.out.println("Uploading HY");
        ByteString y = ByteString.copyFromUtf8("hello");
        UploadMessageOut up = hcc.uploadAlgorithm("HY", y);
        Assert.assertTrue(Utils.isSuccess(up.getErrorMsg()));
        Assert.assertTrue(up.getPortIn() > 0);
        Assert.assertEquals(1, up.getPortOutCount()); // we implicitly just requested one port
        Assert.assertTrue(up.getPortOut(0) > 0);
        
        System.out.println("Querying HY");
        Assert.assertTrue(hcc.isRunning("HY"));

        System.out.println("Uploading HY again");
        up = hcc.uploadAlgorithm("HY", y);
        Assert.assertNotNull(up);
        Assert.assertNotNull(up.getErrorMsg()); // already there
        Assert.assertEquals(Code.UPLOAD_ERROR, MessageTable.getCode(up.getErrorMsg()));

        System.out.println("Querying HY");
        Assert.assertTrue(hcc.isRunning("HY"));

        System.out.println("Stopping HY");
        msg = hcc.stopAlgorithm("HY");
        Assert.assertNotNull(msg);
        Assert.assertEquals(Code.SUCCESS, MessageTable.getCode(msg)); // stopped

        System.out.println("Querying HY");
        Assert.assertFalse(hcc.isRunning("HY"));

        System.out.println("Stopping server");
        Assert.assertTrue(hcc.stopServer());
        
        System.out.println("Closing instances");
        hcc.close();
        sendingServer.stop();
        receivingServer.stop();
    }

    /**
     * Sleeps for a given time.
     * 
     * @param time the time to sleep
     */
    private static void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Tests the messages.
     */
    @Test
    public void testMessages() {
        Assert.assertEquals(0, Code.SUCCESS.getCode());
        Assert.assertEquals(-1, Code.UPLOAD_ERROR.getCode());
        Assert.assertEquals(-2, Code.STOP_ERROR.getCode());
        
        for (Code c : Code.values()) {
            Assert.assertEquals(c.getMessage(), MessageTable.getMessage(c.getCode()));
            Assert.assertEquals(c.getMessage(), MessageTable.getMessage(c.toMsg()));
            Assert.assertEquals(c, MessageTable.getCode(c.toMsg()));
        }

        Assert.assertNull(MessageTable.getCode(null));
        Assert.assertNull(MessageTable.getMessage(null));
        Assert.assertNull(MessageTable.getMessage(""));
        Assert.assertNull(MessageTable.getMessage(Integer.MAX_VALUE));
    }

}
