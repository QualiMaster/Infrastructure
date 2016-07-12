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
package tests.eu.qualimaster.logReader;

import eu.qualimaster.events.IEvent;

/**
 * Defines the interface of an event specific reader / parser. Please do not store
 * any data in attributes!
 * 
 * @param <E> the actual event type
 * @author Holger Eichelberger
 */
public abstract class EventReader <E extends IEvent> {

    private Class<E> cls;
    
    /**
     * Required no-arg constructor.
     * 
     * @param cls the event class
     */
    protected EventReader(Class<E> cls) {
        this.cls = cls;
    }
    
    /**
     * Parses <code>line</code> for event data (just the attributes are still in the line).
     * 
     * @param line the line
     * @param reader the calling reader
     * @return the event or <b>null</b> if <code>line</code> cannot be parsed
     */
    public abstract E parseEvent(String line, LogReader reader);
 
    /**
     * Returns the handled event name.
     * 
     * @return the event name
     */
    public String getEventName() {
        return cls.getSimpleName(); // by convention
    }
    
    /**
     * Returns the event class handled by this reader.
     * 
     * @return the event class
     */
    protected Class<E> getEventClass() {
        return cls;
    }
    
}
