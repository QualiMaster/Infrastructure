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
package eu.qualimaster.adaptation.events;

import eu.qualimaster.adaptation.external.RequestMessage;
import eu.qualimaster.common.QMInternal;

/**
 * Links a request message and the related adaptation event into one adaptation event
 * to be passed to the adaptation event queue in order to inform the requested by
 * a response about the result of the adaptation enactment.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public class WrappingRequestMessageAdaptationEvent extends AdaptationEvent {
    
    private static final long serialVersionUID = 3130230115697807697L;
    private RequestMessage message;
    private AdaptationEvent event;
    
    /**
     * Creates a wrapping / linking event.
     *  
     * @param message the request message
     * @param event the adaptation event to cause
     */
    public WrappingRequestMessageAdaptationEvent(RequestMessage message, AdaptationEvent event) {
        this.message = message;
        this.event = event;
    }
    
    /**
     * Returns the request message.
     * 
     * @return the request message
     */
    public RequestMessage getMessage() {
        return message;
    }
    
    /**
     * Returns the adaptation event.
     * 
     * @return the adaptation event
     */
    public AdaptationEvent getAdaptationEvent() {
        return event;
    }
    
    /**
     * If this is a wrapping event, return the wrapped event.
     * 
     * @return the wrapped event (<b>this</b>)
     */
    @QMInternal
    public AdaptationEvent unpack() {
        return event;
    }

}
