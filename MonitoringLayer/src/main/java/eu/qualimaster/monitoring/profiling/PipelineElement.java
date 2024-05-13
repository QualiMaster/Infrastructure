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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;

import eu.qualimaster.coordination.INameMapping;
import eu.qualimaster.monitoring.MonitoringManager;
import eu.qualimaster.monitoring.profiling.approximation.IApproximator;
import eu.qualimaster.monitoring.profiling.approximation.IApproximatorCreator;
import eu.qualimaster.monitoring.profiling.approximation.IStorageStrategy;
import eu.qualimaster.monitoring.profiling.approximation.IStorageStrategy.ProfileKey;
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
     * Returns all parameters.
     * 
     * @return the parameters (copy)
     */
    public Map<Object, Serializable> getParameters() {
        Map<Object, Serializable> result = new HashMap<Object, Serializable>();
        result.putAll(parameters);
        return result;
    }
    
    /**
     * Changes all parameters as given.
     * 
     * @param parameters the new/overriding parameters (ignored if <b>null</b>)
     */
    public void setParameters(Map<Object, Serializable> parameters) {
        if (null != parameters) {
            this.parameters.putAll(parameters);
        }
    }
    
    /**
     * Sets a current parameter value. [public for testing]
     * 
     * @param param the parameter name
     * @param value the parameter value
     */
    public void setParameter(Object param, Serializable value) {
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
        List<IAlgorithmProfile> result = new ArrayList<IAlgorithmProfile>();
        synchronized (profiles) {
            result.addAll(profiles.values());
        }
        return result;
    }
    
    /**
     * Clears outdated profiles.
     */
    void clearOutdatedProfiles() {
        synchronized (profiles) {
            Iterator<IAlgorithmProfile> iter = profiles.values().iterator();
            while (iter.hasNext()) {
                IAlgorithmProfile profile = iter.next();
                if (Utils.isOutdated(profile)) {
                    iter.remove();
                    profile.store();
                }
            }
        }
    }
    
    /**
     * Clears this instance.
     * 
     */
    void clear() {
        store();
        synchronized (profiles) {
            profiles.clear();
        }
        parameters.clear();
    }
    
    /**
     * Clears this instance.
     */
    void store() {
        synchronized (profiles) {
            for (IAlgorithmProfile profile : profiles.values()) {
                profile.store();
            }
        }
        File folder = getApproximatorsPath(null);
        for (Map.Entry<Object, Map<IObservable, IApproximator>> pEntry : approximators.entrySet()) {
            for (IApproximator approximator : pEntry.getValue().values()) {
                approximator.store(folder);
            }
        }
    }

    /**
     * Returns the path to the approximators.
     * 
     * @param algorithm the algorithm to retrieve the approximator for, the actual one if <b>null</b>
     * @return the path to the approximators
     */
    private File getApproximatorsPath(String algorithm) {
        IStorageStrategy storageStrategy = getProfileCreator().getStorageStrategy();
        return storageStrategy.getApproximatorsPath(this, getPath(), getKey(algorithm, null, true));
    }
    
    /**
     * Creates a key for the predictor. [re-creating the key for each prediction may be inefficient, let's see]
     * 
     * @param algorithm the algorithm name (may be <b>null</b> for the active one)
     * @param override overridable parts of the key (may be <b>null</b>, ignored then)
     * @param addParam add the parameters recorded by this element
     * @return the key
     */
    private Map<Object, Serializable> getKey(String algorithm, Map<Object, Serializable> override, boolean addParam) {
        Map<Object, Serializable> result = new HashMap<Object, Serializable>();
        result.put(Constants.KEY_ALGORITHM, null == algorithm ? activeAlgorithm : algorithm);
        if (addParam) {
            putAllForKey(result, parameters);
        }
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
        IAlgorithmProfile profile;
        synchronized (profiles) {
            profile = profiles.get(key);
        }
        if (null == profile) {
            profile = getProfileCreator().createProfile(this, key);
            synchronized (profiles) {
                profiles.put(key, profile);
            }
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
        Map<Object, Serializable> key = getKey(null, null, true);
        IAlgorithmProfile profile = obtainProfile(key);
        profile.update(family);

        for (IObservable obs : family.getObservables()) {
            if (family.hasValue(obs)) {
                Double value = family.getObservedValue(obs);
                if (ProfilingRegistry.storeAsParameter(obs)) {
                    parameters.put(obs, value);
                }
                updateParameterApproximators(null, obs, value, true);
            }
        }
        clearOutdatedProfiles();
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
            Map<Object, Serializable> key = getKey(null, null, true);
            IAlgorithmProfile profile = obtainProfile(key);
            profile.update(pipelineEntry.getTimestamp(), entry);
    
            for (IObservable obs : entry.observables()) {
                Double value = entry.getObservation(obs);
                if (null != value) {
                    if (ProfilingRegistry.storeAsParameter(obs)) {
                        parameters.put(obs, value);
                    }
                    updateParameterApproximators(algorithm, obs, value, true);
                }
            }
        }
    }

    /**
     * Updates the approximators for all parameters.
     * 
     * @param algorithm the algorithm to update the approximator for, use the current one if <b>null</b>
     * @param observable the observable to update for
     * @param value the value to be used for updating
     * @param measured whether <code>observation</code> was measured (<code>true</code>) or 
     *   predicted (<code>false</code>)
     */
    private void updateParameterApproximators(String algorithm, IObservable observable, double value, 
        boolean measured) {
        for (Map.Entry<Object, Serializable> param : parameters.entrySet()) {
            Object paramName = param.getKey();
            Serializable paramValue = param.getValue();
            IApproximator approximator = obtainApproximator(algorithm, paramName, observable);
            Quantizer<?> quantizer = ProfilingRegistry.getQuantizer(paramValue, false);
            if (null != approximator && null != quantizer) {
                approximator.update(quantizer.quantize(paramValue), value, measured);
            }
        }
    }
    
    /**
     * Returns an approximator or creates / loads one if required.
     * 
     * @param algorithm the algorithm to obtain the approximator for, use the current one if <b>null</b>
     * @param paramName the parameter name
     * @param observable the observable
     * @return the approximator or <b>null</b> if no approximator can be created
     */
    private IApproximator obtainApproximator(String algorithm, Object paramName, IObservable observable) {
        IApproximator result = null;
        String alg = null == algorithm ? this.activeAlgorithm : algorithm;
        if (null != alg) {
            String key = algorithm + "/" + paramName;
            Map<IObservable, IApproximator> obsApproximators = approximators.get(key);
            if (null != obsApproximators) {
                result = obsApproximators.get(observable);
            }
            if (null == result) {
                IApproximatorCreator creator = ProfilingRegistry.getApproximatorCreator(paramName, observable);
                if (null != creator) {
                    result = creator.createApproximator(getProfileCreator().getStorageStrategy(), 
                        getApproximatorsPath(algorithm), paramName, observable);
                    registerApproximator(result, observable, key);
                }
            }
        }
        return result;
    }

    /**
     * Registers an approximator.
     * 
     * @param approximator the approximator, call will be ignored if <b>null</b>
     * @param observable the observable to register for
     * @param key the key (consisting of algorithm and parameter name)
     */
    private void registerApproximator(IApproximator approximator, IObservable observable, String key) {
        if (null != approximator) {
            Map<IObservable, IApproximator> obsApproximators = approximators.get(key);
            if (null == obsApproximators) {
                obsApproximators = new HashMap<>();
                approximators.put(key, obsApproximators);
            }
            obsApproximators.put(observable, approximator);
        }
    }

    /**
     * Predicts the actual value for <code>observable</code> in <code>profile</code> or asks the approximators
     * if no prediction is possible. Updates the approximators.
     * 
     * @param algorithm the name of the algorithm (take the active one if <b>null</b>)
     * @param profile the profile to predict for
     * @param observable the observable to predict for
     * @return the prediction or {@link Constants#NO_PREDICTION} if neither prediction nor approximation is possible
     */
    private double predict(String algorithm, IAlgorithmProfile profile, IObservable observable) {
        double result = profile.predict(observable, ProfilingRegistry.getPredictionSteps(observable));
        if (Constants.NO_PREDICTION != result) {
            updateParameterApproximators(algorithm, observable, result, false);
        } else {
            double sum = 0;
            double weights = 0;
            int count = 0;
            if (enableApproximation) {
                for (Map.Entry<Object, Serializable> param : parameters.entrySet()) {
                    Object paramName = param.getKey();
                    Serializable paramValue = param.getValue();
                    IApproximator approximator = obtainApproximator(algorithm, paramName, observable);
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
        Map<Object, Serializable> key = getKey(algorithm, targetValues, true);
        IAlgorithmProfile profile = obtainProfile(key);
        return predict(algorithm, profile, observable);
    }
    
    /**
     * Predict the next value for all known parameters for this pipeline element.
     * 
     * @param algorithm the name of the algorithm (take the active one if <b>null</b>)
     * @param observables the observables to predict
     * @param result the result object to be modified as a side effect
     */
    void predict(String algorithm, Set<IObservable> observables, MultiPredictionResult result) {
        IAlgorithmProfileCreator creator = getProfileCreator();
        IStorageStrategy strategy = creator.getStorageStrategy();
        Map<Map<Object, Serializable>, Map<IObservable, Double>> algResults = new HashMap<>();
        Map<Object, Serializable> filter = getFilterParameters(algorithm);
        for (IObservable obs : observables) {
            File path = strategy.getPredictorPath(getPipeline().getName(), getName(), algorithm, getPath(), 
                obs, creator);
            MapFile mapFile = new MapFile(path);
            try {
                mapFile.load();
                for (String k : mapFile.keys()) {
                    ProfileKey parsed = strategy.parseKey(k);
                    if (matchesFilter(parsed.getParameter(), filter)) {
                        Map<Object, Serializable> key = getKey(algorithm, parsed.getParameter(), false);
                        IAlgorithmProfile profile = obtainProfile(key);
                        double predicted = predict(algorithm, profile, obs);
                        // don't return internal key object, remove algorithm pseudo-parameter
                        Map<Object, Serializable> param = new HashMap<Object, Serializable>();
                        param.putAll(parsed.getParameter()); // in sequence
                        param.remove(Constants.KEY_ALGORITHM);
                        Map<IObservable, Double> res = algResults.get(param);
                        if (null == res) {
                            res = new HashMap<IObservable, Double>();
                            takeOverParameters(parsed.getParameter(), res);
                            algResults.put(param, res);
                        }
                        res.put(obs, (Constants.NO_PREDICTION == predicted) ? null : predicted);
                    }
                }
            } catch (IOException e) {
            }
        }
        for (Map.Entry<Map<Object, Serializable>, Map<IObservable, Double>> ent : algResults.entrySet()) {
            result.add(algorithm, ent.getKey(), ent.getValue());                
        }
    }

    /**
     * Returns whether the given parameters matches <code>filter</code>, i.e., all entries and values in filter
     * are also in <code>parameters</code>.
     * 
     * @param parameters the parameters
     * @param filter the filter (all values are not <b>null</b>)
     * @return <code>true</code> if the filter matches, <code>false</code> else
     */
    private boolean matchesFilter(Map<Object, Serializable> parameters, Map<Object, Serializable> filter) {
        boolean result = false;
        if (null == parameters) {
            result = filter.isEmpty();
        } else {
            result = true;
            for (Map.Entry<Object, Serializable> f : filter.entrySet()) {
                if (!f.getValue().equals(parameters.get(f.getKey()))) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }
    
    /**
     * Returns the filter parameters. Loosely integrated algorithms shall have less filter entries and more freedom.
     * 
     * @param algorithm the name of the algorithm (take the active one if <b>null</b>)
     * @return the filter parameters, values in the mapping are not <b>null</b>
     */
    private Map<Object, Serializable> getFilterParameters(String algorithm) {
        Map<Object, Serializable> result = new HashMap<Object, Serializable>();
        boolean fixToParameter = fixToParameter(algorithm);
        for (Map.Entry<Object, Serializable> ent : parameters.entrySet()) {
            Object name = ent.getKey();
            Serializable value = ent.getValue();
            boolean add = false;
            if (Constants.KEY_INPUT_RATE.equals(name)) {
                add = true;
            } else if (fixToParameter && !Constants.KEY_ALGORITHM.equals(name)) {
                add = fixAsParameter(name);
            }
            if (add && null != value) { // null != value just for simplifying filter method
                result.put(name, value);
            }
        }
        return result;
    }

    /**
     * Returns whether the given parameter name.
     * 
     * @param name the parameter "name"
     * @return <code>true</code> if the parameter shall be fixed as parameter, <code>false</code> else
     * @see ProfilingRegistry#fixAsParameter(IObservable)
     */
    private boolean fixAsParameter(Object name) {
        boolean result = false;
        if (name instanceof IObservable) {
            result = ProfilingRegistry.fixAsParameter((IObservable) name);
        }
        return result;
    }
    
    /**
     * Fixes the filter for this element to parameters.
     * 
     * @param algorithm the name of the algorithm (take the active one if <b>null</b>)
     * @return <code>true</code> if fix to parameters, <code>false</code> if more flexibility in prediction is allowed
     */
    private boolean fixToParameter(String algorithm) {
        boolean result = true;
        INameMapping mapping = MonitoringManager.getNameMapping(getPipeline().getName());
        String alg = null != algorithm ? algorithm : this.activeAlgorithm;
        if (null != mapping && null != alg) {
            result = null == mapping.getSubPipelineByAlgorithmName(alg); // do not fix in case of sub-topology
        }
        return result;
    }
    
    /**
     * Takes over storable parameters into result.
     * 
     * @param params the original parameter set
     * @param result the resulting observations
     */
    private void takeOverParameters(Map<Object, Serializable> params, Map<IObservable, Double> result) {
        for (Map.Entry<Object, Serializable> ent : params.entrySet()) {
            Object key = ent.getKey();
            Serializable val = ent.getValue();
            if (key instanceof IObservable && val instanceof Double) {
                IObservable obs = (IObservable) key;
                if (ProfilingRegistry.storeAsParameter(obs)) {
                    result.put(obs, (Double) val);
                }
            }
        }
    }

    /**
     * Predicts parameter values for this pipeline element. Based on the actual settings for the algorithms.
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
            Map<Object, Serializable> key = getKey(null, targetValues, true);
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
                    double pred = predict(null, profile, observable);
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