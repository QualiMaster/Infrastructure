package tests.eu.qualimaster.common;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import eu.qualimaster.common.hardware.IHardwareHandlerCreator;
import eu.qualimaster.common.hardware.MultiHardwareThreadsExecutor;

/**
 * Test for multiple hardware threads.
 * @author Cui Qin
 *
 */
public class MultiHardwareThreadsExecutorTest {
    private static final Logger LOGGER = Logger.getLogger(MultiHardwareThreadsExecutorTest.class);    
    /**
     * Define a server.
     * @author Cui Qin
     *
     */
    public static class Server implements Runnable {
        private int port;
        /**
         * Creates a server with the given port.
         * @param port the port number
         */
        public Server(int port) {
            this.port = port;
            try {
                @SuppressWarnings({ "unused", "resource" })
                ServerSocket serverSocket = new ServerSocket(port);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void run() {
            //do nothing
        }
        
        /**
         * Starts the server.
         */
        public void start() {
            new Thread(this).start();
            LOGGER.info("Server thread started with the port: " + port);
        }
        
    }
    /**
     * A client thread handler.
     * @author Cui Qin
     *
     */
    public static class ClientThreadHandler implements IHardwareHandlerCreator {
        /**
         * Create the socket.
         * @param host the host
         * @param port the port
         */
        public void createSocket(String host, int port) {
            try {
                @SuppressWarnings({ "unused", "resource" })
                Socket socket = new Socket(host, port);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void run() {
            //do nothing
        }
        @Override
        public Runnable createHandler(String host, int port) {
            createSocket(host, port);
            return this;
        }
        
    }
    /**
     * Performs the test.
     * @param args ignored
     */
    public static void main(String[] args) {
        //Start the servers
        Server server1 = new Server(8000);
        server1.start();
        
        Server server2 = new Server(8001);
        server2.start();
        
        List<Integer> ports = new ArrayList<Integer>();
        ports.add(8000);
        ports.add(8001);
        Map<String, List<Integer>> servers = new HashMap<String, List<Integer>>();
        servers.put("localhost", ports);
        
        MultiHardwareThreadsExecutor executor = new MultiHardwareThreadsExecutor(servers
                , new ClientThreadHandler(), ports.size());
        executor.createMultiThreads();
    }

}
