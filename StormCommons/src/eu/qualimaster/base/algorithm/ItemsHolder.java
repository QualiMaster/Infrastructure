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

import java.util.ArrayList;
import java.util.List;

/**
 * Implements an items holder holding sub-items.
 * 
 * @param <T> the items type
 * @author Holger Eichelberger
 * @author Cui Qin
 */
public class ItemsHolder<T> extends NoItemsHolder<T> {
    
    private transient List<T> items;
    
    /**
     * Creates an items holder from the given parent.
     */
    public ItemsHolder() {
        super();
    }

    @Override
    public T next() {
        T result;
        if (noOutput) {
            result = null;
        } else if (0 == pos) {
            result = parent;
        } else {
            if (null == items || pos > items.size()) {
                result = null;
            } else {
                result = items.get(pos - 1);
            }
        }
        if (null != result) {
            pos++;
        }
        return result;
    }

    @Override
    public int count() {
        return noOutput ? 0 : 1 + (null == items ? 0 : items.size());
    }

    @Override
    public T add(T item) {
        if (null == items) {
            items = new ArrayList<T>();
        }
        items.add(item);
        return item;
    }

    @Override
    public void clear() {
        super.clear();
        items = null;
    }

}