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

import eu.qualimaster.common.QMInternal;

/**
 * Implements a returnable event in an abstract way.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public abstract class AbstractReturnableEvent implements IReturnableEvent {

    private static final long serialVersionUID = -961304734828763166L;
    private String senderId;
    private String messageId;
    
    /**
     * Creates an instance.
     */
    protected AbstractReturnableEvent() {
    }

    /**
     * Creates an instance.
     * 
     * @param senderId the sender id
     * @param messageId the message id
     */
    protected AbstractReturnableEvent(String senderId, String messageId) {
        this.senderId = senderId;
        this.messageId = messageId;
    }
    
    @QMInternal
    @Override
    public String getChannel() {
        return null;
    }

    @QMInternal
    @Override
    public void setSenderId(String id) {
        this.senderId = id;
    }

    @QMInternal
    @Override
    public String getSenderId() {
        return senderId;
    }

    @QMInternal
    @Override
    public void setMessageId(String id) {
        this.messageId = id;
    }

    @QMInternal
    @Override
    public String getMessageId() {
        return messageId;
    }

    @QMInternal
    @Override
    public String toString() {
        return AbstractEvent.toString(this);
    }

}
