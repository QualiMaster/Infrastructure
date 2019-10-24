package eu.qualimaster.common.switching.tupleReceiving;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.log4j.Logger;

/**
 * Creates a socket server for receiving tuples.
 * @author Cui Qin
 *
 */
public class TupleReceiveServer implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(TupleReceiveServer.class);
    private ServerSocket serverSocket;
    private ITupleReceiverHandler handler;
    private ITupleReceiveStrategy tupleReceiveStrategy;
    private boolean cont = true;
    private int port;

    /**
     * Creates a socket server for receiving tuples.
     * @param tupleReceiveStrategy the tuple receive strategy
     * @param port the port to create the socket server
     */
    public TupleReceiveServer(ITupleReceiveStrategy tupleReceiveStrategy, int port) {
        this.tupleReceiveStrategy = tupleReceiveStrategy;
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
        while (cont && serverSocket != null && !serverSocket.isClosed()) {
            try {
                LOGGER.info("Accepting the socket connection....");
                Socket socket = serverSocket.accept();
                LOGGER.info("Socket connection accepted " + port);
                if (null != tupleReceiveStrategy) {
                    handler = tupleReceiveStrategy.createHandler();
                }
                handler.setSocket(socket);
                new Thread(handler).start();
            } catch (IOException e) {
                try {
                    stop();
                } catch (IOException e1) {
                }
            } 
        }
    }
    
    /**
     * Stops the server.
     * @throws IOException the IO exception
     */
    public void stop() throws IOException {
        LOGGER.info("Stopping server on port " + port);
        if (cont) {
            cont = false;
        }
        if (null != handler && !handler.isStopped()) {
            handler.stop();
        }
        if (serverSocket != null) {
            serverSocket.close();
            serverSocket = null;
        }
        LOGGER.info("Stopped server");
    }

}
