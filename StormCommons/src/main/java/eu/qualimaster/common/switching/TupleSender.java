package eu.qualimaster.common.switching;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Output;

/**
 * Sends tuples via network.
 * @author Cui Qin
 *
 */
public class TupleSender {
    private static final Logger LOGGER = Logger.getLogger(TupleSender.class);
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
    public boolean connect() {
        Socket s = null;
        if (null == socket) {
            try {
                LOGGER.info("Connecting to the host: " + host + ", the port: " + port);
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
        send(bytes, false);
    }
    
    /**
     * Sends the data bytes.
     * @param bytes the bytes to be sent
     * @param flag send the bytes as flag (negate length)
     */
    private void send(byte[] bytes, boolean flag) {
        if (connect()) {
            try {
                int len = bytes.length;
                if (flag) {
                    len = -len;
                }
                output.writeInt(len);
                output.writeBytes(bytes);
                output.flush();
            } catch (KryoException e) {
                connect();//try to connect again
                //stop();
            }
        }
    }
    
    /**
     * Sends a flag indicating the {@link ISwitchTuple} data type.
     */
    public void sendSwitchTupleFlag() {
        send(DataFlag.SWITCH_TUPLE_FLAG.getBytes(), true);
    }
    
    /**
     * Sends a flag indicating the {@link IGeneralTuple} data type.
     */
    public void sendGeneralTupleFlag() {
        send(DataFlag.GENERAL_TUPLE_FLAG.getBytes(), true);
    }
    
    /**
     * Sends a flag indicating the temporary queue shall be used.
     */
    public void sendTemporaryQueueFlag() {
        send(DataFlag.TEMPORARY_QUEUE_FLAG.getBytes(), true);
    }
    
    /**
     * Sends a flag indicating the general queue shall be used.
     */
    public void sendGeneralQueueFlag() {
        send(DataFlag.GENERAL_QUEUE_FLAG.getBytes(), true);
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
            //output.writeInt(DataFlag.EOD_FLAG);
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
