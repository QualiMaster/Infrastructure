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
package eu.qualimaster.common.shedding;

import eu.qualimaster.common.QMSupport;

/**
 * Identifies default load shedding parameters.
 * 
 * @author Holger Eichelberger
 */
@QMSupport
public enum DefaultLoadSheddingParameter implements ILoadSheddingParameter {

    /**
     * Shed the n-th tuple (Integer, disables if 0 or negative).
     */
    NTH_TUPLE,
    
    /**
     * Shed with the given probability of tuples (Double, disables if 0 or negative). Shedding value range (0;1);
     */
    PROBABILITY,

    /**
     * Shed with the given ratio of tuples (Double, disables if 0 or negative). Shedding value range (0;1);
     */
    RATIO;
    
}
