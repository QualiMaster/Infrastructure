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
 * Defines the basic interface as a reponse to {@link RequestMessage}.
 * 
 * @author Holger Eichelberger
 */
public abstract class ResponseMessage extends UsualMessage {

    private static final long serialVersionUID = -767934189475556491L;
    private String messageId;
    private String clientId;

    /**
     * Creates a response message.
     * 
     * @param request the actual request message
     */
    public ResponseMessage(RequestMessage request) {
        this.messageId = request.getMessageId();
        this.clientId = request.getClientId();
    }
    
    /**
     * Returns the message identifier of the request message.
     * 
     * @return the message identifier
     */
    public String getMessageId() {
        return messageId;
    }
    
    /**
     * Returns the client id where to send this message to.
     * 
     * @return the client id
     */
    public String getClientId() {
        return clientId;
    }

    @Override
    public int hashCode() {
        return Utils.hashCode(getClientId()) + Utils.hashCode(getMessageId());
    }
    
    @Override
    public boolean equals(Object obj) {
        boolean equals = false;
        if (obj instanceof ResponseMessage) {
            ResponseMessage msg = (ResponseMessage) obj;
            equals = Utils.equals(getClientId(), msg.getClientId());
            equals &= Utils.equals(getMessageId(), msg.getMessageId());
        }
        return equals;
    }

}
