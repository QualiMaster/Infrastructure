package eu.qualimaster.adaptation.external;

/**
 * Represents a message indicating that hardware is alive.
 * 
 * @author Holger Eichelberger
 */
public class HardwareAliveMessage extends UsualMessage {

    private static final long serialVersionUID = 7820353899585675613L;
    private String identifier;
    
    /**
     * Creates a hardware alive message with a given identifier.
     * 
     * @param identifier the hardware identifier
     */
    public HardwareAliveMessage(String identifier) {
        this.identifier = identifier == null ? "" : identifier;
    }
    
    @Override
    public void dispatch(IDispatcher dispatcher) {
        dispatcher.handleHardwareAliveMessage(this);
    }
    
    /**
     * Returns the identifier.
     * 
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public int hashCode() {
        return Utils.hashCode(getIdentifier());
    }
    
    @Override
    public boolean equals(Object obj) {
        boolean equals = false;
        if (obj instanceof HardwareAliveMessage) {
            HardwareAliveMessage msg = (HardwareAliveMessage) obj;
            equals = Utils.equals(getIdentifier(), msg.getIdentifier());
        }
        return equals;
    }
    
    @Override
    public boolean passToUnauthenticatedClient() {
        return false; // although not privileged, do not pass to reduce traffic and load
    }

    @Override
    public Message toInformation() {
        return new InformationMessage(null, null, "hardware alive", null);
    }

}
