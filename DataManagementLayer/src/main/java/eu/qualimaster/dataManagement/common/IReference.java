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

/**
 * A reference to a data element. The reference may be remote or local, depending on central
 * host (nimbus) or client (worker).
 * 
 * @param <E> the type of the data element behind the reference
 * @author Holger Eichelberger
 */
public interface IReference <E extends IDataElement> {

    /**
     * Connects the represented element.
     */
    public void connect();

    /**
     * Disconnects the represented element.
     */
    public void disconnect();

    /**
     * Disposes the represented element.
     */
    public void dispose();
    
    /**
     * Returns the id of the represented element.
     * 
     * @return the id
     */
    public String getId();
    
}
