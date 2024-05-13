package eu.qualimaster.common.switching.tupleReceiving;

import eu.qualimaster.common.switching.IStrategy;

/**
 * An interface for the strategy to receive tuples.
 * @author Cui Qin
 *
 */
public interface ITupleReceiveStrategy extends IStrategy {
    /**
     * Creates a handler to receive tuples.
     * @return a handler to receive tuples
     */
    public ITupleReceiverHandler createHandler();
    
    /**
     * Initialize the tuple receive server.
     * @param port the server port
     */
    public void initTupleReceiveServer(int port);
    
    /**
     * Stop the tuple receive server.
     */
    public void stopTupleReceiveServer();
}
