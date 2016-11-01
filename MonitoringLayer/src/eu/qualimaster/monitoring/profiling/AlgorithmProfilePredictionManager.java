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
import java.util.Set;

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
    private static boolean predict = true;
 
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
            pip.enableProfilingMode();
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
        if (predict) {
            Pipeline pip = Pipelines.getPipeline(pipeline);
            if (null != pip) {
                PipelineElement elt = pip.getElement(element);
                if (null != elt) {
                    result = elt.predict(algorithm, observable, targetValues);
                }
            }
        }
        return result;
    }

    /**
     * Performs a mass-prediction for a set of algorithms and a set of observables.
     * 
     * @param pipeline the pipeline name containing <code>element</code>
     * @param element the pipeline element name running <code>algorithm</code>
     * @param algorithms the algorithms to predict for
     * @param observables the observables to create the prediction for
     * @param targetValues the target values for prediction. Predict the next step if <b>null</b> or empty. May contain
     *   observables ({@link IObservable}-Double) or parameter values (String-value)
     * @return the predicted values with <b>null</b> as prediction if there is none, or <b>null</b> if no prediction is 
     *   possible at all, e.g., pipeline or element unknown
     */
    public static Map<String, Map<IObservable, Double>> predict(String pipeline, String element, Set<String> algorithms,
        Set<IObservable> observables, Map<Object, Serializable> targetValues) {
        Map<String, Map<IObservable, Double>> result = null;
        if (predict && null != algorithms) {
            Pipeline pip = Pipelines.getPipeline(pipeline);
            if (null != pip) {
                PipelineElement elt = pip.getElement(element);
                if (null != elt) {
                    result = new HashMap<String, Map<IObservable, Double>>();
                    for (String algorithm : algorithms) {
                        Map<IObservable, Double> algResults = new HashMap<IObservable, Double>();
                        for (IObservable obs : observables) {
                            double predicted = elt.predict(algorithm, obs, targetValues);
                            algResults.put(obs, (Constants.NO_PREDICTION == predicted) ? null : predicted);
                        }
                        result.put(algorithm, algResults);
                    }
                }
            }
        }
        return result;
    }
    
    /**
     * Predicts parameter values for a pipeline element.
     * 
     * @param pipeline the pipeline
     * @param element the pipeline element
     * @param parameter the parameter name
     * @param observables the observables to predict for
     * @param targetValues the target values for prediction. Predict the next step if <b>null</b> or empty. May contain
     *   observables ({@link IObservable}-Double) or parameter values (String-value)
     * @return the parameter-observable-prediction mapping, <b>null</b> if there are no predictions
     */
    public static Map<String, Map<IObservable, Double>> predictParameterValues(String pipeline, String element, 
        String parameter, Set<IObservable> observables, Map<Object, Serializable> targetValues) {
        Map<String, Map<IObservable, Double>> result = null;
        if (predict) {
            Pipeline pip = Pipelines.getPipeline(pipeline);
            if (null != pip) {
                PipelineElement elt = pip.getElement(element);
                if (null != elt) {
                    result = predictParameterValues(elt, parameter, observables, targetValues);
                }
            }
        }
        return result;
    }

    /**
     * Predicts parameter values for a pipeline element.
     * 
     * @param elt the pipeline element
     * @param parameter the parameter name
     * @param observables the observables to predict for
     * @param targetValues the target values for prediction. Predict the next step if <b>null</b> or empty. May contain
     *   observables ({@link IObservable}-Double) or parameter values (String-value)
     * @return the parameter-observable-prediction mapping, <b>null</b> if there are no predictions
     */
    private static Map<String, Map<IObservable, Double>> predictParameterValues(PipelineElement elt, 
        String parameter, Set<IObservable> observables, Map<Object, Serializable> targetValues) {
        Map<String, Map<IObservable, Double>> result = null;
        for (IObservable obs : observables) {
            Map<String, Double> preds = elt.predictParameterValues(parameter, obs, targetValues);
            if (null != preds) {
                if (null == result) {
                    result = new HashMap<String, Map<IObservable, Double>>();
                }
                for (Map.Entry<String, Double> e : preds.entrySet()) {
                    String pVal = e.getKey();
                    Map<IObservable, Double> tmp = result.get(pVal);
                    if (null == tmp) {
                        tmp = new HashMap<IObservable, Double>();
                        result.put(pVal, tmp);
                    }
                    tmp.put(obs, e.getValue());
                }
            }
        }
        return result;
    }
    
    /**
     * Predicts the best algorithm for the given situation.
     * 
     * @param pipeline the pipeline name containing <code>element</code>
     * @param element the pipeline element name running <code>algorithm</code>
     * @param algorithms the algorithms to predict for
     * @param weighting the weighting of observables
     * @param targetValues the target values for prediction. Predict the next step if <b>null</b> or empty. May contain
     *   observables ({@link IObservable}-Double) or parameter values (String-value)
     * @return the predicted algorithm or <b>null</b> if no one can be predicted
     */
    @Deprecated
    public static String predict(String pipeline, String element, Set<String> algorithms, 
        Map<IObservable, Double> weighting, Map<Object, Serializable> targetValues) {
        String result = null;
        if (predict && null != algorithms && null != weighting) {
            Pipeline pip = Pipelines.getPipeline(pipeline);
            if (null != pip) {
                PipelineElement elt = pip.getElement(element);
                if (null != elt) {
                    result = simpleWeighting(elt, algorithms, weighting, targetValues);
                }
            }
        }
        return result;
    }

    /**
     * Performs a simple weighting-based maximization over algorithms.
     * 
     * @param elt the pipeline element to predict for
     * @param algorithms the algorithms to take into account
     * @param weighting the weighting of observables
     * @param targetValues the target values for prediction. Predict the next step if <b>null</b> or empty. May contain
     *   observables ({@link IObservable}-Double) or parameter values (String-value)
     * @return the predicted algorithm or <b>null</b> if no one can be predicted
     */
    @Deprecated
    private static String simpleWeighting(PipelineElement elt, Set<String> algorithms, 
        Map<IObservable, Double> weighting, Map<Object, Serializable> targetValues) {
        String best = null;
        double bestVal = 0;
        for (String algorithm : algorithms) {
            double algVal = 0;
            for (Map.Entry<IObservable, Double> ent : weighting.entrySet()) {
                IObservable obs = ent.getKey();
                Double weight = ent.getValue();
                double sum = 0;
                double weights = 0;
                if (null != obs && null != weight) {
                    double predicted = elt.predict(algorithm, obs, targetValues);
                    if (Constants.NO_PREDICTION != predicted) {
                        sum = predicted * weight;
                        weights += weight;
                    }
                }
                if (weights != 0) {
                    algVal = sum / weights;
                } else {
                    algVal = 0;
                }
            }
            if (null == best || algVal > bestVal) {
                best = algorithm;
            }
        }
        return best;
    }

    /**
    * Called upon shutdown of the infrastructure. Clean up global resources here.
    */
    public static void stop() {
        Pipelines.releaseAllPipelines();
    }

    /**
     * Forces to use test data. [testing]
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
     * Enables or disables predictions. [testing]
     * By default, predictions are enabled.
     * 
     * @param enable <code>true</code> for enabling predictions, <code>false</code> else
     */
    public static void enablePrediction(boolean enable) {
        predict = enable;
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
            Set<IObservable> observables = event.getObservables();
            Map<Object, Serializable> targetValues = event.getTargetValues();
            String parameter = event.getParameter();
            if (null == observables) {
                double result = predict(pipeline, pipelineElement, event.getAlgorithm(), event.getObservable(), 
                    targetValues);
                EventManager.send(new AlgorithmProfilePredictionResponse(event, result));
            } else if (null != parameter) {
                Map<String, Map<IObservable, Double>> result = 
                    predictParameterValues(pipeline, pipelineElement, parameter, observables, targetValues);
                EventManager.send(new AlgorithmProfilePredictionResponse(event, result));
            } else {
                Map<String, Map<IObservable, Double>> result = 
                    predict(pipeline, pipelineElement, event.getAlgorithms(), observables, targetValues);
                EventManager.send(new AlgorithmProfilePredictionResponse(event, result));
            }
        }
        
    }
    
}
