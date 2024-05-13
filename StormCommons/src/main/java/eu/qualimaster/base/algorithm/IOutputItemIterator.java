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
package eu.qualimaster.base.algorithm;

/**
 * An output item iterator.
 * 
 * @param <T> the item type
 * @author Holger Eichelberger
 * @author Cui Qin
 */
public interface IOutputItemIterator<T> {

    /**
    * Does this output instance have more output instances (multiple outputs). 
    * 
    * @return <code>true</code> if there are more instances collected in this one, <code>false</code> else
    */
    public boolean hasNext();

    /**
    * Returns the next output instance.
    * 
    * @return the next output instance (<b>null</b> if there is no more instance)
    */
    public T next();
    
    /**
    * Resets the interator.
    */
    public void reset();

}
