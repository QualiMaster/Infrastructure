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
 * A cluster system part representing a reconfigurable hardware cluster, actually only MPCC nodes.
 * 
 * @author Holger Eichelberger
 */
public class HwNodeSystemPart extends SystemPart {
    
    // TODO split in HWNode, MPCC node etc
    
    private static final long serialVersionUID = 424799663201155228L;

    /**
     * Creates a cluster system part.
     * 
     * @param name the name of the cluster
     */
    HwNodeSystemPart(String name) {
        super(PartType.CLUSTER, name);
    }
    
    /**
     * Creates a copy of this system part.
     * 
     * @param source the source system part
     * @param state the parent system state for crosslinks
     */
    protected HwNodeSystemPart(HwNodeSystemPart source, SystemState state) {
        super(source, state);
    }

}