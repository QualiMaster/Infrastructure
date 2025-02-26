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

import java.util.HashSet;
import java.util.Set;

import eu.qualimaster.observables.AnalysisObservables;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.Scalability;

/**
 * A selector/filter to link system pats against each other.
 * 
 * @author Holger Eichelberger
 */
public abstract class ILinkSelector {
    
    /**
     * Selects all observables.
     */
    public static final ILinkSelector ALL = new ILinkSelector() {
        
        @Override
        protected boolean isLinkEnabled(IObservable observable) {
            return !EXCLUDE.contains(observable);
        }
    };

    /**
     * Selects all externally visible observables (see {@link IObservable#isInternal()}).
     */
    public static final ILinkSelector ALL_EXTERNAL = new ILinkSelector() {
        
        @Override
        protected boolean isLinkEnabled(IObservable observable) {
            return !EXCLUDE.contains(observable) && !observable.isInternal();
        }
        
    };

    private static final Set<IObservable> EXCLUDE = new HashSet<IObservable>();
    
    static {
        EXCLUDE.add(Scalability.ITEMS);
        EXCLUDE.add(AnalysisObservables.IS_ENACTING);
        EXCLUDE.add(AnalysisObservables.IS_VALID);
    }
    
    /**
     * Returns whether the given <code>observable</code> enables (explicit) propagation of values.
     * 
     * @param observable the observable
     * @return <code>true</code> for propagation, <code>false</code> else
     */
    public static boolean enablePropagation(IObservable observable) {
        return !observable.isInternal();
    }

    /**
     * Returns whether the given <code>observable</code> shall be linked.
     * 
     * @param observable the observable to check
     * @return <code>true</code> if the observable shall be linked (default), <code>false</code> else
     * @see #linkImpl(SystemPart, boolean)
     */
    protected abstract boolean isLinkEnabled(IObservable observable);

}
