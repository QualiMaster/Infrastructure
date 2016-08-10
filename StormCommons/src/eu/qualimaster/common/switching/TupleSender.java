package eu.qualimaster.common.switching;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import com.esotericsoftware.kryo.io.Output;

/**
 * Sends tuples via network.
 * @author Cui Qin
 *
 */
public class TupleSender {
    private String host;
    private int port;
    private Socket socket;
    private Output output;
    /**
     * Creates a tuple sender.
     * @param host the host to be connected
     * @param port the port to be connected
     */
    public TupleSender(String host, int port) {
        this.host = host;
        this.port = port;
    }
    /**
     * Connects the host server.
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
     * Sends the data bytes.
     * @param bytes the bytes to be sent
     */
    public void send(byte[] bytes) {
        if (connect()) {
            output.writeInt(bytes.length);
            output.writeBytes(bytes);
            output.flush();
        }
    }
    
    /**
     * Sends a flag indicating the {@link ISwitchTuple} data type.
     */
    public void sendSwitchTupleFlag() {
        send(DataFlag.SWITCH_TUPLE_FLAG.getBytes());
    }
    
    /**
     * Sends a flag indicating the {@link IGeneralTuple} data type.
     */
    public void sendGeneralTupleFlag() {
        send(DataFlag.GENERAL_TUPLE_FLAG.getBytes());
    }
    
    /**
     * Sends a flag indicating the temporary queue shall be used.
     */
    public void sendTemporaryQueueFlag() {
        send(DataFlag.TEMPORARY_QUEUE_FLAG.getBytes());
    }
    
    /**
     * Sends a flag indicating the general queue shall be used.
     */
    public void sendGeneralQueueFlag() {
        send(DataFlag.GENERAL_QUEUE_FLAG.getBytes());
    }
    
    /**
     * Checks whether the connection is there.
     * @return true if connected, otherwise false
     */
    public boolean isConnected() {
        return null != socket;
    }
    
    /**
     * Stops the sender.
     */
    public void stop() {
        System.out.println("Stopping the sender...");
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
        System.out.println("Stopped the sender...");
    }
    
}
