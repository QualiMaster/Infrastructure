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
package eu.qualimaster.monitoring.systemState;

import eu.qualimaster.monitoring.parts.PartType;

/**
 * A general-purpose machine system part.
 * 
 * @author Holger Eichelberger
 */
public class MachineSystemPart extends SystemPart {
    
    private static final long serialVersionUID = 4832235274988173311L;

    /**
     * Implements a general-purpose machine system part.
     * 
     * @param name the name of the system part
     */
    MachineSystemPart(String name) {
        super(PartType.MACHINE, name);            
    }

    /**
     * Creates a copy of this system part.
     * 
     * @param source the source system part
     * @param state the parent system state for crosslinks
     */
    protected MachineSystemPart(MachineSystemPart source, SystemState state) {
        super(source, state);
    }

}