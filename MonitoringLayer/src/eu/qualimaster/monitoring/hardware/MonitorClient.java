package eu.qualimaster.monitoring.hardware;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * Requests and receives monitoring data from a Maxeler server.
 * 
 * @author malakonakis
 * @author ap0n (created on 12/12/14)
 * @author Holger Eichelberger
 */
class MonitorClient {

    private String ip = null;
    private int port;
    private Socket sock = null;
    private PrintWriter out;
    private BufferedReader in;

    /**
     * Creates a monitor client.
     * 
     * @param ip the host IP of the Maxeler server
     * @param port the port to read from
     * @throws IOException in case that creating caused a network error
     */
    MonitorClient(String ip, int port) throws IOException {
        this.ip = ip;
        this.port = port;
        connect();
    }

    /**
     * Connects to the server.
     * 
     * @throws IOException in case that creating caused a network error
     */
    private void connect() throws IOException {
        try {
            sock = new Socket();
            sock.connect(new InetSocketAddress(ip, port), 500); // use timeout for DMZs (e.g., Jenkins)
            sock.setSoTimeout(500);
            out = new PrintWriter(sock.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        } catch (SecurityException | IllegalArgumentException | SocketTimeoutException e) {
            if (!e.getMessage().equals("connect timed out")) {
                throw new IOException(e.getMessage(), e);
            }
            close();
        } catch (NoRouteToHostException e) {
            close();
        } catch (SocketException e) {
            if (e.getMessage().equals("Network is unreachable: connect")) {
                close();
            } else {
                throw e;
            }
        }
    }

    /**
     * Sends the monitor data request.
     * 
     * @throws IOException in case that sending fails
     */
    void sendDFEMonitorRequest() throws IOException {
        if (null != out) {
            out.println("r");
        }
    }

    /**
     * Receives data from the Maxeler server.
     * 
     * @return the data received
     * @throws IOException in case that creating caused a network error
     */
    String receiveData() throws IOException {
        return null == in ? "" : in.readLine();
    }

    /**
     * Closes this instance.
     * 
     * @throws IOException in case that closing fails
     */
    void close() throws IOException {
        if (null != out && null != in) { // both > connected
            out.println("c");
        }
        IOException ex = null;
        try {
            if (null != in) {
                in.close();
            }
        } catch (IOException e) {
            ex = e;
        }
        if (null != out) {
            out.close();
        }
        try {
            if (null != sock) {
                sock.close();
            }
        } catch (IOException e) {
            ex = e;
        }
        if (null != ex) {
            throw ex;
        }
    }
}
