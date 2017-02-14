package eu.qualimaster.adaptation.external;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.rmi.dgc.VMID;

/**
 * Implements a client endpoint.
 * 
 * @author Holger Eichelberger
 */
public class ClientEndpoint extends Endpoint {

    private final String clientId = new VMID().toString() + "-" +  System.nanoTime();
    private String authenticationMsgId;
    private boolean isAuthenticated;
    
    /**
     * Creates the client endpoint.
     * 
     * @param dispatcher the message dispatcher
     * @param address the address to connect to
     * @param port the port to connect to
     * @throws IOException in case that the connection fails
     */
    public ClientEndpoint(IDispatcher dispatcher, InetAddress address, int port) throws IOException {
        super(dispatcher);
        System.out.println("Connecting " + address + ":" + port);
        Socket s = new Socket(address, port);
        s.setKeepAlive(true);
        s.setSoTimeout(SO_TIMEOUT); // enable non-blocking communication, also for properly ending threads

        ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
        out.flush(); // write header
        out.writeUTF(PROTOCOL_VERSION);
        out.writeUTF(clientId);
        WritingWorker writing = new WritingWorker(out, s);
        startWorker(writing);

        ObjectInputStream in = new ObjectInputStream(s.getInputStream());
        ReadingWorker reading = new ReadingWorker(in, writing);
        startWorker(reading);
    }
    
    /**
     * Returns the client id.
     * 
     * @return the client id
     */
    public String getClientId() {
        return clientId;
    }
    
    @Override
    protected void addMessageInformation(RequestMessage msg) {
        msg.setClientId(clientId);
    }
    
    /**
     * Schedules a message for sending. In case of a {@link RequestMessage request message}, the client id and the
     * message id are changed by this method.
     * 
     * @param msg the message to be sent
     */
    public void schedule(Message msg) {
        super.schedule(msg);
        if (msg instanceof AuthenticateMessage) {
            authenticationMsgId = ((AuthenticateMessage) msg).getMessageId();
        } else if (msg.isDisconnect()) {
            clearAuthenticationData();
        }
    }
    
    /**
     * Is called when <code>msg</code> was dispatched.
     * 
     * @param msg the message
     */
    protected void dispatched(Message msg) {
        if (null != authenticationMsgId && msg instanceof ExecutionResponseMessage) {
            ExecutionResponseMessage response = (ExecutionResponseMessage) msg;
            if (authenticationMsgId.equals(response.getMessageId())) {
                isAuthenticated = true;
                authenticationMsgId = null;
            }
        } else if (msg.isDisconnect()) {
            clearAuthenticationData();
        }
    }

    /**
     * Clears internal authentication data.
     */
    private void clearAuthenticationData() {
        authenticationMsgId = null;
        isAuthenticated = false;
    }
    
    /**
     * Returns whether this client is authenticated.
     * 
     * @return <code>true</code> if authenticated, <code>false</code> else
     */
    public boolean isAuthenticated() {
        return isAuthenticated;
    }

}