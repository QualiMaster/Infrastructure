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

import eu.qualimaster.common.QMInternal;

/**
 * Handles additional events in the adaptation event queue.
 * 
 * @param <E> the type of the event
 * @author Holger Eichelberger
 */
@QMInternal
public class HandlerAdaptationEvent<E> extends AdaptationEvent {

    private static final long serialVersionUID = 5687407003247315337L;
    private E event;
    private IHandler<E> handler;
    
    /**
     * Creates a handler event.
     * 
     * @param event the event to handle
     * @param handler the handler
     */
    public HandlerAdaptationEvent(E event, IHandler<E> handler) {
        this.event = event;
        this.handler = handler;
    }
    
    /**
     * Handles this event.
     */
    public void handle() {
        handler.handle(event);
    }
    
}
