package eu.qualimaster.common.switching;

import java.net.Socket;

/**
 * A interface for tuple receiver handler.
 * @author Cui Qin
 *
 */
public interface ITupleReceiverHandler extends Runnable {
    /**
     * Sets the socket to be connected.
     * @param socket the socket to be connected
     */
    public void setSocket(Socket socket);
}
