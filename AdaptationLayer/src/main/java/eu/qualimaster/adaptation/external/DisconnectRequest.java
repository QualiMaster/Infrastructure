package eu.qualimaster.adaptation.external;

/**
 * A disconnect message.
 * 
 * @author Holger Eichelberger
 */
public class DisconnectRequest implements Message {

    private static final long serialVersionUID = -8103231018514205453L;

    @Override
    public void dispatch(IDispatcher dispatcher) {
        dispatcher.handleDisconnectRequest(this);
    }

    @Override
    public boolean isDisconnect() {
        return true;
    }
    
    @Override
    public int hashCode() {
        return super.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        return obj instanceof DisconnectRequest;
    }

    @Override
    public boolean requiresAuthentication() {
        return false;
    }

    @Override
    public boolean passToUnauthenticatedClient() {
        return true;
    }

    @Override
    public Message elevate() {
        return this;
    }

    @Override
    public Message toInformation() {
        return null; // do not dispatch
    }

}
