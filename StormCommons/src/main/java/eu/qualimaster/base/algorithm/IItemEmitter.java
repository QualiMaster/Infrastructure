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
package eu.qualimaster.base.algorithm;

/**
 * Represents an emitter instance (implemented by a bolt or a spout) passed along with an {@link IOutputItem} for 
 * direct emitting.
 * 
 * @param <T> the tuple type
 * @author Holger Eichelberger
 */
public interface IItemEmitter <T extends IDirectGroupingInfo> {
    
    /**
     * Directly emits the <code>item</code> to the output without collecting it using the task id
     * provided by {@link IDirectGroupingInfo}.
     *  
     * @param streamId the stream to emit to
     * @param item the item to be emitted, consider {@link #createItem()} for individual instances
     */
    public void emitDirect(String streamId, T item);

}
