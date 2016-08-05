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
import java.io.Serializable;
import java.util.Map;

import eu.qualimaster.coordination.events.AlgorithmProfilingEvent;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.monitoring.MonitoringConfiguration;
import eu.qualimaster.monitoring.events.AlgorithmChangedMonitoringEvent;
import eu.qualimaster.monitoring.events.ParameterChangedMonitoringEvent;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.tracing.Tracing;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.Observables;
import eu.qualimaster.observables.ResourceUsage;

/**
 * Interface to the prediction of algorithm quality properties.
 * 
 * @author Christopher Voges
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
        //System.err.println(event);
        /*
         * Use-Case The pipeline is STARTED, STOPPED or SWITCHED (i.e. one
         * starts, another stops)
         * 
         * 0. Continue only if the Pipeline did change, else: break/abort 
         * Implementation ideas: 
         * 1. Get all data needed to identify the potentially Kalman-Instance(s) 
         * (starting and/or stopping) 
         * 2. If STOP: Store the stopping Kalman-Instance 
         * 3. If START: 
         * 3a. Load the (re)starting Kalman-Instance from ram/disk or 
         * 3b. Create a new Kalman-Instance 
         *  (first: from scratch 
         *  later: as analogy, based on similar instances)
         * 
         * Assumption: Only the Pipeline changes. The Algorithm and IObsevables
         * (incl. EXECUTOTRS and TASKS) stay the same.
         */
    }

    /**
     * Is called when an algorithm changed. This happens in particular during pipeline startup.
     * 
     * @param event the algorithm changed monitoring event
     */
    public static void notifyAlgorithmChanged(AlgorithmChangedMonitoringEvent event) {
        event.getAlgorithm();
        event.getPipeline();
        event.getPipelineElement();
        /*
         * Use-Case
         * An Algorithm changes.
         * 
         * Implementation ideas: 
         * 0. Continue only if the Algorithm did change, else: break/abort 
         * 1. Get all data needed to identify the Kalman-Instances 
         * (old and new Algorithm) 
         * 2. Store the old Algorithms Kalman-Instance to ram/disk.
         * 3. For the new Algorithm:  
         * 3a. Load the (re)starting Kalman-Instance from ram/disk or 
         * 3b. Create a new Kalman-Instance 
         *  (first: from scratch, later: as analogy, based on similar instances)
         * 
         * Assumption: Only the Algorithm changes. The Pipeline and IObsevables
         * (incl. EXECUTOTRS and TASKS) stay the same.
         */
    }

    /**
     * Is called when the monitoring manager receives a {@link ParameterChangedMonitoringEvent}.
     * Although a full event bus handler would also do the job, this shall be less resource consumptive as 
     * the event is anyway received in the Monitoring Layer.
     * 
     * @param event the parameter change event
     */
    public static void notifyParameterChangedMonitoringEvent(ParameterChangedMonitoringEvent event) {
        event.getPipeline();
        event.getPipelineElement();
        event.getParameter();
        event.getValue();
        
        /*
         * Use-Case: A Kalman-Instance defining Parameter changes.
         * Such Paramters are EXECUTORS, TASKS or the monitored/predicted IObservable (e.g. latency).
         *  
         * Implementation ideas:
         * 0. Continue only if one or more Parameters changed, else: break/abort 
         * 1. Get all data needed to identify the Kalman-Instances 
         * (old and new Algorithm) 
         * 2. Store the old Kalman-Instance to ram/disk.
         * 3. For the new Algorithm:  
         * 3a. Load the (re)starting Kalman-Instance from ram/disk or 
         * 3b. Create a new Kalman-Instance 
         *  (first: from scratch, later: as analogy, based on similar instances)
         * 
         * Assumption: Only EXECUTORS and/or TASKS changes. The Pipeline, Algorithm
         * and monitored/predicted IObservabklstay the same.
         * 
         */
    }

    /**
     * Is called during algorithm profiling, i.e., when collecting the initial profiles. Create new structures
     * for algorithm during {@link AlgorithmProfilingEvent.Status#START} and save them when a profiling round is closed
     * at {@link AlgorithmProfilingEvent.Status#NEXT} or {@link AlgorithmProfilingEvent.Status#END}.
     * 
     * @param event the profiling event
     */
    public static void notifyAlgorithmProfilingEvent(AlgorithmProfilingEvent event) {
        MonitoringConfiguration.getProfilingLogLocation(); 
        /*
         * Use-Case: Creating Kalman-Instances for later use.
         * 
         * Implementation ideas: 
         * 1. Get all data needed to identify the Kalman-Instance 
         * 2a. If START: Create a new Kalman-Instance
         * 2b. If NEXT:  Load the already existing corresponding Kalman-Instance 
         * 2c. If END: Store the Kalman-Instance 
         * 3. If (START or NEXT) while (event.hasValues) updateKalman
         * 
         * Assumption: This method is (only) used for the batch-wise creation or 
         * updating of Kalman-Instances in an non-productive environment (during profiling), i.e. 
         * the total execution time can be longer than 500ms.
         * 
         */
    }
    
    /**
     * Called regularly to update the prediction model with the most recently monitored values.
     * 
     * @param pipeline the pipeline name containing <code>element</code>
     * @param element the pipeline element name running <code>algorithm</code>
     * @param family the family holding the algorithm as current node (copy). Family measures shall correspond to the 
     * algorithm measures, even if the algorithm is distribured and consists of different nodes
     */
    public static void update(String pipeline, String element, PipelineNodeSystemPart family) {
        @SuppressWarnings("unused")
        String algorithmName = family.getCurrent().getName(); // current may be null but shall not be passed
        // access to the predecessor nodes, e.g., for input/s
        Tracing.getPredecessors(family);
        for (IObservable obs : Observables.OBSERVABLES) {
            if (family.hasValue(obs)) {
                // TODO update Kalman
                family.getObservedValue(ResourceUsage.EXECUTORS);
                family.getObservedValue(ResourceUsage.TASKS);
                // Evtl noch Inputs/s fuer den Parameterraum
                dummy();
            }
        }
        /*
         * Use-Case:
         * A new value for a specific IObservable (e.g. latency) was observed.
         * 
         * 1. yValue=value; xValue=(timestamp/now)
         * 2. Get all data needed to identify the Kalman-Instance 
         * 3. If Kalman-Instance is NOT running:  
         * 3a. Load the (re)starting Kalman-Instance from ram/disk or 
         * 3b. Create a new Kalman-Instance 
         *  (first: from scratch, later: as analogy, based on similar instances)
         * 4. Update the Kalman-Instance with (xValue, yValue)
         * 
         * Note: One Kalman-Instance handles one specific (measured) IObservable.
         */
    }

    /**
     * Predict the next value for the given algorithm. If <code>targetValues</code> (observables or parameter values)
     * are given, the prediction shall take these into account, either to determine the related profile or to 
     * interpolate.
     * 
     * @param pipeline the pipeline name containing <code>element</code>
     * @param element the pipeline element name running <code>algorithm</code>
     * @param algorithm the name of the algorithm
     * @param observable the observable to predict
     * @param targetValues the target values for prediction. Predict the next step if <b>null</b> or empty. May contain
     *   observables ({@link IObservable}-Double) or parameter values (String-value)
     * @return the predicted value (<code>Double.MIN_VALUE</code> in case of no prediction)
     */
    public static double predict(String pipeline, String element, String algorithm, IObservable observable, 
        Map<Object, Serializable> targetValues) {
        return 0;
        /*
         * Use-Case:
         * Get the prediction for a specific IObservable 
         * one time-step (second) into the future.
         * 
         * Implementation idea:
         * 1. If targetValues==null:
         * 1a. Get all data needed to identify the Kalman-Instance then (If None: Abort; result=Double.MIN_VALUE)
         * 1b. If Kalman-Instance is NOT running: Load it from ram/disk
         * 1c: If Kalman-Instances last Update older than 1s: 
         *      Call Gap-Closing method (could be handled internally by Kalman)
         * 1d. Get predicted value ann return it
         * OR
         * 2: If targetValues!=null:
         * 2a. Get all data needed to identify the Kalman-Instance then 
         *      If None: Search for Kalman-Instances (Analog-Kalmans) corresponding to
         *      the elements of targetValue
         * 2b: IF Analog-Kalman==null: abort, result=Double.MIN_VALUE
         * 2c: ELSE
         *      Do 1a-1d and for all Instances in Analog-Kalman and interpolate the result.
         * 2d. return the interpolated result
         * 
         * 
         * Note: The second use case (2) is likely to take longer/too long.
         */
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
