/*
 * Copyright 2009-2014 University of Hildesheim, Software Systems Engineering
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
package eu.qualimaster.logging.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import eu.qualimaster.common.QMInternal;
import eu.qualimaster.events.AbstractEvent;

/**
 * Implements an event to change the (remote) logging filters.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public class LoggingFilterEvent extends AbstractEvent {

    /**
     * Indicates that all regular expressions shall be removed.
     */
    public static final String REMOVE_ALL = "*";
    private static final long serialVersionUID = 6289180595762057010L;
    
    private ArrayList<String> additions = new ArrayList<String>();
    private ArrayList<String> removals = new ArrayList<String>();
    
    /**
     * Creates a logging filter event.
     * 
     * @param additions the additional Java filter regular expressions
     * @param removals the regular expressions to be removed from the filter, {@link #REMOVE_ALL} removes
     * all
     */
    public LoggingFilterEvent(Collection<String> additions, Collection<String> removals) {
        if (null != additions) {
            this.additions.addAll(additions);
        }
        if (null != removals) {
            this.removals.addAll(removals);
        }
    }
    
    /**
     * Returns the filter additions.
     * 
     * @return the filter additions
     */
    public List<String> getFilterAdditions() {
        return additions;
    }

    /**
     * Returns the filter removals.
     * 
     * @return the filter removals
     */
    public List<String> getFilterRemovals() {
        return removals;
    }
    
}
