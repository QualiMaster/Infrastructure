/*
 * Copyright 2009-2016 University of Hildesheim, Software Systems Engineering
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
package eu.qualimaster.common.signal;

import eu.qualimaster.events.IEvent;

/**
 * A dummy event for forcing startup of the event manager.
 * 
 * @author Holger Eichelberger
 */
public class ConnectEvent implements IEvent {

    private static final long serialVersionUID = -7128804008518291862L;
    private String id;

    /**
     * Creates a connect event with an id to be printed out.
     * 
     * @param id the id
     */
    public ConnectEvent(String id) {
        this.id = id;
    }
    
    @Override
    public String getChannel() {
        return null;
    }

    @Override
    public String toString() {
        return "ConnectEvent " + id;
    }

}
