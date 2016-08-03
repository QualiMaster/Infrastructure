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
    
}
