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

import java.io.Serializable;

import eu.qualimaster.common.QMSupport;

/**
 * Describes a load schedder.
 * 
 * @author Holger Eichelberger
 */
@QMSupport
public interface ILoadShedderDescriptor extends Serializable {

    /**
     * Returns a (unique) load shedder identifier.
     * 
     * @return the identifier (unique per infrastructure)
     */
    public String getIdentifier();
    
    /**
     * An optional short name that must not be unique. Here the last registered shedder counts.
     * 
     * @return the short name or <b>null</b>.
     */
    public String getShortName();
    
}
