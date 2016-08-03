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

import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.monitoring.MonitoringConfiguration;
import eu.qualimaster.monitoring.events.AlgorithmChangedMonitoringEvent;
import eu.qualimaster.monitoring.events.ParameterChangedMonitoringEvent;
import eu.qualimaster.monitoring.systemState.NodeImplementationSystemPart;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.Observables;

/**
 * Interface to the prediction of algorithm quality properties.
 * 
 * @author Holger Eichelberger
 */
public class AlgorithmProfilePredictor {
    
    /**
     * Called upon startup of the infrastructure.
     */
    public static void start() {
        // will contain the data files if provided through the pipeline artifact, for tests see #useTestData(File)
        MonitoringConfiguration.getProfileLocation(); 
    }

     /**
     * Notifies the predictor about changes in the lifecycle of pipelines. This happens in particular during 
     * pipeline startup.
     *  
     * @param event the lifecycle event
     */
    public static void notifyPipelineLifecycleChange(PipelineLifecycleEvent event) {
    }

    /**
     * Is called when an algorithm changed. This happens in particular during pipeline startup.
     * 
     * @param event the algorithm changed monitoring event
     */
    public static void notifyAlgorithmChanged(AlgorithmChangedMonitoringEvent event) {
    }

    /**
     * Is called when the monitoring manager receives a {@link ParameterChangedMonitoringEvent}.
     * Although a full event bus handler would also do the job, this shall be less resource consumptive as 
     * the event is anyway received in the Monitoring Layer.
     * 
     * @param event the event
     */
    public static void notifyParameterChangedMonitoringEvent(ParameterChangedMonitoringEvent event) {
    }
    
    /**
     * Called regularly to update the prediction model with the most recently monitored values.
     * 
     * @param pipeline the pipeline name containing <code>element</code>
     * @param element the pipeline element name running <code>algorith,</code>
     * @param algorithm the actual algorithm implement system part (copy)
     */
    public static void update(String pipeline, String element, NodeImplementationSystemPart algorithm) {
        @SuppressWarnings("unused")
        String algorithmName = algorithm.getName();
        for (IObservable obs : Observables.OBSERVABLES) {
            if (algorithm.hasValue(obs)) {
                // TODO update Kalman
                dummy();
            }
        }
    }

    /**
     * Predict the next value for the given algorithm.
     * 
     * @param pipeline the pipeline name containing <code>element</code>
     * @param element the pipeline element name running <code>algorithm</code>
     * @param algorithm the name of the algorithm
     * @param observable the observable to predict
     * @return the predicted value (<code>Double.MIN_VALUE</code> in case of no prediction)
     */
    public static double predict(String pipeline, String element, String algorithm, IObservable observable) {
        return 0;
    }

    /**
     * Just for checkstyle.
     */
    private static void dummy() {
    }

    /**
    * Called upon shutdown of the infrastructure. Clean up global resources here.
    */
    public static void stop() {
       // free resources, store all changed matrices
    }

    /**
     * Forces to use test data.
     * 
     * @param baseFolder the base folder where the test data is located
     */
    public static void useTestData(File baseFolder) {
        // so far not called as there is no specific data available
    }
    
}
