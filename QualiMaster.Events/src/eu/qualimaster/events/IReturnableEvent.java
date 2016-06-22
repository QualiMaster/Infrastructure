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
 * An event that can lead to an answer. Initial data will be ignored and setters will be used
 * by event manager to define return path. The values will be set upon {@link EventManager#handle(IEvent)} or
 * {@link EventManager#send(IEvent)} and are available after method completion. This is a separate interface
 * to avoid sending unnecessary data.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public interface IReturnableEvent extends IEvent {

    /**
     * Defines the sender JVM id.
     * 
     * @param id the id
     */
    @QMInternal
    public void setSenderId(String id);
    
    /**
     * Returns the sender JVM id.
     * 
     * @return the id (must be exactly what has been defined by {@link #setVmId(String)})
     */
    @QMInternal
    public String getSenderId();
    
    /**
     * Defines the id if this message.
     * 
     * @param id the message id
     */
    @QMInternal
    public void setMessageId(String id);
    
    /**
     * Returns the id of this message.
     * 
     * @return the id (must be exactly what has been defined by {@link #setMessageId(String)})
     */
    @QMInternal
    public String getMessageId();

}
