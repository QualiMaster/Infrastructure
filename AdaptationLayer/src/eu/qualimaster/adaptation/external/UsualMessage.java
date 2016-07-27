package eu.qualimaster.adaptation.external;

/**
 * An usual message does not cause a disconnect.
 * 
 * @author Holger Eichelberger
 */
public abstract class UsualMessage implements Message {

    private static final long serialVersionUID = -2589429498017653860L;

    @Override
    public final boolean isDisconnect() {
        return false;
    }

    @Override
    public boolean requiresAuthentication() {
        return false; // no authentication required - just restrict in subclasses
    }

    @Override
    public boolean passToUnauthenticatedClient() {
        return true; // pass always
    }
    
    @Override
    public Message elevate() {
        return this; // cannot elevate as unknown how
    }

}
