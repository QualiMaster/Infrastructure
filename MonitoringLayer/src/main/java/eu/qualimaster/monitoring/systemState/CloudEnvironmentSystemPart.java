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
package eu.qualimaster.monitoring.systemState;

import eu.qualimaster.monitoring.parts.PartType;

/**
 * Defines the system part reflecting a cloud environment.
 *  
 * @author Holger Eichelberger
 */
public class CloudEnvironmentSystemPart extends SystemPart {

    private static final long serialVersionUID = 4806578587151211736L;

    /**
     * Implements a cloud environment system part.
     * 
     * @param name the name of the system part
     */
    CloudEnvironmentSystemPart(String name) {
        super(PartType.CLOUDENV, name);            
    }

    /**
     * Creates a copy of this system part.
     * 
     * @param source the source system part
     * @param state the parent system state for crosslinks
     */
    protected CloudEnvironmentSystemPart(CloudEnvironmentSystemPart source, SystemState state) {
        super(source, state);
    }
}
