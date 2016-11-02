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
package eu.qualimaster.monitoring.profiling;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import eu.qualimaster.monitoring.profiling.approximation.HarmonicApacheMathApproximator;
import eu.qualimaster.monitoring.profiling.approximation.IApproximatorCreator;
import eu.qualimaster.monitoring.profiling.quantizers.DoubleIntegerQuantizer;
import eu.qualimaster.monitoring.profiling.quantizers.IdentityIntegerQuantizer;
import eu.qualimaster.monitoring.profiling.quantizers.Quantizer;
import eu.qualimaster.monitoring.profiling.quantizers.ScalingDoubleQuantizer;
import eu.qualimaster.monitoring.profiling.validators.IValidator;
import eu.qualimaster.monitoring.profiling.validators.MinMaxValidator;
import eu.qualimaster.monitoring.profiling.validators.MinValidator;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.ResourceUsage;
import eu.qualimaster.observables.Scalability;
import eu.qualimaster.observables.TimeBehavior;

/**
 * A registry for singleton instances used during profiling. Was called <code>QuantizerRegistry</code> before.
 * 
 * @author Holger Eichelberger
 */
public class ProfilingRegistry {
    
    private static final Map<Class<? extends Serializable>, QuantizerInfo<?>> TYPE_QUANTIZERS = new HashMap<>();
    private static final Map<IObservable, QuantizerInfo<Double>> OBSERVABLE_QUANTIZERS = new HashMap<>();
    private static final Map<IObservable, Integer> PREDICTION_STEPS = new HashMap<>();
    private static final Map<IObservable, IValidator> VALIDATORS = new HashMap<>();
    private static final Map<IObservable, Double> APPROXIMATION_WEIGHTS = new HashMap<>();
    private static final Map<String, IApproximatorCreator> APPROXIMATION_CREATORS = new HashMap<>();
    
    static {
        // observable quantizers
        registerQuantizer(TimeBehavior.LATENCY, ScalingDoubleQuantizer.INSTANCE, true); // ms
        registerValidator(TimeBehavior.LATENCY, MinValidator.MIN_0_VALIDATOR);
        registerApproximationCreator(TimeBehavior.LATENCY, HarmonicApacheMathApproximator.INSTANCE_10);
        registerQuantizer(TimeBehavior.THROUGHPUT_ITEMS, ScalingDoubleQuantizer.INSTANCE, true);
        registerValidator(TimeBehavior.THROUGHPUT_ITEMS, MinValidator.MIN_0_VALIDATOR);
        registerApproximationCreator(TimeBehavior.THROUGHPUT_ITEMS, HarmonicApacheMathApproximator.INSTANCE_10);
        registerQuantizer(Scalability.ITEMS, ScalingDoubleQuantizer.INSTANCE, true);
        registerValidator(Scalability.ITEMS, MinValidator.MIN_0_VALIDATOR);
        registerApproximationCreator(Scalability.ITEMS, HarmonicApacheMathApproximator.INSTANCE_10);
        registerQuantizer(ResourceUsage.EXECUTORS, DoubleIntegerQuantizer.INSTANCE, true);
        registerValidator(ResourceUsage.EXECUTORS, MinValidator.MIN_0_VALIDATOR);
        registerQuantizer(ResourceUsage.TASKS, DoubleIntegerQuantizer.INSTANCE, true);
        registerValidator(ResourceUsage.TASKS, MinValidator.MIN_0_VALIDATOR);
        registerQuantizer(ResourceUsage.CAPACITY, ScalingDoubleQuantizer.INSTANCE, false);
        registerValidator(ResourceUsage.CAPACITY, MinMaxValidator.MIN_0_MAX_1_VALIDATOR);
        registerApproximationCreator(ResourceUsage.CAPACITY, HarmonicApacheMathApproximator.INSTANCE_10);
        
        // type quantizers for parameters
        registerQuantizer(IdentityIntegerQuantizer.INSTANCE, true);
        registerQuantizer(DoubleIntegerQuantizer.INSTANCE, true);
    }
    
    /**
     * Stores a quantizer and additional information.
     * 
     * @param <T> the value type the quantizer is operating on
     * @author Holger Eichelberger
     */
    private static class QuantizerInfo<T extends Serializable> {
        private Quantizer<T> quantizer;
        private boolean forKey;

        /**
         * Creates an information object.
         * 
         * @param quantizer the quantizer
         * @param forKey whether the quantizer shall be used for building profile keys 
         */
        private QuantizerInfo(Quantizer<T> quantizer, boolean forKey) {
            this.quantizer = quantizer;
            this.forKey = forKey;
        }
        
        /**
         * Returns the quantizer.
         * 
         * @return the quantizer
         */
        private Quantizer<T> getQuantizer() {
            return quantizer;
        }
        
        /**
         * Returns, whether the quantizer shall be used for building profile keys.
         * 
         * @return <code>true</code> for building profile keys, <code>false</code> else
         */
        private boolean forKey() {
            return forKey;
        }
        
    }
    
    /**
     * Returns the quantizer for an observable.
     * 
     * @param observable the observable
     * @param forKey whether the quantizer shall be used to determine the profile identification key
     * @return the {@link ProfilingRegistry} (may be <b>null</b> if there is none and the observable shall be ignored)
     */
    public static Quantizer<Double> getQuantizer(IObservable observable, boolean forKey) {
        Quantizer<Double> result = null;
        if (null != observable) {
            QuantizerInfo<Double> info = OBSERVABLE_QUANTIZERS.get(observable);
            if (null != info) {
                if ((forKey && info.forKey()) || !forKey) {
                    result = info.getQuantizer();
                }
            }
        }
        return result;
    }
    
    /**
     * Returns the quantizer for a serializable (parameter value).
     * 
     * @param serializable the serializable
     * @param forKey whether the quantizer shall be used to determine the profile identification key
     * @return the quantizer (may be <b>null</b> if there is none and the parameter shall be ignored)
     */
    public static Quantizer<?> getQuantizer(Serializable serializable, boolean forKey) {
        Quantizer<?> result = null;
        if (null != serializable) {
            QuantizerInfo<?> info = TYPE_QUANTIZERS.get(serializable.getClass());
            if (null != info) {
                if ((forKey && info.forKey()) || !forKey) {
                    result = info.getQuantizer();
                }
            }
        }
        return result;
    }
    
    /**
     * Registers an observable quantizer.
     * 
     * @param observable the observable
     * @param quantizer the quantizer (<b>null</b> unregisters the quantizer)
     * @param forKey whether the quantizer shall be used to determine the profile identification key
     */
    public static void registerQuantizer(IObservable observable, Quantizer<Double> quantizer, boolean forKey) {
        if (null != observable) {
            OBSERVABLE_QUANTIZERS.put(observable, new QuantizerInfo<Double>(quantizer, forKey));
        }
    }
    
    /**
     * Unregisters an observable quantizer.
     * 
     * @param observable the observable
     */
    public static void unregisterQuantizer(IObservable observable) {
        if (null != observable) {
            OBSERVABLE_QUANTIZERS.remove(observable);
        }
    }

    /**
     * Registers an observable validator.
     * 
     * @param observable the observable
     * @param validator the validator (<b>null</b> unregisters the quantizer)
     */
    public static void registerValidator(IObservable observable, IValidator validator) {
        if (null != observable) {
            VALIDATORS.put(observable, validator);
        }
    }

    /**
     * Unregisters an observable validator.
     * 
     * @param observable the observable
     */
    public static void unregisterValidator(IObservable observable) {
        if (null != observable) {
            VALIDATORS.remove(observable);
        }
    }

    /**
     * Returns a validator for a given <code>observable</code>.
     * 
     * @param observable the observable
     * @return the validator instance or <b>null</b> if no validator is known and the predicted value shall not 
     *     be validated
     */
    public static IValidator getValidator(IObservable observable) {
        IValidator result = null;
        if (null != observable) {
            result = VALIDATORS.get(observable);
        }
        return result;
    }

    /**
     * Registers a given type quantizer.
     * 
     * @param <T> the value type of the quantizer
     * @param quantizer the quantizer
     * @param forKey whether the quantizer shall be used to determine the profile identification key
     */
    public static <T extends Serializable> void registerQuantizer(Quantizer<T> quantizer, boolean forKey) {
        if (null != quantizer && null != quantizer.handles()) {
            TYPE_QUANTIZERS.put(quantizer.handles(), new QuantizerInfo<T>(quantizer, forKey));
        }
    }

    /**
     * Unregisters a given type quantizer.
     * 
     * @param quantizer the quantizer
     */
    public static void unregisterQuantizer(Quantizer<?> quantizer) {
        if (null != quantizer && null != quantizer.handles()) {
            TYPE_QUANTIZERS.remove(quantizer.handles());
        }
    }

    /**
     * Returns the number of prediction steps to apply for <code>observable</code>.
     * 
     * @param observable the observable
     * @return the number of prediction steps
     */
    public static int getPredictionSteps(IObservable observable) {
        Integer result = null;
        if (null != observable) {
            result = PREDICTION_STEPS.get(observable);
        }
        if (null == result) {
            result = 0; 
        }
        return result;
    }
    
    /**
     * Sets the prediction steps to default.
     * 
     * @param observable the observable
     */
    public static void defaultPredictionSteps(IObservable observable) {
        registerPredictionSteps(observable, 0);
    }
    
    /**
     * Registers the desired number of prediction steps.
     * 
     * @param observable the observable
     * @param steps the number of steps (negative value sets back to default)
     */
    public static void registerPredictionSteps(IObservable observable, int steps) {
        if (null != observable) {
            if (steps < 0) {
                PREDICTION_STEPS.remove(observable);
            } else {
                PREDICTION_STEPS.put(observable, steps);
            }
        }
    }
    
    /**
     * Returns the approximator creator for a given parameter/observable combination.
     * 
     * @param paramName the parameter name
     * @param observable the observable
     * @return the approximator creator, may be <b>null</b> if no approximator shall be created / used
     */
    public static IApproximatorCreator getApproximatorCreator(Object paramName, IObservable observable) {
        IApproximatorCreator result = null;
        if (null != paramName && null != observable) {
            String obsPostfix = "/" + observable.name();
            String key = paramName + obsPostfix;
            result = APPROXIMATION_CREATORS.get(key);
            if (null == result) { // fallback
                result = APPROXIMATION_CREATORS.get(obsPostfix);    
            }
        }
        return result; 
    }
    
    /**
     * Registers an approximation creator for all parameters and the given observable.
     * 
     * @param observable the observable to register (ignored if <b>null</b>)
     * @param creator the creator (ignored if <b>null</b>)
     */
    public static void registerApproximationCreator(IObservable observable, IApproximatorCreator creator) {
        registerApproximationCreator(null, observable, creator);
    }

    /**
     * Registers an approximation creator for the given parameter and the given observable.
     * 
     * @param paramName the parameter name (may be <b>null</b> to indicate all parameters)
     * @param observable the observable to register (ignored if <b>null</b>)
     * @param creator the creator (ignored if <b>null</b>)
     */
    public static void registerApproximationCreator(Object paramName, IObservable observable, 
        IApproximatorCreator creator) {
        if (null != observable && null != creator) {
            String key = null == paramName ? "" : paramName.toString();
            key += "/" + observable.toString();
            APPROXIMATION_CREATORS.put(key, creator);
        }
    }
    
    /**
     * Returns the approximation weights if multiple appoximations are present.
     * 
     * @param observable the observable
     * @return the approximation weight (1 by default)
     */
    public static double getApproximationWeight(IObservable observable) {
        double result = 1;
        if (null != observable) {
            Double weight = APPROXIMATION_WEIGHTS.get(observable);
            if (null != weight) {
                result = weight.doubleValue();
            }
        }
        return result;
    }
    
    /**
     * Registers an approximation weight.
     * 
     * @param observable the observable
     * @param weight the weight
     */
    public static void registerApproximationWeight(IObservable observable, double weight) {
        if (null != observable) {
            APPROXIMATION_WEIGHTS.put(observable, weight);
        }
    }
    
}
