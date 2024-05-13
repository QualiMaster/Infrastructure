package eu.qualimaster.common.hardware;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Network transmitter to hardware.
 * 
 * @author ap0n
 * @author Gregory Chrysos
 * @author Pavlos Malakonakis
 * @author Evripides Sotiriadis
 */
class Transmitter {
    private String ip;  //vergina
    private int port;
    private Socket sock = null;

    /**
     * Creates a transmitter with given IP/port.
     * 
     * @param ip the ip address
     * @param port the port number
     * @throws IOException in case that creating the transmitter fails
     */
    public Transmitter(String ip, int port) throws IOException {
        this.ip = ip;
        this.port = port;
        connect();
    }

    /**
     * Connects the socket.
     * 
     * @throws IOException in case that creating the socket fails
     */
    private void connect() throws IOException {
        sock = new Socket(ip, port);
    }

    /**
     * Disconnects the socket.
     * 
     * @throws IOException in case that disconnecting fails
     */
    void disconnect() throws IOException {
        sock.close();
    }

    /**
     * Sends a chunk of data.
     * 
     * @param data the data to be sent
     * @throws IOException in case that sending fails
     */
    public void send(byte[] data) throws IOException {
        OutputStream out = sock.getOutputStream();
        out.write(data);
        out.flush();
    }
    
}
