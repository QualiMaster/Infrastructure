package eu.qualimaster.common.hardware;
/**
 * Creates the handler interface.
 * @author Cui Qin
 *
 */
public interface IHardwareHandlerCreator extends Runnable {
    /**
     * Create a handler.
     * @param host the host
     * @param port the port
     * @return the handler
     */
    public Runnable createHandler(String host, int port); 
}
