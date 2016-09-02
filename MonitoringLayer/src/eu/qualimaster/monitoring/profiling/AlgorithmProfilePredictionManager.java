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
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import eu.qualimaster.coordination.events.AlgorithmProfilingEvent;
import eu.qualimaster.events.EventHandler;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent.Status;
import eu.qualimaster.monitoring.MonitoringConfiguration;
import eu.qualimaster.monitoring.events.AlgorithmChangedMonitoringEvent;
import eu.qualimaster.monitoring.events.AlgorithmProfilePredictionRequest;
import eu.qualimaster.monitoring.events.AlgorithmProfilePredictionResponse;
import eu.qualimaster.monitoring.events.ParameterChangedMonitoringEvent;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.observables.IObservable;

/**
 * Interface to the prediction of algorithm quality properties.
 * 
 * @author Christopher Voges
 * @author Holger Eichelberger
 */
public class AlgorithmProfilePredictionManager {
    
    private static String baseFolder = MonitoringConfiguration.getProfileLocation();
    private static IAlgorithmProfileCreator creator = new KalmanProfileCreator(); // currently fixed, may be replaced
 
    private static final Logger LOGGER = LogManager.getLogger(AlgorithmProfilePredictionManager.class);
    
    static {
        EventManager.register(new AlgorithmProfilePredictionRequestHandler());
    }
    
    /**
     * Called upon startup of the infrastructure.
     */
    public static void start() {
    }
    
    /**
     * Obtains a pipeline.
     * 
     * @param name the name of the pipeline
     * @return the pipeline
     */
    private static Pipeline obtainPipeline(String name) {
        return Pipelines.obtainPipeline(name, creator);
    }

     /**
     * Notifies the predictor about changes in the lifecycle of pipelines. This happens in particular during 
     * pipeline startup.
     *  
     * @param event the lifecycle event
     */
    public static void notifyPipelineLifecycleChange(PipelineLifecycleEvent event) {
        LOGGER.debug("TESTOUT: " + event);
        String pipeline = event.getPipeline();
        Status status = event.getStatus();
        
        switch (status) {
        case STARTING:
            obtainPipeline(pipeline).setPath(baseFolder);
            break;
        case STOPPED:
            Pipelines.releasePipeline(pipeline);
            break;
        default:
            break;
        }
    }

    /**
     * Is called when an algorithm changed. This happens in particular during pipeline startup.
     * 
     * @param event the algorithm changed monitoring event
     */
    public static void notifyAlgorithmChanged(AlgorithmChangedMonitoringEvent event) {
        Pipeline pip = Pipelines.getPipeline(event.getPipeline());
        if (null != pip) {
            PipelineElement elt = pip.obtainElement(event.getPipelineElement());
            elt.setActive(event.getAlgorithm());
        }
    }

    /**
     * Is called when the monitoring manager receives a {@link ParameterChangedMonitoringEvent}.
     * Although a full event bus handler would also do the job, this shall be less resource consumptive as 
     * the event is anyway received in the Monitoring Layer.
     * 
     * @param event the parameter change event
     */
    public static void notifyParameterChangedMonitoringEvent(ParameterChangedMonitoringEvent event) {
        Pipeline pip = Pipelines.getPipeline(event.getPipeline());
        if (null != pip) {
            PipelineElement elt = pip.obtainElement(event.getPipelineElement());
            elt.setParameter(event.getParameter(), event.getValue());
        }
    }

    /**
     * Is called during algorithm profiling, i.e., when collecting the initial profiles. Create new structures
     * for algorithm during {@link AlgorithmProfilingEvent.Status#START} and save them when a profiling round is closed
     * at {@link AlgorithmProfilingEvent.Status#NEXT} or {@link AlgorithmProfilingEvent.Status#END}.
     * 
     * @param event the profiling event
     */
    public static void notifyAlgorithmProfilingEvent(AlgorithmProfilingEvent event) {
        String pipeline = event.getPipeline();
        switch (event.getStatus()) {
        case NEXT:
        case END:
            Pipelines.releasePipeline(pipeline);
            // fallthrough
        case START:
            Pipeline pip = obtainPipeline(pipeline);
            pip.setPath(MonitoringConfiguration.getProfilingLogLocation());
            break;
        default:
            break;
        }
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
        Pipeline pip = Pipelines.getPipeline(pipeline);
        if (null != pip) {
            PipelineElement elt = pip.getElement(element);
            if (null != elt) {
                elt.update(family);
            }
        }
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
     * @return the predicted value ({@link Constants#NO_PREDICTION} in case of no prediction)
     */
    public static double predict(String pipeline, String element, String algorithm, IObservable observable, 
        Map<Object, Serializable> targetValues) {
        double result = Constants.NO_PREDICTION;
        Pipeline pip = Pipelines.getPipeline(pipeline);
        if (null != pip) {
            PipelineElement elt = pip.getElement(element);
            if (null != elt) {
                result = elt.predict(algorithm, observable, targetValues);
            }
        }
        return result;
    }

    /**
    * Called upon shutdown of the infrastructure. Clean up global resources here.
    */
    public static void stop() {
        Pipelines.releaseAllPipelines();
    }

    /**
     * Forces to use test data.
     * 
     * @param folder the base folder where the test data is located (<b>null</b> to reset 
     * to {@link MonitoringConfiguration#getProfileLocation()})
     */
    public static void useTestData(String folder) {
        if (null == folder) {
            baseFolder = MonitoringConfiguration.getProfileLocation();
        } else {
            baseFolder = folder;
        }
    }
    
    /**
     * Handles algorithm profile prediction requests.
     * 
     * @author Holger Eichelberger
     */
    private static class AlgorithmProfilePredictionRequestHandler 
        extends EventHandler<AlgorithmProfilePredictionRequest> {

        /**
         * Creates an algorithm profile prediction request handler.
         */
        protected AlgorithmProfilePredictionRequestHandler() {
            super(AlgorithmProfilePredictionRequest.class);
        }

        @Override
        protected void handle(AlgorithmProfilePredictionRequest event) {
            String pipeline = event.getPipeline();
            String pipelineElement = event.getPipelineElement();
            Map<IObservable, Double> weighting = event.getWeighting();
            if (null == weighting) {
                double result = predict(pipeline, event.getPipelineElement(), event.getAlgorithm(), 
                    event.getObservable(), event.getTargetValues());
                EventManager.send(new AlgorithmProfilePredictionResponse(event, result));
            } else {
                Pipeline pip = Pipelines.getPipeline(pipeline);
                if (null != pip) {
                    PipelineElement elt = pip.getElement(pipelineElement);
                    if (null != elt) {
                        dummy();
                        // TODO go over all algorithms, obtain profiles, predict
                    }
                }
                EventManager.send(new AlgorithmProfilePredictionResponse(event, Double.MIN_VALUE));
            }
        }

        /**
         * For checkstyle.
         */
        private void dummy() {
        }
        
    }
    
}
