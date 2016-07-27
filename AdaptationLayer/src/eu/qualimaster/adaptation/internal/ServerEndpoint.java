package eu.qualimaster.adaptation.internal;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.qualimaster.adaptation.IAuthenticationCallback;
import eu.qualimaster.adaptation.external.AuthenticateMessage;
import eu.qualimaster.adaptation.external.ConnectedMessage;
import eu.qualimaster.adaptation.external.Endpoint;
import eu.qualimaster.adaptation.external.ExecutionResponseMessage;
import eu.qualimaster.adaptation.external.ExecutionResponseMessage.ResultType;
import eu.qualimaster.adaptation.external.IDispatcher;
import eu.qualimaster.adaptation.external.Logging;
import eu.qualimaster.adaptation.external.Message;
import eu.qualimaster.adaptation.external.RequestMessage;
import eu.qualimaster.adaptation.external.ResponseMessage;

/**
 * Implements a server communication endpoint.
 * 
 * @author Holger Eichelberger
 */
public class ServerEndpoint extends Endpoint implements IAuthenticationCallback {

    private IAuthenticationProvider authenticationProvider;
    private ServerSocket serverSocket;
    private Map<String, Client> clients = Collections.synchronizedMap(new HashMap<String, Client>());
    private List<Client> echoClients = Collections.synchronizedList(new ArrayList<Client>());
    
    /**
     * Creates a server endpoint for the given <code>port</code>.
     * 
     * @param dispatcher the message dispatcher
     * @param port the TCP port to listen on
     * @param authenticationProvider defines the authentication provider (if <b>null</b>, all 
     *   {@link AuthenticateMessage authenticate messages} will be rejected and treated as not authenticatable)
     * @throws IOException in case of I/O problems creating the server socket
     */
    public ServerEndpoint(IDispatcher dispatcher, int port, 
        IAuthenticationProvider authenticationProvider) throws IOException {
        super(dispatcher);
        this.authenticationProvider = authenticationProvider;
        setRunning(false);
        serverSocket = new ServerSocket(port);
        serverSocket.setSoTimeout(SO_TIMEOUT); // enable non-blocking accepts
    }
    
    /**
     * Implements a reconnectable server process.
     * 
     * @author Holger Eichelberger
     */
    private class ServerRunnable implements Runnable {

        @Override
        public void run() {
            while (isRunning()) {
                try {
                    Socket s = serverSocket.accept();
                    Thread thread = new Thread(new ClientCreationRunnable(s));
                    thread.start();
                } catch (SocketTimeoutException e) {
                    // this is ok due to non-blocking mode
                } catch (IOException e) {
                    Logging.error(e.getMessage());
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }        
            try {
                serverSocket.close();
            } catch (IOException e) {
                Logging.error(e.getMessage());
            }
        }
    }
    
    /**
     * A runnable for a specific thread creating client endpoints on server side. This is required as
     * client and server immediately start communicating (client id, version number).
     * 
     * @author Holger Eichelberger
     */
    private class ClientCreationRunnable implements Runnable {

        private Socket socket;
        
        /**
         * Creates a client creation runnable.
         * 
         * @param socket the socket to create the client for
         */
        private ClientCreationRunnable(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            try {
                Logging.info("Accepted adaptation layer connection from " + socket.getRemoteSocketAddress());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                out.flush(); // write header
                String version = in.readUTF();
                if (PROTOCOL_VERSION.equals(version)) {
                    String clientId = in.readUTF();
                    WritingWorker writing = new WritingWorker(out, new CloseableSocket(socket, clientId), false);
                    startWorker(writing);
                    clients.put(clientId, new Client(writing));
                    ReadingWorker reading = new ReadingWorker(in, writing);
                    startWorker(reading);
                } else {
                    Logging.error("Client from " + socket.getRemoteSocketAddress() 
                        + " tries to connect with wrong " + "protocol version. Given " + version + ", expected " 
                        + PROTOCOL_VERSION);
                    in.close();
                    out.close();
                    socket.close();
                }
            } catch (IOException e) {
                Logging.error(e.getMessage());
            }
        }
        
    }
    
    /**
     * Performs the authentication.
     * 
     * @param msg the authentication message
     */
    protected void authenticate(AuthenticateMessage msg) {
        boolean done = false;
        String target = msg.getClientId();
        if (null != target) {
            Client client = clients.get(target);
            if (null != client) {
                client.authenticate(msg);
                done = true;
            }
        }
        if (!done) {
            super.authenticate(msg);
        }
    }

    @Override
    protected void connected(ConnectedMessage msg) {
        schedule(new ExecutionResponseMessage(msg, ResultType.SUCCESSFUL, "Connected."));
    }

    @Override
    protected void route(Message msg) {
        boolean routed = false;
        if (msg instanceof ResponseMessage) { // find specific
            ResponseMessage rMsg = (ResponseMessage) msg;
            String target = rMsg.getClientId();
            if (null != target) {
                Client client = clients.get(target);
                if (null != client) {
                    client.schedule(rMsg);
                    routed = true;
                }
            }
            
        }
        if (!routed) { // send to all
            for (Client client : clients.values()) {
                client.schedule(msg);
            }
        }
        if (echoClients.size() > 0) {
            Message info = msg.toInformation();
            if (null != info) {
                for (int e = 0; e < echoClients.size(); e++) {
                    echoClients.get(e).schedule(info);
                }
            }
        }
    }
    
    /**
     * Bundles socket and client id into a closable unit.
     * 
     * @author Holger Eichelberger
     */
    private class CloseableSocket implements Closeable {
        
        private Socket socket;
        private String clientId;

        /**
         * Creates a closable socket.
         * 
         * @param socket the socket
         * @param clientId the client id
         */
        private CloseableSocket(Socket socket, String clientId) {
            this.socket = socket;
            this.clientId = clientId;
        }

        @Override
        public void close() throws IOException {
            socket.close();
            Client client = clients.remove(clientId);
            if (null != client) {
                echoClients.remove(client);
            }
        }
        
    }
    
    /**
     * Implements a client representation.
     * 
     * @author Holger Eichelberger
     */
    private class Client {
        
        private WritingWorker worker;
        private boolean isAuthenticated;

        /**
         * Creates a client representation.
         * 
         * @param worker the client worker
         */
        private Client(WritingWorker worker) {
            this.worker = worker;
        }

        /**
         * Explicitly schedules the given message in the worker.
         * 
         * @param msg the message to be scheduled
         */
        private void schedule(Message msg) {
            boolean schedule;
            if (msg.requiresAuthentication()) {
                schedule = isAuthenticated;
            } else {
                if (!msg.passToUnauthenticatedClient()) {
                    schedule = isAuthenticated;
                } else {
                    schedule = true;
                }
            }
            if (schedule) {
                worker.schedule(msg);
            }
        }
        
        /**
         * Performs the authentication and informs the client.
         * 
         * @param msg the authentication request message
         */
        private void authenticate(AuthenticateMessage msg) {
            ResultType type;
            String description;
            if (null != authenticationProvider) { 
                if (authenticationProvider.authenticate(msg.getUser(), msg.getPassphrase())) {
                    isAuthenticated = true;
                    type = ResultType.SUCCESSFUL;
                    description = "Authenticated " + msg.getUser() + ".";
                    Logging.info("Accepted user " + msg.getUser());
                    echoClients.add(this);
                } else {
                    isAuthenticated = false;
                    type = ResultType.FAILED;
                    description = "Unmatching user/passphrase.";
                    Logging.info("Rejected user " + msg.getUser());
                }
            } else {
                isAuthenticated = false;
                type = ResultType.FAILED;
                description = "Server misconfiguration. No authentication provider defined.";
            }
            schedule(new ExecutionResponseMessage(msg, type, description));
        }
        
    }

    /**
     * Starts the server endpoint.
     */
    public void start() {
        if (!isRunning()) {
            setRunning(true);
            Thread thread = new Thread(new ServerRunnable());
            thread.start();
            Thread router = new Thread(new RoutingWorker());
            router.start();
        }
    }
    
    /**
     * Stops the server endpoint.
     */
    public void stop() {
        setRunning(false);
        super.stop();
    }
    
    @Override
    protected void addMessageInformation(RequestMessage msg) {
        // usually not needed on server-side
    }

    @Override
    public boolean isAuthenticated(RequestMessage message) {
        boolean result = false;
        String id = message.getClientId();
        if (null != id) {
            Client clt = clients.get(id);
            result = null != clt && clt.isAuthenticated;
        }
        return result;
    }

}
