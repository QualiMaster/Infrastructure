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
 * Provides an abstract implementation of the response event.
 *
 * @param <E> the returnable event type
 * @author Holger Eichelberger
 */
@QMInternal
public abstract class AbstractResponseEvent <E extends IReturnableEvent> implements IResponseEvent {

    private static final long serialVersionUID = 2093929018197678497L;
    private String receiverId;
    private String messageId;

    /**
     * Creates a test request event.
     * 
     * @param returnable the returnable to take sender and message id from
     */
    public AbstractResponseEvent(E returnable) {
        this.receiverId = returnable.getSenderId();
        this.messageId = returnable.getMessageId();
    }

    @QMInternal
    @Override
    public String getChannel() {
        return null;
    }

    @QMInternal
    @Override
    public String getReceiverId() {
        return receiverId;
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
