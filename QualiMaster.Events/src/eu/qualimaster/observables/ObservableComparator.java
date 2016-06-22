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

import java.util.Comparator;

/**
 * Implements a comparator for observables.
 * 
 * @author Holger Eichelberger
 */
public class ObservableComparator implements Comparator<IObservable> {

    public static final ObservableComparator INSTANCE = new ObservableComparator();

    /**
     * Prevents external creation. 
     */
    private ObservableComparator() {
    }
    
    @Override
    public int compare(IObservable o1, IObservable o2) {
        return o1.name().compareTo(o2.name());
    }

}
