/*
 * Copyright 2009-2014 University of Hildesheim, Software Systems Engineering
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
 * An internal event bus event to notify the event server about
 * a client that needs to handle events locally.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
class ForwardHandlerEvent extends AbstractEvent {
    
    private static final long serialVersionUID = -6353697868091603805L;
    private String clientId;
    private String eventClass;

    /**
     * Creates a forward event.
     * 
     * @param clientId the client VM id to send information to
     * @param eventClass the event class to handle
     */
    public ForwardHandlerEvent(String clientId, String eventClass) {
        this.clientId = clientId;
        this.eventClass = eventClass;
    }
    
    /**
     * Returns the client VM identification.
     * 
     * @return the client VM identification
     */
    public String getClientId() {
        return clientId;
    }
    
    /**
     * Returns the event class to handle.
     * 
     * @return the event class
     */
    public String getEventClass() {
        return eventClass;
    }

}
