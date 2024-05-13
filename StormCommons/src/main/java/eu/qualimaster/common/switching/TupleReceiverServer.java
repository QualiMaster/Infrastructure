package eu.qualimaster.common.switching;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.log4j.Logger;

import eu.qualimaster.common.switching.tupleReceiving.ITupleReceiverHandler;
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
    private boolean useSwitchHandler;
    
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
        this(creator, port, false);
    }

    /**
     * Creates a socket server for receiving tuples.
     * @param creator the tuple receive creator
     * @param port the port to create the socket server
     * @param useSwitchHandler use the switch handler or the general handler
     */
    public TupleReceiverServer(ITupleReceiveCreator creator, int port, boolean useSwitchHandler) {
        this.creator = creator;
        this.port = port;
        this.useSwitchHandler = useSwitchHandler;
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
                if (null != creator) {
                    handler = creator.create(useSwitchHandler);
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
