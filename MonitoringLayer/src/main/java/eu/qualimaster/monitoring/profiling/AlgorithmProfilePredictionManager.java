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
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.qualimaster.coordination.events.AlgorithmProfilingEvent;
import eu.qualimaster.coordination.events.PipelineResourceUnpackingPluginRegistrationEvent;
import eu.qualimaster.events.EventHandler;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent.Status;
import eu.qualimaster.monitoring.MonitoringConfiguration;
import eu.qualimaster.monitoring.events.AlgorithmChangedMonitoringEvent;
import eu.qualimaster.monitoring.events.AlgorithmProfilePredictionRequest;
import eu.qualimaster.monitoring.events.AlgorithmProfilePredictionResponse;
import eu.qualimaster.monitoring.events.ParameterChangedMonitoringEvent;
import eu.qualimaster.monitoring.profiling.ProfileReader.Meta;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.tracing.TraceReader.PipelineEntry;
import eu.qualimaster.observables.IObservable;

/**
 * Interface to the prediction of algorithm quality properties.
 * 
 * @author Christopher Voges
 * @author Holger Eichelberger
 */
public class AlgorithmProfilePredictionManager {
    
    private static String baseFolder;
    private static IAlgorithmProfileCreator creator = new KalmanProfileCreator(); // currently fixed, may be replaced
    private static boolean predict = MonitoringConfiguration.enableProfile();
    private static Double testPrediction;
    private static MultiPredictionResult testPredictionsMulti;
    private static Map<String, Map<IObservable, Double>> testPredictions;
    private static Map<String, Map<IObservable, Double>> testParameterPredictions;
 
    static {
        EventManager.register(new AlgorithmProfilePredictionRequestHandler());
    }
    
    /**
     * Access to the base folder.
     * 
     * @return the base folder
     */
    private static String getBaseFolder() {
        return null == baseFolder ? MonitoringConfiguration.getProfileLocation() : baseFolder;
    }
    
    /**
     * Called upon startup of the infrastructure.
     */
    public static void start() {
        // register the unpacking plugin
        EventManager.send(new PipelineResourceUnpackingPluginRegistrationEvent(new PipelineProfileUnpackingPlugin()));
        enableApproximation(MonitoringConfiguration.enableProfileApproximate());
    }
    
    /**
     * Obtains a pipeline. [public for testing]
     * 
     * @param name the name of the pipeline
     * @return the pipeline
     */
    public static Pipeline obtainPipeline(String name) {
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
            obtainPipeline(pipeline).setPath(getBaseFolder());
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
     * 
     * @see #setTestPrediction(Double)
     * @see #enablePrediction(boolean)
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
        return null == testPrediction ? result : testPrediction.doubleValue();
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
     * 
     * @see #setTestPredictions(Map)
     * @see #enablePrediction(boolean)
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
        return null == testPredictions ? result : testPredictions;
    }

    /**
     * Performs a mass-prediction for a set of algorithms and a set of observables.
     * 
     * @param pipeline the pipeline name containing <code>element</code>
     * @param element the pipeline element name running <code>algorithm</code>
     * @param algorithms the algorithms to predict for
     * @param observables the observables to create the prediction for
     * @return the predicted values 
     *   
     * @see #setTestPredictionsMulti(MultiPredictionResult)
     * @see #enablePrediction(boolean)
     */
    public static MultiPredictionResult predict(String pipeline, String element, Set<String> algorithms,
        Set<IObservable> observables) {
        MultiPredictionResult result = null;
        if (predict && null != algorithms) {
            result = new MultiPredictionResult();
            Pipeline pip = Pipelines.getPipeline(pipeline);
            if (null != pip) {
                PipelineElement elt = pip.getElement(element);
                if (null != elt) {
                    for (String algorithm : algorithms) {
                        elt.predict(algorithm, observables, result);
                    }
                }
            }
        }
        return null == testPredictionsMulti ? result : testPredictionsMulti;
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
     * 
     * @see #setTestParameterPredictions(Map)
     * @see #enablePrediction(boolean)
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
        return null == testParameterPredictions ? result : testParameterPredictions;
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
     * Stores all known pipelines and profiles.
     */
    public static void store() {
        Pipelines.storeAll();
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
     * Fills the algorithm profiles with the given entries. [just for replaying a stored pipeline from a CSV]
     * 
     * @param entries the CSV entries
     * @param meta the meta information about the pipeline
     */
    public static void fill(List<PipelineEntry> entries, Meta meta) {
        for (PipelineEntry entry : entries) {
            Pipeline pip = Pipelines.obtainPipeline(entry.getName(), creator);
            pip.setPath(getBaseFolder());
            pip.enableProfilingMode();
            for (String node : entry.nodes()) {
                PipelineElement elt = pip.obtainElement(node);
                for (IObservable obs : entry.observables()) {
                    if (ProfilingRegistry.storeAsParameter(obs)) {
                        elt.setParameter(obs, entry.getObservation(obs));
                    }
                }
                Map<Object, Serializable> params = meta.getParameters(node);
                if (null != params) {
                    for (Map.Entry<Object, Serializable> param : params.entrySet()) {
                        elt.setParameter(param.getKey(), param.getValue());
                    }
                }
                elt.update(entry, meta.getAlgorithm(), meta.getPredecessors(node));
            }
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
                if (event.doMultiAlgorithmPrediction()) {
                    MultiPredictionResult result = predict(pipeline, pipelineElement, event.getAlgorithms(), 
                        observables);
                    EventManager.send(new AlgorithmProfilePredictionResponse(event, result));
                } else {
                    Map<String, Map<IObservable, Double>> result = 
                        predict(pipeline, pipelineElement, event.getAlgorithms(), observables, targetValues);
                    EventManager.send(new AlgorithmProfilePredictionResponse(event, result));
                }
            }
        }
        
    }
    
    /**
     * Enables or disables the approximation functionality in case that parameters do not match any known profile.
     * 
     * @param enable <code>true</code> enable, <code>false</code> disable
     */
    public static void enableApproximation(boolean enable) {
        PipelineElement.enableApproximation(enable);
    }
    
    /**
     * Sets the next mass predictions for testing. The values remain valid until changed by the next call.
     * 
     * @param predictions the next predictions, i.e., a source-keyword-value mapping, <b>null</b> for real predictions
     */
    public static void setTestPredictions(Map<String, Map<IObservable, Double>> predictions) {
        testPredictions = predictions;
    }

    /**
     * Sets the next mass predictions for testing. The values remain valid until changed by the next call.
     * 
     * @param predictions the next predictions
     */
    public static void setTestPredictionsMulti(MultiPredictionResult predictions) {
        testPredictionsMulti = predictions;
    }
    
    /**
     * Sets the next mass parameter predictions for testing. The values remain valid until changed by the next call.
     * 
     * @param predictions the next predictions, i.e., a source-keyword-value mapping, <b>null</b> for real predictions
     */
    public static void setTestParameterPredictions(Map<String, Map<IObservable, Double>> predictions) {
        testParameterPredictions = predictions;
    }

    /**
     * Sets the next single predictions for testing. The value remain valid until changed by the next call.
     * 
     * @param prediction the next prediction, i.e., <b>null</b> for real prediction
     */
    public static void setTestPrediction(Double prediction) {
        testPrediction = prediction;
    }
    
    /**
     * Clears this prediction manager.
     */
    public static void clear() {
        Pipelines.releaseAllPipelines();
    }
    
    /**
     * Returns the profile creator.
     * 
     * @return the profile creator
     */
    public static IAlgorithmProfileCreator getCreator() {
        return creator;
    }

}
