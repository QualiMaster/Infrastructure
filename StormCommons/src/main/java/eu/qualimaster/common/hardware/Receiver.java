package eu.qualimaster.common.hardware;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

/**
 * Network receiver for hardware.
 * 
 * @author ap0n
 * @author Gregory Chrysos
 * @author Pavlos Malakonakis
 * @author Evripides Sotiriadis
 */
class Receiver {

    private String ip;
    private int port;
    private Socket sock = null;

    /**
     * Connects to the hardware on a given IP address and port.
     * 
     * @param ip the IP address
     * @param port the port
     * @throws IOException in case that creating caused a network error
     */
    public Receiver(String ip, int port) throws IOException {
        this.ip = ip;
        this.port = port;
        connect();
    }
    
    /**
     * Connects this receiver.
     * 
     * @throws IOException if connecting fails
     */
    private void connect() throws IOException {
        sock = new Socket(ip, port);
    }

    /**
     * Disconnects this receiver.
     * 
     * @throws IOException if disconnecting fails
     */
    void disconnect() throws IOException {
        sock.close();
    }

    /**
     * Receives plain data blocks.
     * 
     * @return the data block
     * @throws IOException if receiving the block fails
     */
    public byte [] receiveData() throws IOException {
        byte[] msg = new byte[1024];
        byte[] temp = new byte[1];

        int counter = 0;
        InputStream in = sock.getInputStream();
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

}