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
package eu.qualimaster.monitoring.events;

/**
 * Allows containing decisions upon inserting new elements whether this element shall be removed.
 * Please note that classes implementing this interface shall also implement / override <code>equals</code>
 * and <code>hashCode</code> from Object.
 * 
 * @author Holger Eichelberger
 */
public interface IRemovalSelector {

    /**
     * Returns whether this object shall be removed from its containing collection for <code>object</code>.
     * 
     * @param object the object to test for
     * @return <code>true</code> if this object shall be removed, <code>false</code> else
     */
    public boolean remove(Object object);
    
}
