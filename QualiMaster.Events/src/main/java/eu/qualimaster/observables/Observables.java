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
package eu.qualimaster.observables;

import java.util.ArrayList;
import java.util.List;

/**
 * Summarizes all obserables.
 * 
 * @author Holger Eichelberger
 */
public class Observables {

    public static final IObservable[] OBSERVABLES = combine(TimeBehavior.values(), FunctionalSuitability.values(), 
        ResourceUsage.values(), Scalability.values(), AnalysisObservables.values(), CloudResourceUsage.values());

    /**
     * Combines multiple arrays of observables into one.
     * 
     * @param observables the observables to be combined
     * @return the combined observables
     */
    public static final IObservable[] combine(IObservable[] ... observables) {
        List<IObservable> tmp = new ArrayList<IObservable>();
        for (IObservable[] obs : observables) {
            for (IObservable o : obs) {
                tmp.add(o);
            }
        }
        IObservable[] result = new IObservable[tmp.size()];
        tmp.toArray(result);
        return result;
    }
    
    /**
     * Returns the observable with name <code>name</code>.
     * 
     * @param name the name to search for
     * @return the observable or <b>null</b>
     */
    public static final IObservable valueOf(String name) {
        IObservable result = null;
        for (int i = 0; i < OBSERVABLES.length; i++) {
            IObservable observable = OBSERVABLES[i];
            if (observable.name().equals(name)) {
                result = observable;
                break;
            }
        }
        return result;
    }
    
}
