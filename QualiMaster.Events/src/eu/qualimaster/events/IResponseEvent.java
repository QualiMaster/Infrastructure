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
 * Defines the interface of a message that is an answer. This is a separate interface
 * to avoid sending unnecessary data.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public interface IResponseEvent extends IEvent {

    /**
     * Returns the receiver JVM id.
     * 
     * @return the id 
     */
    @QMInternal
    public String getReceiverId();
    
    /**
     * Returns the id of this message.
     * 
     * @return the id 
     */
    @QMInternal
    public String getMessageId();

}
