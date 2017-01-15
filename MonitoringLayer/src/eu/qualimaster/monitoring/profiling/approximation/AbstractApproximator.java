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

import eu.qualimaster.monitoring.profiling.Constants;
import eu.qualimaster.observables.IObservable;

/**
 * Implements an abstract approximator handling the storage strategy.
 * 
 * @author Holger Eichelberger
 */
public abstract class AbstractApproximator implements IApproximator {
    
    private IObservable observable;
    private Object parameterName;
    private boolean updated = false;
    private IStorageStrategy strategy;

    /**
     * Creates an abstract approximator.
     * 
     * @param strategy the storage strategy
     * @param path the path to load a persisted version from
     * @param parameterName the parameter name
     * @param observable the observable this approximator is handling
     * @see #initialize()
     * @see #load(File)
     */
    protected AbstractApproximator(IStorageStrategy strategy, File path, Object parameterName, IObservable observable) {
        this.observable = observable;
        this.parameterName = parameterName;
        this.strategy = strategy;
        initialize();
        load(path);
    }
    
    /**
     * Returns the storage strategy.
     * 
     * @return the storage strategy
     */
    protected IStorageStrategy getStorageStrategy() {
        return strategy;
    }

    /**
     * Initializations that must be done before {@link #load(File)}.
     */
    protected void initialize() {
    }

    @Override
    public IObservable getObservable() {
        return observable;
    }
    
    @Override
    public Object getParameterName() {
        return parameterName;
    }

    @Override
    public double approximate(int paramValue) {
        double result = Constants.NO_APPROXIMATION;
        if (wasUpdated()) {
            updateApproximator();
            clearUpdated();
        }
        return result;
    }

    /**
     * Loads the approximator.
     * 
     * @param folder the path to load a persisted version from
     * @see #doLoad(File)
     */
    protected final void load(File folder) {
        doLoad(folder);
        updated();
        updateApproximator();
        clearUpdated();
    }
    
    /**
     * Performs the loading.
     * 
     * @param folder the path to load a persisted version from
     */
    protected abstract void doLoad(File folder);

    /**
     * Updates the approximator.
     */
    protected abstract void updateApproximator();
    
    /**
     * Notifies that an update happened and internal approximation information must be updated as well.
     */
    protected void updated() {
        updated = true;
    }
    
    /**
     * Returns whether an {@link #update(int, double, boolean)} happened in the mean time.
     * 
     * @return <code>true</code> if updated, <code>false</code> else
     */
    protected boolean wasUpdated() {
        return updated;
    }
    
    /**
     * Clears the updated flag.
     */
    protected void clearUpdated() {
        updated = false;
    }

}
