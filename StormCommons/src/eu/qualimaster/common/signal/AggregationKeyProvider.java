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
package eu.qualimaster.common.signal;

/**
 * An aggregation key provider plugin.
 * 
 * @param <T> the tuple type
 * @author Holger Eichelberger
 */
public abstract class AggregationKeyProvider<T> {

    private Class<T> cls;
    
    /**
     * Creates an aggregation key provider.
     * 
     * @param cls the tuple class
     */
    public AggregationKeyProvider(Class<T> cls) {
        if (null == cls) {
            throw new IllegalArgumentException("cls must not be null");
        }
        this.cls = cls;
    }
    
    /**
     * Returns the aggregation key for <code>tuple</code>.
     * 
     * @param tuple the tuple
     * @return the aggregation key
     */
    public abstract String getAggregationKey(T tuple);
    
    /**
     * Returns the aggregation key for <code>tuple</code>.
     * 
     * @param tuple the tuple
     * @return the aggregation key
     */
    String getKey(Object tuple) {
        return getAggregationKey(cls.cast(tuple));
    }
    
}