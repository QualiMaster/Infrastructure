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
 * Defines the interface of an items holder, i.e., a holder for no or many sub-items.
 * 
 * @param <T> the item type
 * @author Holger Eichelberger
 * @author Cui Qin
 */
public interface IItemsHolder<T> extends IOutputItemIterator<T> {

    /**
     * Returns the number of items.
     * 
     * @return the number of items
     */
    public int count();

    /**
     * Declares that there shall be no output.
     */
    public void noOutput();
    
    /**
     * Adds an item.
     * 
     * @param item the item to be added
     * @return <code>item</code>
     */
    public T add(T item);
    
    /**
     * Clears the instance.
     */
    public void clear();
    
    /**
     * Defines the (delegating) parent.
     * 
     * @param parent the parent
     */
    public void setParent(T parent);
    
}