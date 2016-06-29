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

import java.rmi.dgc.VMID;

import org.apache.log4j.LogManager;

import eu.qualimaster.dataManagement.events.ReferenceDataManagementEvent;
import eu.qualimaster.dataManagement.events.ReferenceDataManagementEvent.Command;
import eu.qualimaster.events.EventManager;

/**
 * Represents a remote reference. Remote references encapsulate real data elements residing
 * on client side, e.g., distributed among Storm workers.
 * 
 * @param <E> the type of the data element
 * @author Holger Eichelberger
 */
public class LocalReference <E extends IDataElement> implements IReference<E> {

    private static final String VMID = new VMID().toString();
    private E dataElement;
    private String id;
    
    /**
     * Creates a remote reference for a certain data element.
     * 
     * @param dataElement the data element
     */
    public LocalReference(E dataElement) {
        this.dataElement = dataElement;
        this.id = VMID + "-" + System.identityHashCode(dataElement);
    }
    
    @Override
    public void connect() {
        LogManager.getLogger(AbstractDataManager.class).info("Connecting '" + dataElement + "'");
        dataElement.connect();
    }
    
    @Override
    public void disconnect() {
        dataElement.disconnect();
        LogManager.getLogger(AbstractDataManager.class).info("Disconnecting '" + dataElement + "'");
    }

    @Override
    public void dispose() {
        // no need by now
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * Registers this reference on server side.
     * 
     * @param manager the holding manager
     * @param unit the containing unit
     */
    void register(AbstractDataManager<E> manager, String unit) {
        EventManager.send(new ReferenceDataManagementEvent(manager.getId(), unit, this.id, Command.REGISTER));
    }
    
}
