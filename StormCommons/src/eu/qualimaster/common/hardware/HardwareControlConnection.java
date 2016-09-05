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
package eu.qualimaster.common.hardware;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.google.protobuf.ByteString;

import eu.qualimaster.dataManagement.serialization.SerializerRegistry;

/**
 * Realizes the control communication with hardware.
 * 
 * @author Gregory Chrysos
 * @author Pavlos Malakonakis
 * @author Evripides Sotiriadis
 * @author Holger Eichelberger
 */
public class HardwareControlConnection {

    private static final InternalDispatcher DISPATCHER = new InternalDispatcher();
    private Transmitter transmitter;
    private Receiver receiver;
    
    /**
     * Register the default serializers.
     */
    static {
        SerializerRegistry.register(UploadMessageOut.class, new UploadMessageOutSerializer());
        SerializerRegistry.register(UploadMessageIn.class, new UploadMessageInSerializer());
        SerializerRegistry.register(StopMessageIn.class, new StopMessageInSerializer());
        SerializerRegistry.register(StopMessageOut.class, new StopMessageOutSerializer());
        SerializerRegistry.register(IsRunningAlgorithmIn.class, new IsRunningAlgorithmInSerializer());
        SerializerRegistry.register(IsRunningAlgorithmOut.class, new IsRunningAlgorithmOutSerializer());
    }
    
    /**
     * Creates a hardware control interface on a given IP address, a sending port and a receiving port.
     * 
     * @param ip the IP address
     * @param sendingPort the sending port
     * @param receivingPort the receiving port
     * @throws IOException if the control interface cannot be created for some reason, trying to close open connections 
     */
    public HardwareControlConnection(String ip, int sendingPort, int receivingPort) throws IOException {
        try {
            transmitter = new Transmitter(ip, sendingPort);
            receiver = new Receiver(ip, receivingPort);
        } catch (IOException e) {
            try {
                close();
            } catch (IOException e1) {
            }
            throw e;
        }
    }
    
    /**
     * Closes the control interface.
     * 
     * @throws IOException in case that closing fails for some reason
     */
    public void close() throws IOException {
        IOException ex = null;
        try {
            if (null != transmitter) {
                transmitter.disconnect();
                transmitter = null;
            }
        } catch (IOException e) {
            ex = e;
        }
        try {
            if (null != receiver) {
                receiver.disconnect();
                receiver = null;
            }
        } catch (IOException e) {
            ex = e;
        }
        if (null != ex) {
            throw ex;
        }
    }
    
    // helpers
    
    /**
     * Sends a lead in token.
     * 
     * @param token the token
     * @return <code>true</code> if sent, <code>false</code> else
     * @throws IOException if sending the token fails
     */
    private boolean sendToken(String token) throws IOException {
        boolean done;
        if (null != transmitter) {
            transmitter.send(token.getBytes());
            done = true;
        } else {
            done = false;
        }
        return done;
    }
    
    /**
     * Sends a message by serializing it.
     * 
     * @param <M> the message type
     * @param msg the message (may be <b>null</b>, then no message is sent)
     * @param cls the message class
     * @return <code>true</code> if sent, <code>false</code> else
     * @throws IOException in case that serializing fails
     */
    private <M> boolean sendMessage(M msg, Class<M> cls) throws IOException {
        boolean done;
        if (null != transmitter) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (null != msg) {
                SerializerRegistry.getSerializer(cls).serializeTo(msg, out);
            }
            out.write(0);
            transmitter.send(out.toByteArray());
            done = true;
        } else {
            done = false;
        }
        return done;
    }
    
    /**
     * Receives the different kinds of messages and reacts on them by calling <code>receiver</code>.
     * 
     * @param token the message token
     * @param in the stream containing the message
     * @param dispatcher the dispatcher to call (may be <b>null</b>)
     * @throws IOException if there is a problem
     */
    private void receive(String token, ByteArrayInputStream in, IHardwareDispatcher dispatcher) throws IOException {
        if (token.equalsIgnoreCase("ra") && in != null) {
            UploadMessageOut tmp = SerializerRegistry.getSerializer(UploadMessageOut.class).deserializeFrom(in);
            if (null != tmp && null != dispatcher) {
                dispatcher.received(tmp);
            }
        } else if (token.equalsIgnoreCase("rb")) {
            StopMessageOut tmp = SerializerRegistry.getSerializer(StopMessageOut.class).deserializeFrom(in);
            if (null != tmp && null != dispatcher) {
                dispatcher.received(tmp);
            }
        } else if (token.equalsIgnoreCase("rc")) {
            IsRunningAlgorithmOut tmp = SerializerRegistry.getSerializer(IsRunningAlgorithmOut.class)
                .deserializeFrom(in);
            if (null != tmp && null != dispatcher) {
                dispatcher.received(tmp);
            }
        } else if (token.equalsIgnoreCase("rd")) {
            close(); // transmitter is already closed
            if (null != dispatcher) {
                dispatcher.serverTerminated();
            }
        }
    }
    
    // asynchronous

    /**
     * Sends a non-blocking algorithm upload request to the hardware machine addressed by this connection, shall be 
     * {@link IHardwareDispatcher#uploaded(int, int)} or {@link IHardwareDispatcher#failed()}. This method requests 1 
     * output port.
     * 
     * @param id the algorithm id
     * @param executable the executable (preliminary)
     * @throws IOException in case that sending the command fails for some reason
     */
    public void sendAlgorithmUpload(String id, ByteString executable) throws IOException {
        sendAlgorithmUpload(id, 1, executable);
    }
    
    /**
     * Sends a non-blocking algorithm upload request to the hardware machine addressed by this connection, shall be 
     * {@link IHardwareDispatcher#uploaded(int, int)} or {@link IHardwareDispatcher#failed()}.
     * 
     * @param id the algorithm id
     * @param portCount the number of ports to use (numbers less than 1 will be turned to 1)
     * @param executable the executable (preliminary)
     * @throws IOException in case that sending the command fails for some reason
     */
    public void sendAlgorithmUpload(String id, int portCount, ByteString executable) throws IOException {
        sendToken("ca");
        
        UploadMessageIn message = new UploadMessageIn();
        message.setId(id);
        message.setExecutable(executable);
        message.setPortCount(portCount);

        sendMessage(message, UploadMessageIn.class);        
    }
    
    /**
     * Sends a non-blocking stop request for a given algorithm. A response comes in via 
     * {@link #receive(IHardwareDispatcher)}, shall be {@link IHardwareDispatcher#stopped()} or 
     * {@link IHardwareDispatcher#failed()}.
     * 
     * @param id the id of the algorithm to stop
     * @throws IOException in case that sending the command fails for some reason
     */
    public void sendStopAlgorithm(String id) throws IOException {
        sendToken("cb");

        StopMessageIn message = new StopMessageIn();
        message.setId(id);

        sendMessage(message, StopMessageIn.class);
    }
    
    /**
     * Sends a non-blocking request whether the given algorithm is running. A response comes in via 
     * {@link #receive(IHardwareDispatcher)}, shall be {@link IHardwareDispatcher#closed()}.
     * 
     * @param id the id of the algorithm to query for
     * @throws IOException in case that sending the query command fails for some reason
     */
    public void sendIsRunning(String id) throws IOException {
        sendToken("cc");
        
        IsRunningAlgorithmIn message = new IsRunningAlgorithmIn();
        message.setId(id);

        sendMessage(message, IsRunningAlgorithmIn.class);
    }
    
    /**
     * Stops the HW server for shutting down the infrastructure. Handle with care. A response comes in via 
     * {@link IHardwareDispatcher#serverTerminated()}.
     * 
     * @throws IOException in case that sending the command fails
     */
    public void sendStopServer() throws IOException {
        sendToken("cd");
        sendMessage(null, Object.class);
        
        transmitter.disconnect();
        transmitter = null;
    }
    
    /**
     * Receives the different kinds of messages and reacts on them by calling <code>receiver</code>. Blocking call.
     * 
     * @param dispatcher the dispatcher to call (may be <b>null</b>)
     * @param block whether this method shall block until there is an answer
     * @throws IOException if there is a problem
     */
    public void receive(IHardwareDispatcher dispatcher, boolean block) throws IOException {
        if (null != receiver) {
            byte[] msg;
            do {
                msg = receiver.receiveData();
                if (null != msg) {
                    ByteArrayInputStream in = new ByteArrayInputStream(msg);
                    in.skip(2);
                    receive(new String(msg, 0, 2), in, dispatcher);
                } else {
                    if (block) {
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            } while (block && null == msg);
        }
    }

    // synchronous

    /**
     * Uploads an algorithm to the hardware machine addressed by this interface connection. This method requests 
     * 1 output port (default).
     * 
     * @param id the algorithm id
     * @param executable the Maven URL of the executable 
     * @return an instance indicating the result of this operation. If successful, in and out ports are returned.
     * @throws IOException in case that sending the command fails for some reason
     */
    public UploadMessageOut uploadAlgorithm(String id, ByteString executable) throws IOException {
        return uploadAlgorithm(id, 1, executable);
    }
    
    /**
     * Uploads an algorithm to the hardware machine addressed by this interface connection.
     * 
     * @param id the algorithm id
     * @param executable the Maven URL of the executable
     * @param portCount the number of ports to use (numbers less than 1 will be turned to 1)
     * @return an instance indicating the result of this operation. If successful, in and out ports are returned.
     * @throws IOException in case that sending the command fails for some reason
     */
    public UploadMessageOut uploadAlgorithm(String id, int portCount, ByteString executable) throws IOException {
        sendAlgorithmUpload(id, portCount, executable);
        receive(DISPATCHER, true);
        UploadMessageOut result = DISPATCHER.uploadMessage;
        DISPATCHER.clear();
        return result;
    }
    
    /**
     * Stops an algorithm from running.
     * 
     * @param id the id of the algorithm to stop
     * @return <b>null</b> in case of success, the failure message else
     * @throws IOException in case that sending the command fails for some reason
     */
    public String stopAlgorithm(String id) throws IOException {
        sendStopAlgorithm(id);
        receive(DISPATCHER, true);
        String message = DISPATCHER.stopMessage.getErrorMsg();
        DISPATCHER.clear();
        return message;
    }
    
    /**
     * Queries whether the given algorithm is running.
     * 
     * @param id the id of the algorithm to query for
     * @return <code>true</code> if the algorithm is running, <code>false</code> else
     * @throws IOException in case that sending the query command fails for some reason
     */
    public boolean isRunning(String id) throws IOException {
        sendIsRunning(id);
        receive(DISPATCHER, true);
        boolean result = DISPATCHER.isRunningMessage.getIsRunning();
        DISPATCHER.clear();
        return result;
    }

    /**
     * Stops the HW server for shutting down the infrastructure. Handle with care. A response comes in via 
     * {@link IHardwareDispatcher#serverTerminated()}.
     * 
     * @return <code>true</code> if stopped, <code>false</code> else
     * @throws IOException in case that sending the command fails
     */
    public boolean stopServer() throws IOException {
        sendStopServer();
        receive(DISPATCHER, true);
        boolean done = DISPATCHER.terminated;
        DISPATCHER.clear();
        return done;
    }
    
    // dispatchers

    /**
     * An internal dispatcher. Call {@link #clear()} for reuse.
     * 
     * @author Holger Eichelberger
     */
    private static class InternalDispatcher implements IHardwareDispatcher {
        
        private UploadMessageOut uploadMessage;
        private IsRunningAlgorithmOut isRunningMessage;
        private StopMessageOut stopMessage;
        private boolean terminated;
        
        @Override
        public void received(UploadMessageOut msg) {
            this.uploadMessage = msg;
        }

        @Override
        public void received(IsRunningAlgorithmOut msg) {
            this.isRunningMessage = msg;
        }

        @Override
        public void received(StopMessageOut msg) {
            this.stopMessage = msg;
        }

        @Override
        public void serverTerminated() {
            this.terminated = true;
        }

        /**
         * Clears this instance for reuse.
         */
        public void clear() {
            terminated = false;
            uploadMessage = null;
            isRunningMessage = null;
            stopMessage = null;
        }
        
    };

}
