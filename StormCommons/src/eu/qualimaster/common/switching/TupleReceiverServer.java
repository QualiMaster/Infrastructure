package eu.qualimaster.common.switching;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
/**
 * Creates a socket server for receiving tuples.
 * @author Cui Qin
 *
 */
public class TupleReceiverServer implements Runnable {
    private ServerSocket serverSocket;
    private ITupleReceiverHandler handler;
    private boolean cont = true;
    /**
     * Creates a socket server for receiving tuples.
     * @param handler the handler for receiving tuples.
     * @param port the port to create the scoket server
     */
    public TupleReceiverServer(ITupleReceiverHandler handler, int port) {
        this.handler = handler;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void run() {
        while (cont) {
            try {
                Socket socket = serverSocket.accept();
                handler.setSocket(socket);
                new Thread(handler).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            
        }
        
    }

}
