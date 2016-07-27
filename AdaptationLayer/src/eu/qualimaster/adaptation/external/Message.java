package eu.qualimaster.adaptation.external;

import java.io.Serializable;

/**
 * Represents a message to be sent. Basically, "messages" are sent from server side
 * to client side to inform about changes, "requests" come from client side to request changes
 * on server side. For testing, messages shall implement
 * <code>equals</code> and <code>hashCode</code>.
 * 
 * @author Holger Eichelberger
 */
public interface Message extends Serializable {

    /**
     * Dispatches this message. The call is asynchronous.
     * 
     * @param dispatcher the message dispatcher
     */
    public void dispatch(IDispatcher dispatcher);

    /**
     * Does this message cause a disconnect of the connection.
     * 
     * @return <code>true</code> if it causes a disconnect, <code>false</code> else
     */
    public boolean isDisconnect();
    
    /**
     * Whether processing this message requires an authenticated client.
     * 
     * @return <code>true</code> if it requires an authenticated client connection, <code>false</code> else
     */
    public boolean requiresAuthentication();
    
    /**
     * Pass this message to unauthenticated clients. There shall be no restriction for authenticated clients.
     * 
     * @return <code>true</code> for passing, <code>false</code> else
     */
    public boolean passToUnauthenticatedClient();
    
    /**
     * Elevates this message to a privileged message.
     * 
     * @return the privileged message (may be <b>this</b> if already privileged or it cannot be elevated)
     */
    public Message elevate();
    
    /**
     * Turns this message into an information message.
     * 
     * @return the information message, may be <b>null</b> if no such message can/shall be created
     */
    public Message toInformation();
    
}
