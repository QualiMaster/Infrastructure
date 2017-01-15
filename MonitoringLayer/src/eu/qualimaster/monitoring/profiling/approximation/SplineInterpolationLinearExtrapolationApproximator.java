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

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import eu.qualimaster.observables.IObservable;

/**
 * Implements a Spline interpolation approximator with linear approximation for the values outside the known
 * domain.
 * 
 * @author Holger Eichelberger
 */
public class SplineInterpolationLinearExtrapolationApproximator extends AbstractApacheMathApproximator {

    /**
     * An approximator creator for at maximum 10 fitting iterations.
     */
    public static final IApproximatorCreator INSTANCE = new IApproximatorCreator() {
        
        @Override
        public IApproximator createApproximator(IStorageStrategy strategy, File path, Object paramName, 
            IObservable observable) {
            return new SplineInterpolationLinearExtrapolationApproximator(strategy, path, paramName, observable);
        }
    };
    
    private SplineInterpolator interpolator;
    private PolynomialSplineFunction function;
    private double[] knots;
    private PolynomialFunction[] splines;

    /**
     * Creates a polynomial approximator.
     * 
     * @param strategy the storage strategy
     * @param path the path to load a persisted version from
     * @param parameterName the parameter name
     * @param observable the observable this approximator is handling
     */
    public SplineInterpolationLinearExtrapolationApproximator(IStorageStrategy strategy, File path, 
        Object parameterName, IObservable observable) {
        super(strategy, path, parameterName, observable);
    }

    @Override
    public void update(int paramValue, double value, boolean measured) {
        super.update(paramValue, value, measured);
    }


    @Override
    protected int getMinSampleSize() {
        return 3;
    }

    @Override
    public double approximate(int paramValue) {
        // http://stackoverflow.com/questions/32076041/extrapolation-in-java
        double result = super.approximate(paramValue);
        if (null != function) {
            if (paramValue > knots[knots.length - 1]) {
                PolynomialFunction lastFunction = splines[splines.length - 1];
                result = lastFunction.value(paramValue - knots[knots.length - 2]);
            } else if (paramValue < knots[0]) {
                PolynomialFunction firstFunction = splines[0];
                result = firstFunction.value(paramValue - knots[0]);
            } else {
                result = function.value(paramValue);
            }
        }
        return result;
    }

    @Override
    protected void updateApproximator() {
        if (size() >= getMinSampleSize()) {
            double[][] data = getPointArrays();
            if (null == interpolator) {
                interpolator = new SplineInterpolator();
            }
            function = interpolator.interpolate(data[0], data[1]);
            knots = function.getKnots();
            splines = function.getPolynomials();
        }
    }

}
