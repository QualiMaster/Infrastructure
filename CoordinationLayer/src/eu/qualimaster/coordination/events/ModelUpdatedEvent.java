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
package eu.qualimaster.coordination.events;

/**
 * Informs that the coordination model has been changed.
 * 
 * @author Holger Eichelberger
 */
public class ModelUpdatedEvent extends CoordinationEvent {

    private static final long serialVersionUID = 6449323533180161122L;
    private Type type;
    
    /**
     * The type of the event.
     * 
     * @author Holger Eichelberger
     */
    public enum Type {
        /**
         * Pre-announcement that the model will change. Time to unregister things.
         */
        CHANGING,

        /**
         * Announcement that the model changed. Time to register things again with the new model.
         */
        CHANGED;
    }

    /**
     * Creates a model updated event with given type.
     * 
     * @param type the type
     */
    public ModelUpdatedEvent(Type type) {
        this.type = type;
    }
    
    /**
     * Returns the type of the event.
     * 
     * @return the type
     */
    public Type getType() {
        return type;
    }

}
