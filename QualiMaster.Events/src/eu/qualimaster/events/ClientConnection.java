/*
 * Copyright 2009-2015 University of Hildesheim, Software Systems Engineering
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.qualimaster.events;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

import org.apache.log4j.LogManager;

/**
 * Represents a client connection, in particular a sharable output stream.
 * 
 * @author Holger Eichelberger
 */
class ClientConnection {
    private Socket socket;
    private ObjectOutputStream out;
    private boolean closed;
    private String clientId;

    /**
     * Creates the client connection.
     * 
     * @param clientId the client id
     * @param socket the client socket
     */
    ClientConnection(String clientId, Socket socket) {
        this.socket = socket;
        this.clientId = clientId;
        closed = false;
    }
    
    /**
     * Returns the client id.
     * 
     * @return the client id
     */
    String getClientId() {
        return clientId;
    }
    
    /**
     * Returns the client socket.
     * 
     * @return the socket, may be <b>null</b> if the connection is closed
     */
    Socket getSocket() {
        return socket;
    }

    /**
     * Returns the sharable output stream. Creates the stream upon first use.
     * 
     * @return the output stream, may be <b>null</b> if the connection is closed
     * @throws IOException in case of I/O problems
     */
    ObjectOutputStream getStream() throws IOException {
        if (!closed && null == out) {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
        }
        return out;
    }
    
    /**
     * Closes the connection.
     */
    void close() {
        closed = true;
        if (null != out) {
            try {
                out.close();
            } catch (IOException e) {
                LogManager.getLogger(getClass()).error("While closing client " + socket.getRemoteSocketAddress() 
                    + " : " + e.getMessage(), e);
            }
            out = null;
        }
        // don't close the socket, this is done by the client reader on server side
    }
}