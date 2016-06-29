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
package eu.qualimaster.dataManagement.events;

import eu.qualimaster.common.QMInternal;

/**
 * Handles remote reference commands.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public class ReferenceDataManagementEvent extends DataManagementEvent {
    
    private static final long serialVersionUID = -3248856467223904987L;
    private String managerId;
    private String unit;
    private String elementId;
    private Command command;
    
    /**
     * Defines the type of commands.
     * 
     * @author Holger Eichelberger
     */
    public enum Command {
        
        /**
         * Registers an unknown data element.
         */
        REGISTER,
        
        /**
         * Connects the data element.
         */
        CONNECT,
        
        /**
         * Disconnects the data element.
         */
        DISCONNECT,
        
        /**
         * Disposes the data element.
         */
        DISPOSE
    }
    
    /**
     * Creates a reference data management event, actually a command over a data managment reference.
     * 
     * @param managerId the identification of the manager
     * @param unit the data unit (identification)
     * @param elementId the data element identification
     * @param command the command to be executed
     */
    public ReferenceDataManagementEvent(String managerId, String unit, String elementId, Command command) {
        this.managerId = managerId;
        this.unit = unit;
        this.elementId = elementId;
        this.command = command;
    }

    /**
     * Returns the manager id representing the data manager.
     * 
     * @return the manager id
     */
    public String getManagerId() {
        return managerId;
    }

    /**
     * Returns the data unit holding data elements.
     * 
     * @return the data unit
     */
    public String getUnit() {
        return unit;
    }

    /**
     * Returns the data element id.
     * 
     * @return the data element id
     */
    public String getElementId() {
        return elementId;
    }

    /**
     * Returns the command represented by this event.
     * 
     * @return the command
     */
    public Command getCommand() {
        return command;
    }

}
