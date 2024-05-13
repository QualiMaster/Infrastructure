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
package eu.qualimaster.monitoring.systemState;

import java.util.Set;

import eu.qualimaster.observables.IObservable;

/**
 * Implements a set-based link selector.
 * 
 * @author Holger Eichelberger
 */
public class SetLinkSelector extends ILinkSelector {

    private Set<IObservable> enabled;
    
    /**
     * Creates a link selector instance.
     * 
     * @param enabled the enabled instances
     */
    public SetLinkSelector(Set<IObservable> enabled) {
        this.enabled = enabled;
    }
    
    @Override
    protected boolean isLinkEnabled(IObservable observable) {
        return enabled.contains(observable);
    }

}
