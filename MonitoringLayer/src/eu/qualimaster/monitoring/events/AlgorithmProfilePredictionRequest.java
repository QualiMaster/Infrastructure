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
package eu.qualimaster.monitoring.events;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import eu.qualimaster.common.QMInternal;
import eu.qualimaster.events.AbstractReturnableEvent;
import eu.qualimaster.observables.IObservable;

/**
 * An event for requesting an algorithm profile prediction.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public class AlgorithmProfilePredictionRequest extends AbstractReturnableEvent {

    private static final long serialVersionUID = 6396953916340985524L;
    private String pipeline;
    private String pipelineElement;
    private String algorithm;
    @Deprecated
    private Map<IObservable, Double> weighting;
    private IObservable observable;
    private Map<Object, Serializable> targetValues;
    private Set<String> algorithms;
    private Set<IObservable> observables;

    /**
     * Creates a request (internal).
     * 
     * @param pipeline the pipeline to predict for
     * @param pipelineElement the pipeline element
     * @param targetValues the target values for a modified situation (may be <b>null</b> if just the algorithm may 
     *     change based on the current situation)
     */
    private AlgorithmProfilePredictionRequest(String pipeline, String pipelineElement, 
        Map<Object, Serializable> targetValues) {
        this.pipeline = pipeline;
        this.pipelineElement = pipelineElement;
        this.targetValues = targetValues;
    }
    
    /**
     * Creates a request for predicting the characteristics for a given algorithm without changes in the setting.
     * 
     * @param pipeline the pipeline to predict for
     * @param pipelineElement the pipeline element
     * @param algorithm the algorithm (may be <b>null</b> for the actual one)
     * @param observable the observable to predict for
     */
    public AlgorithmProfilePredictionRequest(String pipeline, String pipelineElement, String algorithm, 
        IObservable observable) {
        this(pipeline, pipelineElement, algorithm, observable, null);
    }
    
    /**
     * Creates a request for predicting the characteristics for a given algorithm in a target setting.
     * 
     * @param pipeline the pipeline to predict for
     * @param pipelineElement the pipeline element
     * @param algorithm the algorithm (may be <b>null</b> for the actual one)
     * @param observable the observable to predict for
     * @param targetValues the target values for a modified situation (may be <b>null</b> if just the algorithm may 
     *     change based on the current situation)
     */
    public AlgorithmProfilePredictionRequest(String pipeline, String pipelineElement, String algorithm, 
        IObservable observable, Map<Object, Serializable> targetValues) {
        this(pipeline, pipelineElement, targetValues);
        this.algorithm = algorithm;
        this.observable = observable;
    }
    
    /**
     * Creates a request to obtain the best algorithm in this situation.
     * 
     * @param pipeline the pipeline to predict for
     * @param pipelineElement the pipeline element
     * @param weighting the weighting
     * @param targetValues the target values for a modified situation (may be <b>null</b> if just the algorithm may 
     *     change based on the current situation)
     * @deprecated use {@link #AlgorithmProfilePredictionRequest(String, String, Set, Map, Map)} instead
     */
    @Deprecated
    public AlgorithmProfilePredictionRequest(String pipeline, String pipelineElement, 
        Map<IObservable, Double> weighting, Map<Object, Serializable> targetValues) {
        this(pipeline, pipelineElement, null, weighting, targetValues);
    }
    
    /**
     * Creates a request to obtain the best algorithm in this situation.
     * 
     * @param pipeline the pipeline to predict for
     * @param pipelineElement the pipeline element
     * @param algorithms the algorithms to predict for
     * @param weighting the weighting
     * @param targetValues the target values for a modified situation (may be <b>null</b> if just the algorithm may 
     *     change based on the current situation)
     * @deprecated use {@link #AlgorithmProfilePredictionRequest(String, String, Set, Set, Map)} instead
     */
    @Deprecated
    public AlgorithmProfilePredictionRequest(String pipeline, String pipelineElement, Set<String> algorithms, 
        Map<IObservable, Double> weighting, Map<Object, Serializable> targetValues) {
        this(pipeline, pipelineElement, targetValues);
        this.weighting = weighting;
        this.algorithms = algorithms;
    }
    
    /**
     * Creates a request to obtain a mass prediction for a set of algorithms / observables in this situation.
     * 
     * @param pipeline the pipeline to predict for
     * @param pipelineElement the pipeline element
     * @param algorithms the algorithms to predict for
     * @param observables the observables to predict for
     * @param targetValues the target values for a modified situation (may be <b>null</b> if just the algorithm may 
     *     change based on the current situation)
     */
    public AlgorithmProfilePredictionRequest(String pipeline, String pipelineElement, Set<String> algorithms, 
        Set<IObservable> observables, Map<Object, Serializable> targetValues) {
        this(pipeline, pipelineElement, targetValues);
        this.observables = observables;
        this.algorithms = algorithms;
    }

    /**
     * Returns the name of the pipeline.
     * 
     * @return the name of the pipeline
     */
    public String getPipeline() {
        return pipeline;
    }

    /**
     * Returns the name of the pipeline element.
     * 
     * @return the name of the pipeline element
     */
    public String getPipelineElement() {
        return pipelineElement;
    }
    
    /**
     * Returns the name of the algorithm to predict for.
     * 
     * @return the name of the algorithm (may be <b>null</b> for the actual algorithm or in case that 
     * {@link #getWeighting()} is given)
     */
    public String getAlgorithm() {
        return algorithm;
    }
    
    /**
     * Returns the actual weighting of observables.
     * 
     * @return the weighting of observables (may be <b>null</b> if {@link #getAlgorithm()} is given)
     */
    public Map<IObservable, Double> getWeighting() {
        return weighting;
    }

    /**
     * Returns the observables to perform a mass prediction for.
     * 
     * @return the observables (may be <b>null</b> if {@link #getAlgorithm()} is given)
     * @see #getObservable()
     */
    public Set<IObservable> getObservables() {
        return observables;
    }
    
    /**
     * Returns the observable to predict for.
     * 
     * @return the observable (may be <b>null</b> if {@link #getObservables()} is given and vv.)
     */
    public IObservable getObservable() {
        return observable;
    }
    
    /**
     * Returns the target values, i.e., the modification to the current settings.
     * 
     * @return the target values (in terms of parameters, distribution settings or input rate) 
     */
    public Map<Object, Serializable> getTargetValues() {
        return targetValues;
    }
    
    /**
     * Returns the algorithms to predict for.
     * 
     * @return the algorithms (may be <b>null</b> if {@link #getAlgorithm()} contains a value)
     */
    public Set<String> getAlgorithms() {
        return algorithms;
    }
    
}
