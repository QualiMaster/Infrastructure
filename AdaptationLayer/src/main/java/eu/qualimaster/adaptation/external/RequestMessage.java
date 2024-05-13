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
 * A message that represents a request leading to a {@link ResponseMessage}.
 * 
 * @author Holger Eichelberger
 */
public abstract class RequestMessage extends UsualMessage {

    private static final long serialVersionUID = -3815714453647839001L;
    private String clientId;
    private String messageId;

    /**
     * Defines the client identifier. (public for testing)
     * 
     * @param clientId the client identifier
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    /**
     * Defines the message identifier. (public for testing)
     * 
     * @param messageId the message identifier
     */
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    /**
     * Returns the client identifier.
     * 
     * @return the client identifier
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Returns the message identifier.
     * 
     * @return the message identifier
     */
    public String getMessageId() {
        return messageId;
    }

    @Override
    public int hashCode() {
        return Utils.hashCode(getClientId()) + Utils.hashCode(getMessageId());
    }
    
    @Override
    public boolean equals(Object obj) {
        boolean equals = false;
        if (obj instanceof RequestMessage) {
            RequestMessage msg = (RequestMessage) obj;
            equals = Utils.equals(getMessageId(), msg.getMessageId());
            equals &= Utils.equals(getClientId(), msg.getClientId());
        }
        return equals;
    }

}
