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
 * Identifies an (extensible) load shedding parameter.
 * 
 * @author Holger Eichelberger
 */
@QMSupport
public interface ILoadSheddingParameter extends Serializable {

    /**
     * The name of the parameter.
     * 
     * @return the name
     */
    public String name();

}
