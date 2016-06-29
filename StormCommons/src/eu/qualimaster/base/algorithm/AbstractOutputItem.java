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

import org.apache.log4j.LogManager;

/**
 * Provides the abstract base implementation of an output item.
 * 
 * @param <T> the item type
 * @author Holger Eichelberger
 * @author Cui Qin
 */
public abstract class AbstractOutputItem <T extends IDirectGroupingInfo> implements IOutputItem <T> {

    private transient IItemsHolder<T> data;
    private transient IItemEmitter<T> emitter;

    /**
     * Creates an abstract top-level output item (for kryo). Call {@link #setParent(Object)} afterwards.
     */
    public AbstractOutputItem() {
        this(true); // fallback
    }
    
    /**
     * Creates an abstract output item. Call {@link #setParent(Object)} afterwards.
     * 
     * @param topLevel whether this is a top-level or a sub-item
     */
    public AbstractOutputItem(boolean topLevel) {
        if (topLevel) {
            data = new ItemsHolder<T>();
        } else {
            data = new NoItemsHolder<T>();
        }
    }
    
    @Override
    public int count() {
        return data.count();
    }

    @Override
    public void noOutput() {
        data.noOutput();
    }
    
    /**
     * Defines the (delegating) parent.
     * 
     * @param parent the parent
     */
    protected void setParent(T parent) {
        data.setParent(parent);
    }

    @Override
    public T addFurther() {
        return data.add(createItem());
    }

    @Override
    public IOutputItemIterator<T> iterator() {
        return data;
    }
    
    @Override
    public void clear() {
        data.clear();
    }
    
    /**
     * Sets the emitter for direct emitting. Sufficient to use this method if direct emitting shall be done.
     * 
     * @param emitter the emitter
     */
    public void setEmitter(IItemEmitter<T> emitter) {
        this.emitter = emitter;
    }
    
    @Override
    public void emitDirect(String streamId, T item) {
        if (null == emitter) {
            LogManager.getLogger(AbstractOutputItem.class).error("Direct emit not possible: No emitter");
        } else {
            emitter.emitDirect(streamId, item);
        }
    }

}