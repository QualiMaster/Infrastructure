package eu.qualimaster.common.switching;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.log4j.Logger;
/**
 * Creates a socket server for receiving tuples.
 * @author Cui Qin
 *
 */
public class TupleReceiverServer implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(TupleReceiverServer.class);
    private ServerSocket serverSocket;
    private ITupleReceiverHandler handler;
    private ITupleReceiveCreator creator;
    private boolean cont = true;
    private int port;
    /**
     * Creates a socket server for receiving tuples.
     * @param handler the handler for receiving tuples.
     * @param port the port to create the socket server
     */
    @Deprecated
    public TupleReceiverServer(ITupleReceiverHandler handler, int port) {
        this.handler = handler;
        this.port = port;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Creates a socket server for receiving tuples.
     * @param creator the tuple receive creator
     * @param port the port to create the socket server
     */
    public TupleReceiverServer(ITupleReceiveCreator creator, int port) {
        this.creator = creator;
        this.port = port;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Starts the server.
     */
    public void start() {
        new Thread(this).start();
        LOGGER.info("Server thread started with the port: " + port);
    }
    
    @Override
    public void run() {
        while (cont && serverSocket != null) {
            try {
                LOGGER.info("Accepting the socket connection....");
                Socket socket = serverSocket.accept();
                LOGGER.info("Socket connection accepted " + port);
                handler.setSocket(socket);
                new Thread(creator.create()).start();
            } catch (IOException e) {
                e.printStackTrace();
            } 
        }
    }
    
    /**
     * Stops the server.
     * @throws IOException the IO exception
     */
    public void stop() throws IOException {
        LOGGER.info("Stopping server");
        if (cont) {
            cont = false;
        }
        if (!handler.isStopped()) {
            handler.stop();
        }
        if (serverSocket != null) {
            serverSocket.close();
            serverSocket = null;
        }
        LOGGER.info("Stopped server");
    }

}
