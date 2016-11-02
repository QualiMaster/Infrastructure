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
 * Implements an abstract approximator handling the storage strategy.
 * 
 * @author Holger Eichelberger
 */
public abstract class AbstractApproximator implements IApproximator {
    
    private IObservable observable;
    private Object parameterName;

    /**
     * Creates an abstract approximator.
     * 
     * @param path the path to load a persisted version from
     * @param parameterName the parameter name
     * @param observable the observable this approximator is handling
     */
    protected AbstractApproximator(File path, Object parameterName, IObservable observable) {
        this.observable = observable;
        this.parameterName = parameterName;
        load(path);
    }

    @Override
    public IObservable getObservable() {
        return observable;
    }
    
    @Override
    public Object getParameterName() {
        return parameterName;
    }

    /**
     * Loads the approximator.
     * 
     * @param path the path to load a persisted version from
     */
    protected abstract void load(File path);

}
