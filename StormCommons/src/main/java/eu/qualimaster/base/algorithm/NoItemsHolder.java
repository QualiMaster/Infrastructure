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
 * A holder that holds no items except for the given parent one.
 * 
 * @param <T> the item type
 * @author Holger Eichelberger
 * @author Cui Qin
 */
public class NoItemsHolder<T> implements IItemsHolder<T> { 

    protected transient int pos;
    protected boolean noOutput;
    protected transient T parent;

    /**
     * Creates a no items holder from the given parent. Call {@#setParent(Object)} afterwards!
     */
    public NoItemsHolder() {
        clear();
    }
    
    /**
     * Creates a no items holder with parent.
     * 
     * @param parent the parent
     */
    public NoItemsHolder(T parent) {
        this();
        setParent(parent);
    }
    
    /**
     * Defines the (delegating) parent.
     * 
     * @param parent the parent
     */
    public void setParent(T parent) {
        this.parent = parent;
    }

    @Override
    public void noOutput() {
        noOutput = true;
    }

    @Override
    public void reset() {
        pos = 0;
    }

    @Override
    public boolean hasNext() {
        return noOutput ? false : pos < count();
    }

    @Override
    public T next() {
        return noOutput ? null : (0 == pos++ ? parent : null);
    }

    @Override
    public int count() {
        return noOutput ? 0 : 1;
    }

    @Override
    public T add(T item) {
        return item;
    }

    @Override
    public void clear() {
        pos = 0;
        noOutput = false;
    }

}