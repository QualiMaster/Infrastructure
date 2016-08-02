package eu.qualimaster.common.switching;

import java.io.IOException;
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
    /**
     * Stops the handler thread.
     * @throws IOException the IO exception
     */
    public void stop() throws IOException;
    /**
     * Returns true if the handler is stopped, otherwise false.
     * @return true if the handler is stopped, otherwise false.
     */
    public boolean isStopped();
}
