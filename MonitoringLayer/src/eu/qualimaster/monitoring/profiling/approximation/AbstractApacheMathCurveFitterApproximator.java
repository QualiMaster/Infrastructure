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
import java.util.List;

import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.fitting.AbstractCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.log4j.LogManager;

import eu.qualimaster.observables.IObservable;

/**
 * An approximator based on {@link AbstractCurveFitter}.
 * 
 * @author Holger Eichelberger
 */
public abstract class AbstractApacheMathCurveFitterApproximator extends AbstractApacheMathApproximator {

    private AbstractCurveFitter fitter = createFitter();
    private ParametricUnivariateFunction function = createFunction();
    private double[] coeff;

    /**
     * Creates an abstract approximator.
     * 
     * @param strategy the storage strategy
     * @param path the path to load a persisted version from
     * @param parameterName the parameter name
     * @param observable the observable this approximator is handling
     */
    protected AbstractApacheMathCurveFitterApproximator(IStorageStrategy strategy, File path, Object parameterName, 
        IObservable observable) {
        super(strategy, path, parameterName, observable);
    }
    
    /**
     * Returns the fitter.
     * 
     * @return the fitter
     */
    protected abstract AbstractCurveFitter createFitter();
    
    /**
     * Returns the univariate function matching {@link #createFitter()}.
     * 
     * @return the function
     */
    protected abstract ParametricUnivariateFunction createFunction();

    // checkstyle: stop exception type check
    
    @Override
    public double approximate(int paramValue) {
        double result = super.approximate(paramValue);
        if (null != coeff) {
            try {
                result = function.value(paramValue, coeff);
            } catch (Throwable t) {
                LogManager.getLogger(getClass()).warn("During approximation: " + t.getMessage());
            }
        }
        return result;
    }
    
    /**
     * Updates the coefficients and calls {@link #updated()}.
     */
    @Override
    protected void updateApproximator() {
        List<WeightedObservedPoint> points = getPoints();
        if (points.size() >= getMinSampleSize()) {
            try {
                coeff = fitter.fit(points);
                updated();
            } catch (Throwable t) {
                LogManager.getLogger(getClass()).warn("During update: " + t.getMessage());
            }
        }
    }

    // checkstyle: resume exception type check
    
}
