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
package tests.eu.qualimaster.monitoring.profiling;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.monitoring.events.AlgorithmProfilePredictionRequest;
import eu.qualimaster.monitoring.events.AlgorithmProfilePredictionResponse;
import eu.qualimaster.monitoring.profiling.MultiPredictionResult;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.ResourceUsage;
import eu.qualimaster.observables.Scalability;
import eu.qualimaster.observables.TimeBehavior;

/**
 * Some event testing.
 * 
 * @author Holger Eichelberger
 */
public class EventTests {

    /**
     * Some tests of the algorithm profiling request.
     */
    @Test
    public void testAlgorithmProfilingRequest() {
        final String pipeline = "pip";
        final String element = "elt";
        final String algorithm = "alg";

        AlgorithmProfilePredictionRequest req = new AlgorithmProfilePredictionRequest(pipeline, element, 
            algorithm, TimeBehavior.THROUGHPUT_ITEMS);
        Assert.assertEquals(pipeline, req.getPipeline());
        Assert.assertEquals(element, req.getPipelineElement());
        Assert.assertNull(req.getAlgorithms());
        Assert.assertEquals(algorithm, req.getAlgorithm());
        Assert.assertEquals(TimeBehavior.THROUGHPUT_ITEMS, req.getObservable());
        Assert.assertNull(req.getObservables());
        Assert.assertNull(req.getTargetValues());
        Assert.assertFalse(req.doMultiAlgorithmPrediction());
        
        req = new AlgorithmProfilePredictionRequest(pipeline, element, algorithm, TimeBehavior.THROUGHPUT_ITEMS, null);
        Assert.assertEquals(pipeline, req.getPipeline());
        Assert.assertEquals(element, req.getPipelineElement());
        Assert.assertNull(req.getAlgorithms());
        Assert.assertEquals(algorithm, req.getAlgorithm());
        Assert.assertEquals(TimeBehavior.THROUGHPUT_ITEMS, req.getObservable());
        Assert.assertNull(req.getObservables());
        Assert.assertNull(req.getTargetValues());
        Assert.assertFalse(req.doMultiAlgorithmPrediction());

        Set<String> algorithms = new HashSet<String>();
        algorithms.add(algorithm);
        Set<IObservable> observables = new HashSet<IObservable>();
        observables.add(TimeBehavior.THROUGHPUT_ITEMS);
        observables.add(TimeBehavior.THROUGHPUT_VOLUME);
        req = new AlgorithmProfilePredictionRequest(pipeline, element, algorithms, observables, null);
        Assert.assertEquals(pipeline, req.getPipeline());
        Assert.assertEquals(element, req.getPipelineElement());
        Assert.assertNull(req.getAlgorithm());
        Assert.assertEquals(algorithms, req.getAlgorithms());
        Assert.assertNull(req.getObservable());
        Assert.assertEquals(observables, req.getObservables());
        Assert.assertNull(req.getTargetValues());
        Assert.assertFalse(req.doMultiAlgorithmPrediction());
        
        Assert.assertEquals(req, req.withMultiAlgorithmPrediction());
        Assert.assertTrue(req.doMultiAlgorithmPrediction());
    }

    /**
     * Tests the algorithm profiling response.
     */
    @Test
    public void testAlgorithmProfilingResponse() {
        final String pipeline = "pip";
        final String element = "elt";
        final String algorithm = "alg";
        
        Assert.assertEquals(algorithm, AlgorithmProfilePredictionResponse.getAlgorithmName(
            AlgorithmProfilePredictionResponse.getAlgorithmIdentifier(algorithm, 5)));

        AlgorithmProfilePredictionRequest req = new AlgorithmProfilePredictionRequest(pipeline, element, 
            algorithm, TimeBehavior.THROUGHPUT_ITEMS);
        
        AlgorithmProfilePredictionResponse resp = new AlgorithmProfilePredictionResponse(req, 25.0);
        Assert.assertEquals(25.0, resp.getPrediction(), 0.005);
        
        Map<String, Map<IObservable, Double>> predictions = new HashMap<String, Map<IObservable, Double>>();
        Map<IObservable, Double> tmp = new HashMap<IObservable, Double>();
        tmp.put(Scalability.ITEMS, 5.0);
        predictions.put(algorithm, tmp);
        
        resp = new AlgorithmProfilePredictionResponse(req, predictions);
        Assert.assertEquals(predictions, resp.getMassPrediction());
        
        Map<Object, Serializable> params = new HashMap<Object, Serializable>();
        params.put("window", 25);
        params.put(ResourceUsage.EXECUTORS, 10);
        MultiPredictionResult res = new MultiPredictionResult();
        res.add(algorithm, params, tmp);
        Map<IObservable, Double> tmp2 = new HashMap<IObservable, Double>();
        tmp2.put(Scalability.ITEMS, 7.0);
        Map<Object, Serializable> params2 = new HashMap<Object, Serializable>();
        params2.put("window", 20);
        params2.put(ResourceUsage.EXECUTORS, 10);
        res.add(algorithm, params2, tmp2);
        resp = new AlgorithmProfilePredictionResponse(req, res);
        // weak...
        for (Map.Entry<String, Map<IObservable, Double>> ent : resp.getMassPrediction().entrySet()) {
            Assert.assertEquals(algorithm, AlgorithmProfilePredictionResponse.getAlgorithmName(ent.getKey()));
            Assert.assertTrue(ent.getValue().equals(tmp) || ent.getValue().equals(tmp2));
        }
        for (Map.Entry<String, Map<Object, Serializable>> ent : resp.getParameters().entrySet()) {
            Assert.assertEquals(algorithm, AlgorithmProfilePredictionResponse.getAlgorithmName(ent.getKey()));
            Assert.assertTrue(ent.getValue().equals(params) || ent.getValue().equals(params2));
        }
    }

}
