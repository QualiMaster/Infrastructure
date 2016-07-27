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
package eu.qualimaster.adaptation.external;

/**
 * An optional message for authenticating an infrastructure user connection (in contrast to the 
 * external server communication to the clients). Currently, we keep the "passphrase" open
 * regarding it's structure.
 * 
 * @author Holger Eichelberger
 */
public class AuthenticateMessage extends RequestMessage {
    
    private static final long serialVersionUID = 5124607040479671998L;
    private String user;
    private byte[] passphrase;
    
    /**
     * Creates an authentication message.
     * 
     * @param user the user name
     * @param passphrase the passphrase
     */
    public AuthenticateMessage(String user, byte[] passphrase) {
        this.user = user;
        this.passphrase = passphrase;
    }
    
    /**
     * Returns the user name.
     * 
     * @return the user name
     */
    public String getUser() {
        return user;
    }
    
    /**
     * Returns the passphrase.
     * 
     * @return the passphrase
     */
    public byte[] getPassphrase() {
        return passphrase;
    }

    @Override
    public void dispatch(IDispatcher dispatcher) {
        // do not dispatch this, this is directly handled by the server side
    }
    
    @Override
    public boolean passToUnauthenticatedClient() {
        return false; // just to be sure
    }
    
    @Override
    public Message toInformation() {
        return null; // do not dispatch
    }
    
}
