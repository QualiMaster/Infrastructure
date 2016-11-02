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
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.fitting.AbstractCurveFitter;
import org.apache.commons.math3.fitting.HarmonicCurveFitter;

import eu.qualimaster.observables.IObservable;

/**
 * Implements a harmonic approximator.
 * 
 * @author Holger Eichelberger
 */
public class HarmonicApacheMathApproximator extends AbstractApacheMathApproximator {

    /**
     * An approximator creator for at maximum 10 fitting iterations.
     */
    public static final IApproximatorCreator INSTANCE_10 = new IApproximatorCreator() {
        
        @Override
        public IApproximator createApproximator(File path, Object paramName, IObservable observable) {
            return new HarmonicApacheMathApproximator(path, paramName, observable, 10);
        }
    };
    
    private int maxIterations;
    
    /**
     * Creates a polynomial approximator.
     * 
     * @param path the path to load a persisted version from
     * @param parameterName the parameter name
     * @param observable the observable this approximator is handling
     * @param maxIterations the maximum number of iterations for the optimization of the curve fitting
     */
    protected HarmonicApacheMathApproximator(File path, Object parameterName, IObservable observable, 
        int maxIterations) {
        super(path, parameterName, observable);
        this.maxIterations = Math.max(1, maxIterations);
    }

    @Override
    protected AbstractCurveFitter createFitter() {
        return HarmonicCurveFitter.create().withMaxIterations(maxIterations);
    }

    @Override
    protected ParametricUnivariateFunction createFunction() {
        return new HarmonicOscillator.Parametric();
    }

    @Override
    protected int getMinSampleSize() {
        return 3;
    }

}
