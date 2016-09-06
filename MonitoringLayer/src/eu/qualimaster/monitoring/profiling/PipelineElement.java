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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.qualimaster.monitoring.profiling.quantizers.Quantizer;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.tracing.Tracing;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.Scalability;

/**
 * Represents the actual state of a pipeline element.
 * 
 * @author Holger Eichelberger
 */
public class PipelineElement {
    
    private Pipeline pipeline;
    private String name;
    private String activeAlgorithm;
    private Map<Object, Serializable> parameters = new HashMap<>();
    private Map<Object, IAlgorithmProfile> profiles = new HashMap<>();
    
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
     * Sets a current parameter value.
     * 
     * @param param the parameter name
     * @param value the parameter value
     */
    void setParameter(String param, Serializable value) {
        parameters.put(param,  value);
    }
    
    /**
     * Defines the active algorithm.
     * 
     * @param activeAlgorithm the active algorithm
     */
    void setActive(String activeAlgorithm) {
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
                    quantizer = QuantizerRegistry.getQuantizer((IObservable) key);
                } else {
                    quantizer = QuantizerRegistry.getQuantizer(value);
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
        Quantizer<Double> quantizer = QuantizerRegistry.getQuantizer(Scalability.ITEMS);
        if (null != pred && null != quantizer) {
            double inputRate = 0;
            int predCount = pred.size();
            for (int p = 0; p < predCount; p++) {
                inputRate += pred.get(p).getObservedValue(Scalability.ITEMS);
            }
            inputRate /= predCount;
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
     * @return the predicted value (<code>Double.MIN_VALUE</code> in case of no prediction)
     */
    double predict(String algorithm, IObservable observable, Map<Object, Serializable> targetValues) {
        Map<Object, Serializable> key = getKey(algorithm, targetValues);
        IAlgorithmProfile profile = obtainProfile(key);
        return profile.predict(observable, QuantizerRegistry.getPredictionSteps(observable));
    }
    
}