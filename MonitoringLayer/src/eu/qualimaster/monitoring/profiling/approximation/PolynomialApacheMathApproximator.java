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

import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.AbstractCurveFitter;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;

import eu.qualimaster.observables.IObservable;

/**
 * Implements a polynomial approximator.
 * 
 * @author Holger Eichelberger
 */
public class PolynomialApacheMathApproximator extends AbstractApacheMathCurveFitterApproximator {

    /**
     * An approximator creator for polynoms of dregree 3.
     */
    public static final IApproximatorCreator INSTANCE_3 = new IApproximatorCreator() {
        
        @Override
        public IApproximator createApproximator(IStorageStrategy strategy, File path, Object paramName, 
            IObservable observable) {
            return new PolynomialApacheMathApproximator(strategy, path, paramName, observable, 3);
        }
    };
    
    private int degree;
    
    /**
     * Creates a polynomial approximator.
     * 
     * @param strategy the storage strategy
     * @param path the path to load a persisted version from
     * @param parameterName the parameter name
     * @param observable the observable this approximator is handling
     * @param degree the degree of the polynom to fit
     */
    public PolynomialApacheMathApproximator(IStorageStrategy strategy, File path, Object parameterName, 
        IObservable observable, int degree) {
        super(strategy, path, parameterName, observable);
        this.degree = Math.max(1, degree);
    }

    @Override
    protected AbstractCurveFitter createFitter() {
        return PolynomialCurveFitter.create(degree);
    }

    @Override
    protected ParametricUnivariateFunction createFunction() {
        return new PolynomialFunction.Parametric();
    }

    @Override
    protected int getMinSampleSize() {
        return degree;
    }

}
