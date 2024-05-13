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
package eu.qualimaster.adaptation.external;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import eu.qualimaster.logging.events.LoggingFilterEvent;

/**
 * Implements a logging filter request.
 * 
 * @author Holger Eichelberger
 */
public class LoggingFilterRequest extends UsualMessage {

    public static final String REMOVE_ALL = LoggingFilterEvent.REMOVE_ALL;
    private static final long serialVersionUID = 2942520835422165826L;

    private ArrayList<String> additions = new ArrayList<String>();
    private ArrayList<String> removals = new ArrayList<String>();
    
    /**
     * Creates a logging filter request.
     * 
     * @param additions the additional Java filter regular expressions
     * @param removals the regular expressions to be removed from the filter, {@link #REMOVE_ALL} removes
     * all
     */
    public LoggingFilterRequest(Collection<String> additions, Collection<String> removals) {
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

    
    @Override
    public void dispatch(IDispatcher dispatcher) {
        dispatcher.handleLoggingFilterRequest(this);
    }

    @Override
    public int hashCode() {
        return Utils.hashCode(getFilterAdditions()) + Utils.hashCode(getFilterRemovals());
    }
    
    @Override
    public boolean equals(Object obj) {
        boolean equals = false;
        if (obj instanceof LoggingFilterRequest) {
            LoggingFilterRequest msg = (LoggingFilterRequest) obj;
            equals = Utils.equals(getFilterAdditions(), msg.getFilterAdditions());
            equals &= Utils.equals(getFilterRemovals(), msg.getFilterRemovals());
        }
        return equals;
    }

    @Override
    public Message toInformation() {
        return null; // do not pass on
    }

}
