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

/**
 * An instance containing the information for configuring a load shedder.
 * 
 * @author Holger Eichelberger
 */
public interface ILoadShedderConfigurer {

    /**
     * Returns the value of an int parameter.
     * 
     * @param name the name of the parameter
     * @param dflt the default value to be returned if the parameter is not specified / cannot be read
     * @return the value
     */
    public int getIntParameter(String name, int dflt);

    /**
     * Returns the value of an int parameter.
     * 
     * @param param the parameter identifier
     * @param dflt the default value to be returned if the parameter is not specified / cannot be read
     * @return the value
     */
    public int getIntParameter(ILoadSheddingParameter param, int dflt);

    /**
     * Returns the value of a parameter.
     * 
     * @param param the identifier of the parameter
     * @return the value or <b>null</b> if unknown
     */
    public Serializable getParameter(ILoadSheddingParameter param);

    /**
     * Returns the value of a parameter.
     * 
     * @param name the name of the parameter
     * @return the value or <b>null</b> if unknown
     */
    public Serializable getParameter(String name);

}
