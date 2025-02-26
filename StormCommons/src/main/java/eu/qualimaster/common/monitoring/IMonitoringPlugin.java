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
package eu.qualimaster.common.monitoring;

import java.util.Map;

import backtype.storm.hooks.info.EmitInfo;
import eu.qualimaster.monitoring.events.FrozenSystemState;
import eu.qualimaster.observables.IObservable;

/**
 * Represents a monitoring plugin. Must have a public non-argument constructor. If a monitoring plugin declares new 
 * {@link eu.qualimaster.observables.IObservable observables},
 * it must register them properly with the observation factory of the monitoring layer. So far, these observables
 * can only be accessed via Strings in rt-VIL as loaded dynamically or via their identifier name if they are integrated
 * with the QM-Extension for EASy-Producer.
 * 
 * @author Holger Eichelberger
 */
public interface IMonitoringPlugin {

    /**
     * Starts monitoring for an execution method.
     */
    public void startMonitoring();
    
    /**
     * Notifies about emitting a tuples.
     * 
     * @param info information about the last emit
     */
    public void emitted(EmitInfo info);
    
    /**
     * Ends monitoring for an execution method.
     */
    public void endMonitoring();

    /**
     * Notifies about emitting sink tuples.
     * 
     * @param tuple the emitted tuple
     */
    public void emitted(Object tuple);

    /**
     * Collects the observations for sending them to the infrastructure.
     * 
     * @param observations the observations to be modified as a side effect
     */
    public void collectObservations(Map<IObservable, Double> observations);

    /**
     * Analyzes the actual state. May send adaptation events.
     * 
     * @param state the actual system state
     */
    public void analyze(FrozenSystemState state);
    
}
