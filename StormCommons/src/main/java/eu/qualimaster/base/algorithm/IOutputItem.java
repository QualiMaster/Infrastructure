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
 * Defines the basic interface of an output item.
 * 
 * @param <T> the element type
 * @author Holger Eichelberger
 * @author Cui Qin
 */
public interface IOutputItem<T extends IDirectGroupingInfo> extends IItemEmitter<T> {
    
    /**
    * Returns the number of instances represented by this instance.
    * 
    * @return the number of instances
    */
    public int count();

    /**
    * Declares that there is no output produced by the algorithm.
    */
    public void noOutput();

    /**
    * Adds a further output instance in case that the algorithm produces multiple outputs per input.
    * 
    * @return <code>true</code> the added instance
    */
    public T addFurther();

    /**
     * Returns the output item iterator for this item instance, i.e. an iterator including sub-tuples.
     * 
     * @return the output item iterator
     */
    public IOutputItemIterator<T> iterator();
    
    /**
     * Clears the (sub-) items.
     */
    public void clear();
    
    /**
     * Creates a sub-item instance.
     * 
     * @return the instance
     */
    public T createItem();
    
}
