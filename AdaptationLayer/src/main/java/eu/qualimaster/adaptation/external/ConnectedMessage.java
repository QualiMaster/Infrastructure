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
 * Optional message to check the connection, a kind of ping. If availabe, the server side will respond with 
 * a successful {@link ExecutionResponseMessage}.
 * 
 * @author Holger Eichelberger
 */
public class ConnectedMessage extends RequestMessage {

    private static final long serialVersionUID = -1683077049536442974L;

    /**
     * Creates a connected message.
     */
    public ConnectedMessage() {
    }

    @Override
    public void dispatch(IDispatcher dispatcher) {
        // Handled by server
    }
    
    @Override
    public Message toInformation() {
        return null; // do not dispatch
    }

}
