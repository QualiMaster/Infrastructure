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
import java.io.IOException;

import eu.qualimaster.observables.IObservable;

/**
 * Approximates unknown values for given values of an observation.
 * 
 * @author Holger Eichelberger
 */
public interface IApproximator {

    /**
     * Updates a measured/predicted value to be related to a specific observation.
     * 
     * @param paramValue the (quantized) value of the parameter
     * @param value the assigned value (observation or prediction)
     * @param measured whether <code>value</code> was measured (<code>true</code>), i.e., is an observation or 
     *     predicted (<code>false</code>)
     */
    public void update(int paramValue, double value, boolean measured);
    
    /**
     * Approximates values for a given <code>paramValue</code>.
     * 
     * @param paramValue the (quantized) value of the parameter to return the approximation for
     * @return the approximation, {@link Constants#NO_APPROXIMATION} if no approximation is possible.
     */
    public double approximate(int paramValue);
    
    /**
     * The observable to approximate for.
     * 
     * @return the observable
     */
    public IObservable getObservable();
    
    /**
     * Returns the parameter name to approximate for.
     * 
     * @return the parameter name
     */
    public Object getParameterName();

    /**
     * Stores an approximator into a given folder.
     * 
     * @param folder the folder to store into
     * @return the file it stored into
     */
    public File store(File folder);

    /**
     * Returns whether this and the given approximator contain the same data. [testing]
     * 
     * @param approx the approximator to compare
     * @return <code>true</code> for the same data, <code>false</code> else
     */
    public boolean containsSameData(IApproximator approx);

    /**
     * Merges the data in <code>file</code> into this approximator.
     * 
     * @param file the file
     * @throws IOException if reading fails
     */
    public void merge(File file) throws IOException;
    
}
