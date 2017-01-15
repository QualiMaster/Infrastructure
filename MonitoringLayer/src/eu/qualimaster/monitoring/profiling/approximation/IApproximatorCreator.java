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
package eu.qualimaster.monitoring.profiling.approximation;

import java.io.File;

import eu.qualimaster.observables.IObservable;

/**
 * Creates instances of approximators.
 * 
 * @author Holger Eichelberger
 */
public interface IApproximatorCreator {
    
    /**
     * Creates an instance of an approximator.
     * 
     * @param strategy the storage strategy
     * @param path the path to load the approximator from
     * @param paramName the parameter name
     * @param observable the observable to approximate
     * @return the approximator instance
     */
    public IApproximator createApproximator(IStorageStrategy strategy, File path, Object paramName, 
        IObservable observable);

}
