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
package eu.qualimaster.dataManagement.common;

import eu.qualimaster.dataManagement.events.ReferenceDataManagementEvent;
import eu.qualimaster.dataManagement.events.ReferenceDataManagementEvent.Command;
import eu.qualimaster.events.EventManager;

/**
 * Represents a remote reference. Remote references encapsulate real data elements residing
 * on client side, e.g., distributed among Storm workers and are used on central (nimbus) side.
 * 
 * @param <E> the type of the data element
 * @author Holger Eichelberger
 */
public class RemoteReference <E extends IDataElement> implements IReference<E> {

    private String managerId;
    private String unit;
    private String elementId;
    
    /**
     * Creates a remote reference for a certain data element. Please note that 
     * <code>dataElement</code> shall not be stored within this instance. 
     * Communication shall happen remotely.
     * 
     * @param event the causing event to create the reference for
     */
    public RemoteReference(ReferenceDataManagementEvent event) {
        this.managerId = event.getManagerId();
        this.unit = event.getUnit();
        this.elementId = event.getElementId();
    }
    
    @Override
    public void connect() {
        EventManager.send(new ReferenceDataManagementEvent(managerId, unit, elementId, Command.CONNECT));
    }

    @Override
    public void disconnect() {
        EventManager.send(new ReferenceDataManagementEvent(managerId, unit, elementId, Command.DISCONNECT));
    }

    @Override
    public void dispose() {
        EventManager.send(new ReferenceDataManagementEvent(managerId, unit, elementId, Command.DISPOSE));
    }

    @Override
    public String getId() {
        return elementId;
    }
    
}
