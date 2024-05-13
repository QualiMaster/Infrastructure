/*
 * Copyright 2009-2015 University of Hildesheim, Software Systems Engineering
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

import eu.qualimaster.common.QMInternal;
import eu.qualimaster.observables.IObservable;

/**
 * Represents a pipeline monitoring event issued by the generated pipeline implementation.
 * 
 * @author Holger Eichelberger
 */

@QMInternal
public class PipelineObservationMonitoringEvent extends AbstractPipelineMonitoringEvent {

    private static final long serialVersionUID = -5621010499788402674L;
    private IObservable observable;
    private Double observation;
    private Serializable key;

    /**
     * Creates a pipeline element monitoring event.
     * 
     * @param pipeline the pipeline name
     * @param key the aggregation component key
     * @param observable the observable
     * @param observation the observation made
     */
    public PipelineObservationMonitoringEvent(String pipeline, ComponentKey key, IObservable observable, 
        int observation) {
        this(pipeline, key, observable, Double.valueOf(observation));
    }
    
    /**
     * Creates a pipeline element monitoring event.
     * 
     * @param pipeline the pipeline name
     * @param key the aggregation component key
     * @param observable the observable
     * @param observation the observation made
     */
    public PipelineObservationMonitoringEvent(String pipeline, ComponentKey key, 
        IObservable observable, Double observation) {
        super(pipeline);
        this.key = key;
        this.observable = observable;
        this.observation = observation;
    }

    /**
     * Returns the observable.
     * 
     * @return the observable
     */
    public IObservable getObservable() {
        return observable;
    }
    
    /**
     * Returns the observation.
     * 
     * @return the observation
     */
    public Double getObservation() {
        return observation;
    }

    /**
     * The aggregation component key.
     * 
     * @return the component key
     */
    public Serializable getKey() {
        return key;
    }
    
}
