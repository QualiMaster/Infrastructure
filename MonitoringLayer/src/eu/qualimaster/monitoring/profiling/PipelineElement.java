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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;

import eu.qualimaster.monitoring.profiling.approximation.IApproximator;
import eu.qualimaster.monitoring.profiling.approximation.IApproximatorCreator;
import eu.qualimaster.monitoring.profiling.approximation.IStorageStrategy;
import eu.qualimaster.monitoring.profiling.quantizers.Quantizer;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.tracing.Tracing;
import eu.qualimaster.monitoring.tracing.TraceReader.Entry;
import eu.qualimaster.monitoring.tracing.TraceReader.PipelineEntry;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.Scalability;

/**
 * Represents the actual state of a pipeline element.
 * 
 * @author Holger Eichelberger
 */
public class PipelineElement {

    private static boolean enableApproximation = true;

    private Pipeline pipeline;
    private String name;
    private String activeAlgorithm;
    
    /**
     * Maps parameter identifiers to parameter values (actual state).
     */
    private Map<Object, Serializable> parameters = new HashMap<>();
    
    /**
     * Maps profile keys to profiles.
     */
    private Map<Map<Object, Serializable>, IAlgorithmProfile> profiles = new HashMap<>();
    
    /**
     * Maps parameter identifiers and observables to an approximator for unknown points, i.e., 
     * paramValue x observableValue.
     */
    private Map<Object, Map<IObservable, IApproximator>> approximators = new HashMap<>();
    
    /**
     * Creates a pipeline element.
     * 
     * @param pipeline the parent pipeline
     * @param name the name of the element
     */
    PipelineElement(Pipeline pipeline, String name) {
        this.pipeline = pipeline;
        this.name = name;
    }
    
    /**
     * Returns the parent pipeline.
     * 
     * @return the parent pipeline
     */
    public Pipeline getPipeline() {
        return pipeline;
    }
    
    /**
     * Sets a current parameter value. [public for testing]
     * 
     * @param param the parameter name
     * @param value the parameter value
     */
    public void setParameter(String param, Serializable value) {
        parameters.put(param,  value);
    }
    
    /**
     * Defines the active algorithm. [public for testing]
     * 
     * @param activeAlgorithm the active algorithm
     */
    public void setActive(String activeAlgorithm) {
        this.activeAlgorithm = activeAlgorithm;
    }
    
    /**
     * Returns the responsible profile creator.
     * 
     * @return the creator
     */
    public IAlgorithmProfileCreator getProfileCreator() {
        return pipeline.getProfileCreator();
    }
    
    /**
     * Returns the storage path.
     * 
     * @return the path
     */
    public String getPath() {
        return pipeline.getPath();
    }
    
    /**
     * Returns whether the parent pipeline is in profiling mode.
     * 
     * @return <code>true</code> for profiling, <code>false</code> else
     */
    public boolean isInProfilingMode() {
        return pipeline.isInProfilingMode();
    }
    
    /**
     * Returns the name of the active algorithm.
     * 
     * @return the name of the algorithm
     */
    public String getActiveAlgorithm() {
        return activeAlgorithm;
    }
    
    /**
     * Returns the name of this pipeline element.
     * 
     * @return the name of this pipeline element
     */
    public String getName() {
        return name;
    }
    
    /**
     * Returns all profiles. [testing]
     * 
     * @return all profiles
     */
    public Collection<IAlgorithmProfile> profiles() {
        return profiles.values();
    }
    
    /**
     * Clears this instance.
     * 
     */
    void clear() {
        store();
        profiles.clear();
        parameters.clear();
    }
    
    /**
     * Clears this instance.
     */
    void store() {
        for (IAlgorithmProfile profile : profiles.values()) {
            profile.store();
        }
        File folder = getApproximatorsPath();
        for (Map.Entry<Object, Map<IObservable, IApproximator>> pEntry : approximators.entrySet()) {
            for (Map.Entry<IObservable, IApproximator> aEntry : pEntry.getValue().entrySet()) {
                IApproximator approximator = aEntry.getValue();
                approximator.store(folder);
            }
        }
    }

    /**
     * Returns the path to the approximators.
     * 
     * @return the path to the approximators
     */
    private File getApproximatorsPath() {
        IStorageStrategy storageStrategy = getProfileCreator().getStorageStrategy();
        return storageStrategy.getApproximatorsPath(this, getPath(), getKey(null, null));
    }
    
    /**
     * Creates a key for the predictor. [re-creating the key for each prediction may be inefficient, let's see]
     * 
     * @param algorithm the algorithm name (may be <b>null</b> for the active one)
     * @param override overridable parts of the key (may be <b>null</b>, ignored then)
     * @return the key
     */
    private Map<Object, Serializable> getKey(String algorithm, Map<Object, Serializable> override) {
        Map<Object, Serializable> result = new HashMap<Object, Serializable>();
        result.put(Constants.KEY_ALGORITHM, null == algorithm ? activeAlgorithm : algorithm);
        putAllForKey(result, parameters);
        putAllForKey(result, override);
        return result;
    }
    
    /**
     * Puts all quantizable and quantized mappings from <code>source</code> into target.
     * 
     * @param target the target map
     * @param source the source map
     */
    private void putAllForKey(Map<Object, Serializable> target, Map<Object, Serializable> source) {
        if (null != source) {
            for (Map.Entry<Object, Serializable> ent : source.entrySet()) {
                Object key = ent.getKey();
                Serializable value = ent.getValue();
                Quantizer<?> quantizer;
                if (key instanceof IObservable) {
                    quantizer = ProfilingRegistry.getQuantizer((IObservable) key, true);
                } else {
                    quantizer = ProfilingRegistry.getQuantizer(value, true);
                }
                if (null != quantizer) {
                    target.put(key, quantizer.quantize(value));
                }
            }
        }
    }

    /**
     * Obtains an algorithm profile for <code>key</code>, i.e., creates one if there is none.
     * 
     * @param key the key
     * @return the profile
     */
    private IAlgorithmProfile obtainProfile(Map<Object, Serializable> key) {
        IAlgorithmProfile profile = profiles.get(key);
        if (null == profile) {
            profile = getProfileCreator().createProfile(this, key);
            profiles.put(key, profile);
        }
        return profile;
    }

    /**
     * Updates the (internal) parameter for the input rate.
     * 
     * @param family the family to take the predecessors from
     */
    private void updateInputRate(PipelineNodeSystemPart family) {
        List<PipelineNodeSystemPart> pred = Tracing.getPredecessors(family);
        Quantizer<Double> quantizer = ProfilingRegistry.getQuantizer(Scalability.ITEMS, false);
        if (null != pred && null != quantizer) {
            double inputRate = 0;
            int predCount = pred.size();
            for (int p = 0; p < predCount; p++) {
                inputRate += pred.get(p).getObservedValue(Scalability.ITEMS);
            }
            //inputRate /= predCount;
            parameters.put(Constants.KEY_INPUT_RATE, quantizer.quantize(inputRate));
        }
    }
    
    /**
     * Updates the predictor(s).
     * 
     * @param family the actual measurements for this pipeline element / family
     */
    void update(PipelineNodeSystemPart family) {
        updateInputRate(family);
        Map<Object, Serializable> key = getKey(null, null);
        IAlgorithmProfile profile = obtainProfile(key);
        profile.update(family);

        for (IObservable obs : family.getObservables()) {
            if (family.hasValue(obs)) {
                updateParameterApproximators(obs, family.getObservedValue(obs), true);
            }
        }
    }

    /**
     * Updates the predictor(s) from a profiled CSV file.
     * 
     * @param pipelineEntry the pipeline to update the predictors
     * @param algorithm the name of the profiled algorithm
     * @param predecessors the predecessors of this pipeline element
     */
    void update(PipelineEntry pipelineEntry, String algorithm, List<String> predecessors) {
        this.setActive(algorithm);
        Entry entry = pipelineEntry.getNodeEntry(getName());
        if (null != entry) {
            Quantizer<Double> quantizer = ProfilingRegistry.getQuantizer(Scalability.ITEMS, false);
            if (null != predecessors && null != quantizer) {
                double inputRate = 0;
                for (String pred : predecessors) {
                    Double o = pipelineEntry.getNodeEntry(pred).getObservation(Scalability.ITEMS);
                    if (null != o) {
                        inputRate += o;
                    }
                }
                parameters.put(Constants.KEY_INPUT_RATE, quantizer.quantize(inputRate));
            }
            Map<Object, Serializable> key = getKey(null, null);
            IAlgorithmProfile profile = obtainProfile(key);
            profile.update(pipelineEntry.getTimestamp(), entry);
    
            for (IObservable obs : entry.observables()) {
                updateParameterApproximators(obs, entry.getObservation(obs), true);
            }
        }
    }

    /**
     * Updates the approximators for all parameters.
     * 
     * @param observable the observable to update for
     * @param value the value to be used for updating
     * @param measured whether <code>observation</code> was measured (<code>true</code>) or 
     *   predicted (<code>false</code>)
     */
    private void updateParameterApproximators(IObservable observable, double value, boolean measured) {
        for (Map.Entry<Object, Serializable> param : parameters.entrySet()) {
            Object paramName = param.getKey();
            Serializable paramValue = param.getValue();
            IApproximator approximator = obtainApproximator(paramName, observable);
            Quantizer<?> quantizer = ProfilingRegistry.getQuantizer(paramValue, false);
            if (null != approximator && null != quantizer) {
                approximator.update(quantizer.quantize(paramValue), value, measured);
            }
        }
    }
    
    /**
     * Returns an approximator or creates / loads one if required.
     * 
     * @param paramName the parameter name
     * @param observable the observable
     * @return the approximator or <b>null</b> if no approximator can be created
     */
    private IApproximator obtainApproximator(Object paramName, IObservable observable) {
        IApproximator result = null;
        Map<IObservable, IApproximator> obsApproximators = approximators.get(paramName);
        if (null != obsApproximators) {
            result = obsApproximators.get(observable);
        }
        if (null == result) {
            IApproximatorCreator creator = ProfilingRegistry.getApproximatorCreator(paramName, observable);
            if (null != creator) {
                result = creator.createApproximator(getProfileCreator().getStorageStrategy(), getApproximatorsPath(), 
                    paramName, observable);
                if (null != result) {
                    if (null == obsApproximators) {
                        obsApproximators = new HashMap<>();
                        approximators.put(paramName, obsApproximators);
                    }
                    obsApproximators.put(observable, result);
                }
            }
        }
        return result;
    }

    /**
     * Predicts the actual value for <code>observable</code> in <code>profile</code> or asks the approximators
     * if no prediction is possible. Updates the approximators.
     * 
     * @param profile the profile to predict for
     * @param observable the observable to predict for
     * @return the prediction or {@link Constants#NO_PREDICTION} if neither prediction nor approximation is possible
     */
    private double predict(IAlgorithmProfile profile, IObservable observable) {
        double result = profile.predict(observable, ProfilingRegistry.getPredictionSteps(observable));
        if (Constants.NO_PREDICTION != result) {
            updateParameterApproximators(observable, result, false);
        } else {
            double sum = 0;
            double weights = 0;
            int count = 0;
            if (enableApproximation) {
                for (Map.Entry<Object, Serializable> param : parameters.entrySet()) {
                    Object paramName = param.getKey();
                    Serializable paramValue = param.getValue();
                    IApproximator approximator = obtainApproximator(paramName, observable);
                    double weight = ProfilingRegistry.getApproximationWeight(observable);
                    Quantizer<?> quantizer = ProfilingRegistry.getQuantizer(paramValue, false);
                    if (null != approximator && weight != 0 && null != quantizer) {
                        double approx = approximator.approximate(quantizer.quantize(paramValue));
                        if (Constants.NO_APPROXIMATION != approx) {
                            sum += approx * weight;
                            weights += weight;
                            count++;
                        }
                    }                
                }
            }
            result = count > 0 ? sum / weights : Constants.NO_PREDICTION;
        }
        return result;
    }
    
    /**
     * Predict the next value for this pipeline element. If <code>targetValues</code> (observables or parameter values)
     * are given, the prediction shall take these into account, either to determine the related profile or to 
     * interpolate.
     * 
     * @param algorithm the name of the algorithm (take the active one if <b>null</b>)
     * @param observable the observable to predict
     * @param targetValues the target values for prediction. Predict the next step if <b>null</b> or empty. May contain
     *   observables ({@link IObservable}-Double) or parameter values (String-value)
     * @return the predicted value ({@link Constants#NO_PREDICTION} in case of no prediction)
     */
    double predict(String algorithm, IObservable observable, Map<Object, Serializable> targetValues) {
        Map<Object, Serializable> key = getKey(algorithm, targetValues);
        IAlgorithmProfile profile = obtainProfile(key);
        return predict(profile, observable);
    }

    /**
     * Predict the next value for all known parameters for this pipeline element.
     * 
     * @param algorithm the name of the algorithm (take the active one if <b>null</b>)
     * @param observables the observables to predict
     * @param result the result object to be modified as a side effect
     */
    void predict(String algorithm, Set<IObservable> observables, MultiPredictionResult result) {
        for (Map.Entry<Map<Object, Serializable>, IAlgorithmProfile> entry : profiles.entrySet()) {
            Map<Object, Serializable> key = entry.getKey();
            if (algorithm.equals(key.get(Constants.KEY_ALGORITHM))) {
                IAlgorithmProfile profile = entry.getValue();
                Map<IObservable, Double> algResults = new HashMap<IObservable, Double>();
                for (IObservable obs : observables) {
                    double predicted = predict(profile, obs);
                    algResults.put(obs, (Constants.NO_PREDICTION == predicted) ? null : predicted);
                }
                // don't return internal key object, remove algorithm pseudo-parameter
                Map<Object, Serializable> k = new HashMap<Object, Serializable>();
                k.putAll(key);
                k.remove(Constants.KEY_ALGORITHM);
                result.add(algorithm, k, algResults);
            }
        }
    }

    /**
     * Predicts parameter values for this pipeline element.
     * 
     * @param parameter the parameter name
     * @param observable the observable to predict for
     * @param targetValues the target values for prediction. Predict the next step if <b>null</b> or empty. May contain
     *   observables ({@link IObservable}-Double) or parameter values (String-value)
     * @return the parameter-observable-prediction mapping, <b>null</b> if there are no predictions
     */
    Map<String, Double> predictParameterValues(String parameter, IObservable observable, 
        Map<Object, Serializable> targetValues) {
        Map<String, Double> result = null; 
        try {
            // just use the key, parameter will be ignored
            Map<Object, Serializable> key = getKey(null, targetValues);
            List<String> values = getProfileCreator().getKnownParameterValues(this, key, observable, parameter);
            // now with the known values
            if (!values.isEmpty()) {
                Map<Object, Serializable> tv = new HashMap<Object, Serializable>();
                if (null != targetValues) {
                    tv.putAll(targetValues);
                }
                for (String value : values) {
                    tv.put(parameter, value);
                    IAlgorithmProfile profile = obtainProfile(key);
                    double pred = predict(profile, observable);
                    if (pred != Constants.NO_PREDICTION) {
                        if (null == result) {
                            result = new HashMap<String, Double>();
                        }
                        result.put(value, pred);
                    }
                }
            }
        } catch (IOException e) {
            LogManager.getLogger(getClass()).warn("No parameter value predictions due to " + e.getMessage());
        }
        return result;
    }
    
    /**
     * Enables or disables the approximation functionality in case that parameters do not match any known profile.
     * 
     * @param enable <code>true</code> enable (default), <code>false</code> disable
     */
    static void enableApproximation(boolean enable) {
        enableApproximation = enable;
    }
    
}